package fr.alescis.aelia.provider.openmeteo;

import fr.alescis.aelia.model.LocationWeather;
import fr.alescis.aelia.model.WeatherCondition;

import java.time.ZoneId;
import java.util.Objects;

/**
 * Immutable geographical location used by Open-Meteo requests and dashboard summaries.
 */
public record OpenMeteoLocation(
        String city,
        String country,
        double latitude,
        double longitude,
        ZoneId zoneId,
        WeatherCondition fallbackCondition,
        int fallbackTemperatureCelsius,
        boolean selected
) {
    public OpenMeteoLocation {
        city = requireText(city, "city");
        country = requireText(country, "country");
        if (latitude < -90.0 || latitude > 90.0) {
            throw new IllegalArgumentException("Latitude must be between -90 and 90 degrees.");
        }
        if (longitude < -180.0 || longitude > 180.0) {
            throw new IllegalArgumentException("Longitude must be between -180 and 180 degrees.");
        }
        zoneId = Objects.requireNonNull(zoneId, "zoneId");
        fallbackCondition = Objects.requireNonNull(fallbackCondition, "fallbackCondition");
    }

    public OpenMeteoLocation withSelected(boolean selectedValue) {
        return new OpenMeteoLocation(city, country, latitude, longitude, zoneId, fallbackCondition,
                fallbackTemperatureCelsius, selectedValue);
    }

    public LocationWeather fallbackSummary(boolean selectedValue) {
        return new LocationWeather(city, country, fallbackCondition, fallbackTemperatureCelsius, selectedValue);
    }

    private static String requireText(String value, String fieldName) {
        String normalized = Objects.requireNonNull(value, fieldName).trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank.");
        }
        return normalized;
    }
}
