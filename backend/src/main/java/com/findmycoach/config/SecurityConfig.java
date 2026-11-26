package com.findmycoach.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.findmycoach.common.exception.ErrorResponse;
import com.findmycoach.security.AuthUserConverter;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private static final String API = "/api/v1";

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, AuthUserConverter converter,
                                                   ObjectMapper objectMapper) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> {})
                .headers(h -> h.frameOptions(f -> f.sameOrigin())) // H2 console uses frames
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/actuator/health/**", "/actuator/info", "/error").permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/h2-console/**").permitAll()
                        .requestMatchers(HttpMethod.POST, API + "/auth/register", API + "/auth/login",
                                API + "/auth/social", API + "/auth/refresh", API + "/auth/logout").permitAll()
                        .requestMatchers(HttpMethod.GET, API + "/trainers", API + "/trainers/**",
                                API + "/venues", API + "/venues/**", API + "/sports", API + "/stats").permitAll()
                        .requestMatchers(API + "/admin/**").hasRole("ADMIN")
                        .requestMatchers(API + "/coach/**").hasRole("TRAINER")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(converter))
                        .authenticationEntryPoint((req, res, ex) ->
                                writeError(objectMapper, req, res, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED",
                                        "Authentication required or token invalid"))
                        .accessDeniedHandler((req, res, ex) ->
                                writeError(objectMapper, req, res, HttpStatus.FORBIDDEN, "FORBIDDEN",
                                        "You do not have permission to do that")))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((req, res, ex) ->
                                writeError(objectMapper, req, res, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED",
                                        "Authentication required"))
                        .accessDeniedHandler((req, res, ex) ->
                                writeError(objectMapper, req, res, HttpStatus.FORBIDDEN, "FORBIDDEN",
                                        "You do not have permission to do that")));
        return http.build();
    }

    private static void writeError(ObjectMapper om, HttpServletRequest req, HttpServletResponse res,
                                   HttpStatus status, String code, String message) throws IOException {
        res.setStatus(status.value());
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        om.writeValue(res.getOutputStream(),
                new ErrorResponse(status.value(), code, message, null, req.getRequestURI(), Instant.now()));
    }

    @Bean
    public JwtDecoder jwtDecoder(AppProperties props) {
        SecretKey key = new SecretKeySpec(props.jwt().secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(props.jwt().issuer()));
        return decoder;
    }

    @Bean
    public JwtEncoder jwtEncoder(AppProperties props) {
        SecretKey key = new SecretKeySpec(props.jwt().secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        return new NimbusJwtEncoder(new ImmutableSecret<>(key));
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(AppProperties props) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(props.corsAllowedOrigins() == null ? List.of() : props.corsAllowedOrigins());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Location"));
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
