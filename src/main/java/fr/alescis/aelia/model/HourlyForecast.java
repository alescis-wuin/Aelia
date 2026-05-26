package fr.alescis.aelia.model;

/**
 * Hourly weather forecast point.
 */
public record HourlyForecast(
        String hour,
        WeatherCondition condition,
        int temperatureCelsius,
        boolean selected
) {
    public HourlyForecast {
        if (hour == null || hour.isBlank()) {
            throw new IllegalArgumentException("Hour is required.");
        }
        if (condition == null) {
            throw new IllegalArgumentException("Condition is required.");
        }
    }
}
