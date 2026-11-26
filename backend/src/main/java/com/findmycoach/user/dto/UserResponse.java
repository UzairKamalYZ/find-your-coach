package com.findmycoach.user.dto;

import com.findmycoach.user.AuthProvider;
import com.findmycoach.user.Role;
import com.findmycoach.user.User;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String name,
        String city,
        String phone,
        String avatar,
        Role role,
        AuthProvider provider,
        String bio,
        String sports,
        String goals,
        String level,
        String experience,
        String qualifications,
        boolean enabled,
        boolean hasPassword,
        String trainerId,
        Instant createdAt,
        Instant lastLoginAt
) {
    public static UserResponse from(User u, String trainerId) {
        return new UserResponse(u.getId(), u.getEmail(), u.getName(), nz(u.getCity()), u.getPhone(), u.getAvatar(),
                u.getRole(), u.getProvider(), u.getBio(), u.getSports(), u.getGoals(), u.getLevel(), u.getExperience(),
                u.getQualifications(), u.isEnabled(), u.getPasswordHash() != null, trainerId, u.getCreatedAt(),
                u.getLastLoginAt());
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
