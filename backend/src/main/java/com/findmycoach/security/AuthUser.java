package com.findmycoach.security;

import com.findmycoach.user.Role;

import java.util.UUID;

/** The authenticated caller, resolved from a verified JWT and the current database state. */
public record AuthUser(UUID id, String email, String name, Role role) {
    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    public boolean isTrainer() {
        return role == Role.TRAINER;
    }
}
