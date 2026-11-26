package com.findmycoach.user;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

/** How an account signs in. Social providers are demo-only (no real provider connection). */
public enum AuthProvider {
    EMAIL("Email"), GOOGLE("Google"), APPLE("Apple"), FACEBOOK("Facebook"), MICROSOFT("Microsoft");

    private final String label;

    AuthProvider(String label) {
        this.label = label;
    }

    @JsonValue
    public String label() {
        return label;
    }

    @JsonCreator
    public static AuthProvider from(String value) {
        if (value == null) return null;
        return Arrays.stream(values())
                .filter(p -> p.label.equalsIgnoreCase(value.trim()) || p.name().equalsIgnoreCase(value.trim()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown provider: " + value));
    }
}
