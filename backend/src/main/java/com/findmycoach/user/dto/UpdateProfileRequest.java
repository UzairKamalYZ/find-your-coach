package com.findmycoach.user.dto;

import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
    @Size(min = 1, max = 100) String name,
    @Size(min = 1, max = 100) String city,
    @Size(max = 500) String avatar
) {}
