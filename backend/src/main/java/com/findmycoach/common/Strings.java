package com.findmycoach.common;

public final class Strings {
    private Strings() {
    }

    /** Trims and converts blank strings to null. */
    public static String clean(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    public static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
