package com.findmycoach.auth.dto;

import com.findmycoach.user.dto.UserResponse;

public record AuthResponse(String accessToken, String refreshToken, UserResponse user) {}
