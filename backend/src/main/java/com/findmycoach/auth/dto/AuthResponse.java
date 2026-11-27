package com.findmycoach.auth.dto;

import com.findmycoach.user.dto.UserResponse;

public record AuthResponse(String accessToken, String refreshToken, String tokenType, long expiresIn, UserResponse user) {
}
