package com.findmycoach.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** currentPassword may be omitted only by accounts that have never set a password (demo social sign-ins). */
public record ChangePasswordRequest(
        String currentPassword,
        @NotBlank @Size(min = 8, max = 100) String newPassword
) {
}
