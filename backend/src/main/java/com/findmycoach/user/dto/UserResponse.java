package com.findmycoach.user.dto;

import com.findmycoach.user.User;

public record UserResponse(String id, String email, String name, String city, String avatar) {
    public static UserResponse from(User user) {
        return new UserResponse(
            user.getId().toString(),
            user.getEmail(),
            user.getName(),
            user.getCity(),
            user.getAvatar()
        );
    }
}
