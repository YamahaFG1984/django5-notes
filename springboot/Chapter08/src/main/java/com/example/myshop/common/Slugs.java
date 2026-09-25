package com.example.myshop.common;

import java.text.Normalizer;
import java.util.Locale;

/** django.utils.text.slugify 的 Java 版。 */
public final class Slugs {

    private Slugs() {
    }

    public static String slugify(String value) {
        if (value == null) {
            return "";
        }
        String ascii = Normalizer.normalize(value, Normalizer.Form.NFKD).replaceAll("\\p{M}", "");
        return ascii.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\s-]", "")
                .trim()
                .replaceAll("[\\s-]+", "-");
    }
}
