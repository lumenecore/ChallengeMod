package com.lumenechallenge.util;

import java.util.concurrent.ThreadLocalRandom;

public final class SeedUtil {
    private SeedUtil() {
    }

    public static long resolveSeed(String raw) {
        if (raw == null) {
            return ThreadLocalRandom.current().nextLong();
        }
        String value = raw.trim();
        if (value.isEmpty()) {
            return ThreadLocalRandom.current().nextLong();
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ignored) {
            long hash = 1125899906842597L;
            for (int i = 0; i < value.length(); i++) {
                hash = 31L * hash + value.charAt(i);
            }
            return hash;
        }
    }
}
