package fr.alescis.aelia.model;

import java.time.LocalTime;
import java.util.Objects;

/**
 * Hourly weather forecast point.
 */
public record HourlyForecast(
        LocalTime time,
        WeatherCondition condition,
        int temperatureCelsius,
        boolean selected
) {
    public HourlyForecast {
        time = Objects.requireNonNull(time, "time");
        condition = Objects.requireNonNull(condition, "condition");
    }

    public HourlyForecast(String hour, WeatherCondition condition, int temperatureCelsius, boolean selected) {
        this(parseHour(hour), condition, temperatureCelsius, selected);
    }

    public String hour() {
        return time.getMinute() == 0 ? time.getHour() + "h" : "%02dh%02d".formatted(time.getHour(), time.getMinute());
    }

    private static LocalTime parseHour(String label) {
        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException("Hour is required.");
        }
        String normalized = label.trim().toLowerCase().replace("h", ":");
        if (normalized.endsWith(":")) {
            normalized += "00";
        }
        String[] parts = normalized.split(":");
        int hour = Integer.parseInt(parts[0]);
        int minute = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
        return LocalTime.of(hour % 24, minute);
    }
}
