package fr.alescis.aelia.ui;

import fr.alescis.aelia.model.DataMetric;
import fr.alescis.aelia.model.MetricValue;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Centralizes user-facing formatting rules for the JavaFX UI.
 */
public final class UiFormatters {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    private UiFormatters() {
    }

    public static String value(MetricValue value) {
        if (value.numericValue().isPresent()) {
            DataMetric metric = value.metric();
            String pattern = "%." + metric.decimals() + "f%s";
            return String.format(Locale.ROOT, pattern, value.numericValue().getAsDouble(), metric.unitSuffix());
        }
        return value.textValue();
    }

    public static String shortValue(MetricValue value) {
        if (value.numericValue().isPresent()) {
            DataMetric metric = value.metric();
            String pattern = "%." + metric.decimals() + "f";
            return String.format(Locale.ROOT, pattern, value.numericValue().getAsDouble());
        }
        return value.textValue();
    }

    public static String timestamp(Instant instant) {
        return TIME_FORMATTER.format(instant);
    }

    public static String interval(Duration duration) {
        long seconds = duration.toSeconds();
        if (seconds < 60L) {
            return seconds + " s";
        }
        long minutes = seconds / 60L;
        long remainder = seconds % 60L;
        return remainder == 0L ? minutes + " min" : minutes + " min " + remainder + " s";
    }
}
