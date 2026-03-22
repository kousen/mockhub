package com.mockhub.common.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

public final class SlugUtil {

    private static final Pattern NON_LATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]+");
    private static final Pattern CONSECUTIVE_HYPHENS = Pattern.compile("-{2,}");

    private SlugUtil() {
        // Utility class — not instantiable
    }

    /**
     * Converts a string to a URL-friendly slug.
     * Example: "Madison Square Garden" becomes "madison-square-garden"
     */
    public static String toSlug(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }

        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        String slug = WHITESPACE.matcher(normalized).replaceAll("-");
        slug = NON_LATIN.matcher(slug).replaceAll("");
        slug = CONSECUTIVE_HYPHENS.matcher(slug).replaceAll("-");
        slug = slug.toLowerCase(Locale.ENGLISH);
        slug = slug.replaceAll("(?:^-)|(?:-$)", "");

        return slug;
    }
}
