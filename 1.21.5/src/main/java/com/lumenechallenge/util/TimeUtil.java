package com.lumenechallenge.util;

public final class TimeUtil {
    private TimeUtil() {
    }

    public static String formatTicks(long ticks) {
        long totalSeconds = Math.max(0L, ticks / 20L);
        long hours = totalSeconds / 3600L;
        long minutes = (totalSeconds % 3600L) / 60L;
        long seconds = totalSeconds % 60L;
        return String.format(java.util.Locale.ROOT, "%02d.%02d.%02d", hours, minutes, seconds);
    }
}
