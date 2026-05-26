package fr.alescis.aelia.model;

/**
 * Current weather and environmental indicators for the selected location.
 */
public record CurrentWeather(
        String city,
        String conditionLabel,
        String dateLabel,
        int temperatureCelsius,
        int maximumTemperatureCelsius,
        int minimumTemperatureCelsius,
        int apparentTemperatureCelsius,
        String sunrise,
        String sunset,
        int humidityPercent,
        int windSpeedKmh,
        String windDirection,
        int windGustKmh,
        int pressureHpa,
        String pressureTrend,
        int uvIndex,
        String uvAdvice,
        AirQuality airQuality,
        String daylightDuration,
        String currentSolarTime
) {
    public CurrentWeather {
        if (city == null || city.isBlank()) {
            throw new IllegalArgumentException("City is required.");
        }
        if (conditionLabel == null || conditionLabel.isBlank()) {
            throw new IllegalArgumentException("Condition label is required.");
        }
        if (airQuality == null) {
            throw new IllegalArgumentException("Air quality is required.");
        }
    }
}
