package com.findmycoach.common;

import java.text.Normalizer;
import java.util.Locale;

public final class Slugs {
    private Slugs() {
    }

    public static String slugify(String input) {
        String n = Normalizer.normalize(input == null ? "" : input, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        return n.isEmpty() ? "item" : n;
    }
}
