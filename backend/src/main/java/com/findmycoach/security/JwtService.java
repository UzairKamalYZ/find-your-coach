package com.findmycoach.security;

import com.findmycoach.config.AppProperties;
import com.findmycoach.user.User;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;

@Service
public class JwtService {

    private final JwtEncoder encoder;
    private final AppProperties.Jwt props;
    private final Clock clock;

    public JwtService(JwtEncoder encoder, AppProperties appProperties, Clock clock) {
        this.encoder = encoder;
        this.props = appProperties.jwt();
        this.clock = clock;
        if (props.secret() == null || props.secret().length() < 32) {
            throw new IllegalStateException("app.jwt.secret must be at least 32 characters");
        }
    }

    public String issueAccessToken(User user) {
        Instant now = clock.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(props.issuer())
                .subject(user.getId().toString())
                .issuedAt(now)
                .expiresAt(now.plus(props.accessTokenTtl()))
                .claim("email", user.getEmail())
                .claim("name", user.getName())
                .claim("role", user.getRole().name())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    public long accessTokenTtlSeconds() {
        return props.accessTokenTtl().toSeconds();
    }
}
