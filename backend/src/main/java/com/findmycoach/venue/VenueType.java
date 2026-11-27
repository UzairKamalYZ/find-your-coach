package com.findmycoach.venue;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum VenueType {
    COURT("Court"), PITCH("Pitch"), STADIUM("Stadium"), CLUB("Club"), POOL("Pool"), HALL("Hall");

    private final String label;

    VenueType(String label) {
        this.label = label;
    }

    @JsonValue
    public String label() {
        return label;
    }

    @JsonCreator
    public static VenueType from(String v) {
        if (v == null) return null;
        return Arrays.stream(values()).filter(t -> t.label.equalsIgnoreCase(v.trim()) || t.name().equalsIgnoreCase(v.trim()))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("Unknown venue type: " + v));
    }
}
