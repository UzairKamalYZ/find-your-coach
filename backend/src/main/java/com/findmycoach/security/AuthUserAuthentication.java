package com.findmycoach.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;

public class AuthUserAuthentication extends AbstractAuthenticationToken {
    private final AuthUser principal;
    private final Jwt jwt;

    public AuthUserAuthentication(AuthUser principal, Jwt jwt, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.principal = principal;
        this.jwt = jwt;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return jwt;
    }

    @Override
    public AuthUser getPrincipal() {
        return principal;
    }

    @Override
    public String getName() {
        return principal.id().toString();
    }
}
