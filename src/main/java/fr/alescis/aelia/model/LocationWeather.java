package fr.alescis.aelia.model;

import java.util.Objects;

/**
 * Compact weather summary for the location list.
 */
public record LocationWeather(
        String city,
        String country,
        WeatherCondition condition,
        int temperatureCelsius,
        boolean selected
) {
    public LocationWeather {
        if (city == null || city.isBlank()) {
            throw new IllegalArgumentException("City is required.");
        }
        if (country == null || country.isBlank()) {
            throw new IllegalArgumentException("Country is required.");
        }
        condition = Objects.requireNonNull(condition, "condition");
    }
}
