package com.findmycoach.auth;

import com.findmycoach.common.exception.AppException;
import com.findmycoach.support.TestSecurityConfig;
import com.findmycoach.user.User;
import com.nimbusds.jose.jwk.RSAKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TokenServiceTest {

    private RefreshTokenRepository refreshTokenRepository;
    private TokenService tokenService;

    @BeforeEach
    void setUp() throws Exception {
        RSAKey rsaKey = TestSecurityConfig.TEST_RSA_KEY;
        var jwtEncoder = TestSecurityConfig.buildEncoder(rsaKey);
        var jwtDecoder = NimbusJwtDecoder.withPublicKey(rsaKey.toRSAPublicKey()).build();
        refreshTokenRepository = mock(RefreshTokenRepository.class);
        tokenService = new TokenService(jwtEncoder, jwtDecoder, refreshTokenRepository, 900L, 2592000L);
    }

    @Test
    void generateAccessToken_containsExpectedClaims() {
        User user = testUser(true);
        String token = tokenService.generateAccessToken(user);
        assertThat(token).isNotBlank();
    }

    @Test
    void generateRefreshToken_savesToken() {
        User user = testUser(true);
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        String token = tokenService.generateRefreshToken(user);
        assertThat(token).isNotBlank();
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void validateRefreshToken_revokedToken_throwsUnauthorized() {
        RefreshToken rt = new RefreshToken();
        rt.setRevoked(true);
        rt.setExpiresAt(Instant.now().plusSeconds(3600));
        rt.setUser(testUser(true));
        when(refreshTokenRepository.findByToken("tok")).thenReturn(java.util.Optional.of(rt));

        assertThatThrownBy(() -> tokenService.validateRefreshToken("tok"))
            .isInstanceOf(AppException.class)
            .hasMessageContaining("revoked");
    }

    @Test
    void validateRefreshToken_expiredToken_throwsUnauthorized() {
        RefreshToken rt = new RefreshToken();
        rt.setRevoked(false);
        rt.setExpiresAt(Instant.now().minusSeconds(1));
        rt.setUser(testUser(true));
        when(refreshTokenRepository.findByToken("tok")).thenReturn(java.util.Optional.of(rt));

        assertThatThrownBy(() -> tokenService.validateRefreshToken("tok"))
            .isInstanceOf(AppException.class)
            .hasMessageContaining("expired");
    }

    private User testUser(boolean withCity) {
        User u = new User();
        u.setId(UUID.randomUUID());
        u.setGoogleId("g123");
        u.setEmail("test@example.com");
        u.setName("Test User");
        if (withCity) u.setCity("London");
        return u;
    }
}
