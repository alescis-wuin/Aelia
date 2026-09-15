package fr.alescis.aelia.ui;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;

/**
 * Presentation-only formatting helpers.
 */
public final class UiFormatters {
    public static final Locale FRENCH = Locale.FRANCE;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm", FRENCH);

    private UiFormatters() {
    }

    public static String time(LocalTime time) {
        return TIME_FORMATTER.format(time);
    }

    public static String hour(LocalTime time) {
        return time.getMinute() == 0 ? "%02dh".formatted(time.getHour()) : "%02dh%02d".formatted(time.getHour(), time.getMinute());
    }

    public static String dateLabel(LocalDate date) {
        String day = date.getDayOfWeek().getDisplayName(TextStyle.SHORT, FRENCH);
        String month = date.getMonth().getDisplayName(TextStyle.SHORT, FRENCH);
        return capitalize(day) + " " + date.getDayOfMonth() + " " + month + " " + date.getYear();
    }

    public static String daylightDuration(Duration duration) {
        long minutes = Math.max(0, duration.toMinutes());
        long hours = minutes / 60;
        long remainingMinutes = minutes % 60;
        return hours + "h " + String.format(FRENCH, "%02d", remainingMinutes) + " min de jour";
    }

    public static String trackedUppercase(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String upper = value.toUpperCase(FRENCH);
        StringBuilder builder = new StringBuilder(upper.length() * 2);
        boolean previousWasSpace = true;
        for (int i = 0; i < upper.length(); i++) {
            char current = upper.charAt(i);
            if (Character.isWhitespace(current)) {
                builder.append(' ');
                previousWasSpace = true;
            } else {
                if (!previousWasSpace && current != '·' && current != '-') {
                    builder.append(' ');
                }
                builder.append(current);
                previousWasSpace = current == '·' || current == '-';
            }
        }
        return builder.toString();
    }

    private static String capitalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.substring(0, 1).toUpperCase(FRENCH) + value.substring(1);
    }
}
