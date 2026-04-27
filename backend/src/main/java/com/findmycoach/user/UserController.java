package com.findmycoach.user;

import com.findmycoach.user.dto.UpdateProfileRequest;
import com.findmycoach.user.dto.UserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public UserResponse getMe(@AuthenticationPrincipal Jwt jwt) {
        return userService.getUser(jwt.getSubject());
    }

    @PatchMapping
    public UserResponse patchMe(@AuthenticationPrincipal Jwt jwt,
                                @Valid @RequestBody UpdateProfileRequest request) {
        return userService.updateUser(jwt.getSubject(), request);
    }
}
