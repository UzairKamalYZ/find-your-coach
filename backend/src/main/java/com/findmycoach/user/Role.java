package com.findmycoach.user;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

public enum Role {
    TRAINEE, TRAINER, ADMIN;

    @JsonValue
    public String json() {
        return name().toLowerCase(Locale.ROOT);
    }

    @JsonCreator
    public static Role from(String value) {
        if (value == null) return null;
        return Role.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
