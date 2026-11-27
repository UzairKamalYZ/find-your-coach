package com.findmycoach.user.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Partial profile update: a null field is left unchanged, an empty string clears it.
 * Email and role cannot be changed here.
 */
public record UpdateProfileRequest(
        @Size(min = 1, max = 100) String name,
        @Size(max = 100) String city,
        @Size(max = 40) @Pattern(regexp = "^[0-9+()\\-\\s.]*$", message = "must be a valid phone number") String phone,
        @Size(max = 500) String avatar,
        @Size(max = 1000) String bio,
        @Size(max = 200) String sports,
        @Size(max = 300) String goals,
        @Pattern(regexp = "^(|Beginner|Intermediate|Advanced)$", message = "must be Beginner, Intermediate or Advanced") String level,
        @Pattern(regexp = "^(|[0-9]{1,2})$", message = "must be a number between 0 and 80") String experience,
        @Size(max = 300) String qualifications
) {
}
