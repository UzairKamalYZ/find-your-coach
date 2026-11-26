package com.findmycoach.user;

import com.findmycoach.security.AuthUser;
import com.findmycoach.user.dto.ChangePasswordRequest;
import com.findmycoach.user.dto.UpdateProfileRequest;
import com.findmycoach.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "My account")
@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
public class MeController {
    private final UserService userService;

    @GetMapping
    public UserResponse me(@AuthenticationPrincipal AuthUser me) {
        return userService.me(me.id());
    }

    @PatchMapping
    public UserResponse update(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody UpdateProfileRequest req) {
        return userService.update(me.id(), req);
    }

    @PostMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody ChangePasswordRequest req) {
        userService.changePassword(me.id(), req);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@AuthenticationPrincipal AuthUser me) {
        userService.deactivate(me.id());
    }
}
