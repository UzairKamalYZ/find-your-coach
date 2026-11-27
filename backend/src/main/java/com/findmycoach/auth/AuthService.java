package com.findmycoach.auth;

import com.findmycoach.audit.AuditService;
import com.findmycoach.auth.dto.*;
import com.findmycoach.common.Strings;
import com.findmycoach.common.exception.ApiException;
import com.findmycoach.notification.NotificationService;
import com.findmycoach.notification.NotificationType;
import com.findmycoach.security.AuthUser;
import com.findmycoach.security.JwtService;
import com.findmycoach.user.AuthProvider;
import com.findmycoach.user.Role;
import com.findmycoach.user.User;
import com.findmycoach.user.UserRepository;
import com.findmycoach.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthService {

    /** Same message for unknown email and wrong password, to avoid account enumeration. */
    private static final String BAD_CREDENTIALS = "Incorrect email or password";

    private final UserRepository userRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final Clock clock;

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (req.role() == Role.ADMIN) {
            throw ApiException.badRequest("Admin accounts cannot be self-registered");
        }
        String email = normalizeEmail(req.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict("An account with this email already exists");
        }
        User u = new User();
        u.setEmail(email);
        u.setPasswordHash(passwordEncoder.encode(req.password()));
        u.setName(req.name().trim());
        u.setCity(Strings.clean(req.city()));
        u.setPhone(Strings.clean(req.phone()));
        u.setRole(req.role());
        u.setProvider(AuthProvider.EMAIL);
        u.setLastLoginAt(clock.instant());
        userRepository.save(u);
        notificationService.notify(u, NotificationType.ACCOUNT, "Welcome to FindMyTrainer!",
                u.getRole() == Role.TRAINER
                        ? "Create your coaching listing from the coach dashboard so trainees can find you."
                        : "Browse coaches near you and book your first session.",
                u.getRole() == Role.TRAINER ? "/coach" : "/trainers");
        auditService.record(new AuthUser(u.getId(), u.getEmail(), u.getName(), u.getRole()), "USER_REGISTERED", "USER", u.getId(), null);
        return tokens(u);
    }

    @Transactional
    public AuthResponse login(LoginRequest req) {
        User u = userRepository.findByEmailIgnoreCase(normalizeEmail(req.email()))
                .orElseThrow(() -> ApiException.unauthorized(BAD_CREDENTIALS));
        if (u.getPasswordHash() == null || !passwordEncoder.matches(req.password(), u.getPasswordHash())) {
            throw ApiException.unauthorized(BAD_CREDENTIALS);
        }
        if (!u.isEnabled()) {
            throw ApiException.forbidden("This account has been disabled. Please contact support.");
        }
        u.setLastLoginAt(clock.instant());
        return tokens(u);
    }

    /**
     * Demonstration social sign-in. No identity provider is contacted: each provider maps to a fixed sample
     * identity (alex.&lt;provider&gt;@example.com). The role is only used when the account is first created.
     */
    @Transactional
    public AuthResponse socialLogin(SocialLoginRequest req) {
        if (req.provider() == AuthProvider.EMAIL) {
            throw ApiException.badRequest("Use /auth/login for email sign-in");
        }
        if (req.role() == Role.ADMIN) {
            throw ApiException.badRequest("Admin accounts cannot be created via social sign-in");
        }
        String email = "alex." + req.provider().name().toLowerCase(Locale.ROOT) + "@example.com";
        User u = userRepository.findByEmailIgnoreCase(email).orElseGet(() -> {
            User n = new User();
            n.setEmail(email);
            n.setName("Alex Morgan (demo)");
            n.setRole(req.role() == null ? Role.TRAINEE : req.role());
            n.setProvider(req.provider());
            return userRepository.save(n);
        });
        if (!u.isEnabled()) {
            throw ApiException.forbidden("This account has been disabled. Please contact support.");
        }
        u.setProvider(req.provider());
        u.setLastLoginAt(clock.instant());
        return tokens(u);
    }

    /** noRollbackFor: revocations done while rejecting a reused/expired token must be kept. */
    @Transactional(noRollbackFor = ApiException.class)
    public AuthResponse refresh(String refreshToken) {
        User u = refreshTokenService.consume(refreshToken);
        return tokens(u);
    }

    @Transactional
    public void logout(String refreshToken) {
        refreshTokenService.revoke(refreshToken);
    }

    private AuthResponse tokens(User u) {
        String access = jwtService.issueAccessToken(u);
        String refresh = refreshTokenService.issue(u);
        return new AuthResponse(access, refresh, "Bearer", jwtService.accessTokenTtlSeconds(), userService.toResponse(u));
    }

    static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
