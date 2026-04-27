package com.findmycoach.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.findmycoach.common.exception.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ProfileCompleteFilter extends OncePerRequestFilter {

    private static final AntPathMatcher MATCHER = new AntPathMatcher();
    private static final List<String> PROFILE_REQUIRED = List.of(
        "/api/v1/me", "/api/v1/bookings", "/api/v1/bookings/**"
    );
    private static final List<String> EXEMPT = List.of(
        "/api/v1/auth/complete-profile", "/api/v1/auth/logout",
        "/api/v1/auth/refresh"
    );

    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String path = request.getRequestURI();
        boolean requiresProfile = PROFILE_REQUIRED.stream()
            .anyMatch(p -> MATCHER.match(p, path));
        boolean exempt = EXEMPT.stream().anyMatch(p -> MATCHER.match(p, path));

        if (requiresProfile && !exempt) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth instanceof JwtAuthenticationToken jwtAuth) {
                Boolean profileComplete = jwtAuth.getToken().getClaim("profileComplete");
                if (!Boolean.TRUE.equals(profileComplete)) {
                    response.setStatus(HttpStatus.FORBIDDEN.value());
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    objectMapper.writeValue(response.getWriter(),
                        ErrorResponse.of("PROFILE_INCOMPLETE"));
                    return;
                }
            }
        }
        chain.doFilter(request, response);
    }
}
