package fr.alescis.aelia.model;

/**
 * Daily weather forecast point.
 */
public record DailyForecast(
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
        if (condition == null) {
            throw new IllegalArgumentException("Condition is required.");
        }
        if (rainProbabilityPercent < 0 || rainProbabilityPercent > 100) {
            throw new IllegalArgumentException("Rain probability must be between 0 and 100.");
        }
        if (uvIndex < 0) {
            throw new IllegalArgumentException("UV index cannot be negative.");
        }
    }
}
