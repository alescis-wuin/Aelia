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
        boolean selected,
        Double latitude,
        Double longitude
) {
    public LocationWeather(String city, String country, WeatherCondition condition, int temperatureCelsius, boolean selected) {
        this(city, country, condition, temperatureCelsius, selected, null, null);
    }

    public LocationWeather {
        if (city == null || city.isBlank()) {
            throw new IllegalArgumentException("City is required.");
        }
        if (country == null || country.isBlank()) {
            throw new IllegalArgumentException("Country is required.");
        }
        condition = Objects.requireNonNull(condition, "condition");
        if ((latitude == null) != (longitude == null)) {
            throw new IllegalArgumentException("Latitude and longitude must be both present or both absent.");
        }
        if (latitude != null && (latitude < -90.0 || latitude > 90.0)) {
            throw new IllegalArgumentException("Latitude must be between -90 and 90.");
        }
        if (longitude != null && (longitude < -180.0 || longitude > 180.0)) {
            throw new IllegalArgumentException("Longitude must be between -180 and 180.");
        }
    }

    public boolean hasCoordinates() {
        return latitude != null && longitude != null;
    }
}
