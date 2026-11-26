package com.findmycoach.auth;

import com.findmycoach.common.exception.ApiException;
import com.findmycoach.config.AppProperties;
import com.findmycoach.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {
    private static final SecureRandom RANDOM = new SecureRandom();

    private final RefreshTokenRepository repo;
    private final AppProperties props;
    private final Clock clock;

    @Transactional
    public String issue(User user) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        RefreshToken t = new RefreshToken();
        t.setUser(user);
        t.setTokenHash(hash(raw));
        t.setExpiresAt(clock.instant().plus(props.jwt().refreshTokenTtl()));
        repo.save(t);
        return raw;
    }

    /**
     * Validates and revokes the given token (rotation). Re-use of an already revoked token is treated as theft:
     * every refresh token of that user is revoked.
     */
    @Transactional(noRollbackFor = ApiException.class)
    public User consume(String raw) {
        RefreshToken t = repo.findByTokenHash(hash(raw))
                .orElseThrow(() -> ApiException.unauthorized("Invalid refresh token"));
        if (t.isRevoked()) {
            repo.revokeAllForUser(t.getUser());
            throw ApiException.unauthorized("Refresh token has already been used");
        }
        if (t.getExpiresAt().isBefore(clock.instant())) {
            t.setRevoked(true);
            throw ApiException.unauthorized("Refresh token expired");
        }
        t.setRevoked(true);
        User user = t.getUser();
        if (!user.isEnabled()) throw ApiException.unauthorized("Account is disabled");
        return user;
    }

    @Transactional
    public void revoke(String raw) {
        repo.findByTokenHash(hash(raw)).ifPresent(t -> t.setRevoked(true));
    }

    @Transactional
    public void revokeAll(User user) {
        repo.revokeAllForUser(user);
    }

    @Transactional
    public int purgeExpired() {
        return repo.deleteExpired(Instant.now(clock));
    }

    static String hash(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
