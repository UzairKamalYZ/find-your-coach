package com.findmycoach.auth.dto;

import com.findmycoach.user.AuthProvider;
import com.findmycoach.user.Role;
import jakarta.validation.constraints.NotNull;

/** Demo social sign-in: no provider is contacted, a sample identity per provider is used. */
public record SocialLoginRequest(@NotNull AuthProvider provider, Role role) {
}
