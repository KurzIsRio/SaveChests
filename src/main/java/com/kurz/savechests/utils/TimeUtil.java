package com.kurz.savechests.utils;

import java.util.concurrent.TimeUnit;

public class TimeUtil {

    public static String formatDuration(long seconds) {
        if (seconds <= 0) {
            return "0s";
        }

        long days = TimeUnit.SECONDS.toDays(seconds);
        seconds -= TimeUnit.DAYS.toSeconds(days);
        long hours = TimeUnit.SECONDS.toHours(seconds);
        seconds -= TimeUnit.HOURS.toSeconds(hours);
        long minutes = TimeUnit.SECONDS.toMinutes(seconds);
        seconds -= TimeUnit.MINUTES.toSeconds(minutes);

        StringBuilder formatted = new StringBuilder();
        if (days > 0) {
            formatted.append(days).append("d ");
        }
        if (hours > 0) {
            formatted.append(hours).append("h ");
        }
        if (minutes > 0) {
            formatted.append(minutes).append("m ");
        }
        if (seconds > 0 || formatted.length() == 0) {
            formatted.append(seconds).append("s");
        }

        return formatted.toString().trim();
    }
}
