package com.findmycoach.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CompleteProfileRequest(
    @NotBlank @Size(min = 1, max = 100) String city
) {}
