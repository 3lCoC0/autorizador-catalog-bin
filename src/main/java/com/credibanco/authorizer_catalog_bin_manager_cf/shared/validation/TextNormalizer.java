package com.credibanco.authorizer_catalog_bin_manager_cf.shared.validation;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Utility methods to normalize textual input before persisting it. Converts text to
 * upper-case using a locale independent strategy and strips diacritical marks so that
 * characters such as "Á" or "Ñ" are stored as plain ASCII.
 */
public final class TextNormalizer {

    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");

    private TextNormalizer() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Normalizes the provided text by trimming, converting it to upper case using the root
     * locale and stripping diacritical marks. Returns {@code null} when the input is
     * {@code null}. Blank strings are converted to an empty string.
     */
    public static String uppercaseAndRemoveAccents(String input) {
        if (input == null) {
            return null;
        }
        String trimmed = input.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        String upper = trimmed.toUpperCase(Locale.ROOT);
        String normalized = Normalizer.normalize(upper, Normalizer.Form.NFD);
        return DIACRITICS.matcher(normalized).replaceAll("");
    }

    /**
     * Applies {@link #uppercaseAndRemoveAccents(String)} to each value in the provided array,
     * mutating it in place. Null arrays are ignored.
     */
    public static void uppercaseAndRemoveAccents(String... values) {
        if (values == null) {
            return;
        }
        for (int i = 0; i < values.length; i++) {
            values[i] = uppercaseAndRemoveAccents(values[i]);
        }
    }
}