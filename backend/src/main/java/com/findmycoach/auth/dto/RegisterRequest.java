package com.findmycoach.auth.dto;

import com.findmycoach.user.Role;
import jakarta.validation.constraints.*;

public record RegisterRequest(
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(min = 8, max = 100) String password,
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(max = 100) String city,
        @NotNull Role role,
        @Size(max = 40) @Pattern(regexp = "^[0-9+()\\-\\s.]*$", message = "must be a valid phone number") String phone
) {
}
