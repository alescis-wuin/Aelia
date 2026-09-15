package fr.alescis.aelia.model;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Daily weather forecast point.
 */
public record DailyForecast(
        LocalDate date,
        String dayLabel,
        WeatherCondition condition,
        int maximumTemperatureCelsius,
        int minimumTemperatureCelsius,
        int rainProbabilityPercent,
        int windSpeedKmh,
        int uvIndex,
        boolean selected
) {
    public DailyForecast {
        if (dayLabel == null || dayLabel.isBlank()) {
            throw new IllegalArgumentException("Day label is required.");
        }
        date = date == null ? LocalDate.now() : date;
        condition = Objects.requireNonNull(condition, "condition");
        if (rainProbabilityPercent < 0 || rainProbabilityPercent > 100) {
            throw new IllegalArgumentException("Rain probability must be between 0 and 100.");
        }
        if (uvIndex < 0) {
            throw new IllegalArgumentException("UV index cannot be negative.");
        }
    }

    public DailyForecast(
            String dayLabel,
            WeatherCondition condition,
            int maximumTemperatureCelsius,
            int minimumTemperatureCelsius,
            int rainProbabilityPercent,
            int windSpeedKmh,
            int uvIndex,
            boolean selected
    ) {
        this(LocalDate.now(), dayLabel, condition, maximumTemperatureCelsius, minimumTemperatureCelsius,
                rainProbabilityPercent, windSpeedKmh, uvIndex, selected);
    }
}
