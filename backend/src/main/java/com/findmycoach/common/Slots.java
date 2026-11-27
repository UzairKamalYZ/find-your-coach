package com.findmycoach.common;

import java.time.LocalTime;
import java.util.List;

/** Fixed hourly session slots shown on the booking calendar. */
public final class Slots {
    public static final List<String> ALL = List.of("08:00", "09:00", "10:00", "12:00", "14:00", "15:00", "17:00", "18:00", "19:00");
    public static final String PATTERN = "^([01]\\d|2[0-3]):00$";

    private Slots() {
    }

    public static boolean isValid(String slot) {
        return slot != null && ALL.contains(slot);
    }

    public static LocalTime toTime(String slot) {
        return LocalTime.parse(slot);
    }
}
