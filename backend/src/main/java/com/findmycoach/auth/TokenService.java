package com.findmycoach.auth;

import com.findmycoach.common.exception.AppException;
import com.findmycoach.user.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class TokenService {

    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;
    private final RefreshTokenRepository refreshTokenRepository;
    private final long accessTokenTtl;
    private final long refreshTokenTtl;

    public TokenService(JwtEncoder jwtEncoder,
                        JwtDecoder jwtDecoder,
                        RefreshTokenRepository refreshTokenRepository,
                        @Value("${app.jwt.access-token-ttl:900}") long accessTokenTtl,
                        @Value("${app.jwt.refresh-token-ttl:2592000}") long refreshTokenTtl) {
        this.jwtEncoder = jwtEncoder;
        this.jwtDecoder = jwtDecoder;
        this.refreshTokenRepository = refreshTokenRepository;
        this.accessTokenTtl = accessTokenTtl;
        this.refreshTokenTtl = refreshTokenTtl;
    }

    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
            .subject(user.getId().toString())
            .claim("email", user.getEmail())
            .claim("profileComplete", user.getCity() != null)
            .issuedAt(now)
            .expiresAt(now.plusSeconds(accessTokenTtl))
            .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

    @Transactional
    public String generateRefreshToken(User user) {
        String token = UUID.randomUUID().toString();
        RefreshToken rt = new RefreshToken();
        rt.setUser(user);
        rt.setToken(token);
        rt.setExpiresAt(Instant.now().plusSeconds(refreshTokenTtl));
        refreshTokenRepository.save(rt);
        return token;
    }

    @Transactional
    public User validateRefreshToken(String token) {
        RefreshToken rt = refreshTokenRepository.findByToken(token)
            .orElseThrow(() -> AppException.unauthorized("Invalid refresh token"));
        if (rt.isRevoked()) {
            throw AppException.unauthorized("Refresh token revoked");
        }
        if (rt.getExpiresAt().isBefore(Instant.now())) {
            throw AppException.unauthorized("Refresh token expired");
        }
        rt.setRevoked(true);
        refreshTokenRepository.save(rt);
        return rt.getUser();
    }

    @Transactional
    public void revokeRefreshToken(String token) {
        refreshTokenRepository.findByToken(token).ifPresent(rt -> {
            rt.setRevoked(true);
            refreshTokenRepository.save(rt);
        });
    }
}
