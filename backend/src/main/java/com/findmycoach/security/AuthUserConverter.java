package com.findmycoach.security;

import com.findmycoach.user.User;
import com.findmycoach.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Turns a verified JWT into an {@link AuthUserAuthentication}. The user is reloaded on every request so that
 * disabling an account or changing a role takes effect immediately, without waiting for the token to expire.
 */
@Component
@RequiredArgsConstructor
public class AuthUserConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final UserRepository userRepository;

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        UUID id;
        try {
            id = UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException e) {
            throw new InvalidBearerTokenException("Invalid token subject");
        }
        User user = userRepository.findById(id)
                .orElseThrow(() -> new InvalidBearerTokenException("Account no longer exists"));
        if (!user.isEnabled()) {
            throw new DisabledException("Account is disabled");
        }
        AuthUser principal = new AuthUser(user.getId(), user.getEmail(), user.getName(), user.getRole());
        return new AuthUserAuthentication(principal, jwt,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
    }
}
