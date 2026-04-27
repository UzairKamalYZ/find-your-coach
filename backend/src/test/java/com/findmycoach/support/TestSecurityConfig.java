package com.findmycoach.support;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.security.interfaces.RSAPublicKey;

public class TestSecurityConfig {

    public static final RSAKey TEST_RSA_KEY;

    static {
        try {
            TEST_RSA_KEY = new RSAKeyGenerator(2048).generate();
        } catch (JOSEException e) {
            throw new RuntimeException("Failed to generate test RSA key", e);
        }
    }

    public static JwtEncoder buildEncoder(RSAKey rsaKey) {
        return new NimbusJwtEncoder(new com.nimbusds.jose.jwk.source.ImmutableJWKSet<>(new JWKSet(rsaKey)));
    }

    @TestConfiguration
    public static class Config {

        @Bean
        @Primary
        public JwtEncoder jwtEncoder() {
            return buildEncoder(TEST_RSA_KEY);
        }

        @Bean
        @Primary
        public JwtDecoder jwtDecoder() throws JOSEException {
            return NimbusJwtDecoder.withPublicKey((RSAPublicKey) TEST_RSA_KEY.toPublicKey()).build();
        }
    }
}
