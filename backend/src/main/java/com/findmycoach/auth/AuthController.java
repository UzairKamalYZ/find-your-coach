package com.findmycoach.auth;

import com.findmycoach.auth.dto.AuthResponse;
import com.findmycoach.auth.dto.CompleteProfileRequest;
import com.findmycoach.auth.dto.RefreshRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request.refreshToken());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest request) {
        authService.logout(request.refreshToken());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/complete-profile")
    public AuthResponse completeProfile(@AuthenticationPrincipal Jwt jwt,
                                        @Valid @RequestBody CompleteProfileRequest request) {
        return authService.completeProfile(jwt.getSubject(), request);
    }
}
