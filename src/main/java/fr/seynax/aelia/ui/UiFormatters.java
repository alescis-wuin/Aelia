package fr.seynax.aelia.ui;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

/**
 * Small formatting helpers used by the JavaFX view layer.
 */
public final class UiFormatters {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter
            .ofLocalizedDateTime(FormatStyle.SHORT)
            .withLocale(Locale.getDefault())
            .withZone(ZoneId.systemDefault());

    private UiFormatters() {
    }

    public static String instant(Instant instant) {
        return DATE_TIME_FORMATTER.format(instant);
    }

    public static String duration(Duration duration) {
        long seconds = duration.toSeconds();
        if (seconds < 60) {
            return seconds + " s";
        }
        long minutes = seconds / 60;
        if (minutes < 60) {
            return minutes + " min";
        }
        long hours = minutes / 60;
        return hours + " h";
    }
}
