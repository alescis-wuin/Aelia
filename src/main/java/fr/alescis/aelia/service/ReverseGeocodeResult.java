package fr.alescis.aelia.service;

import fr.alescis.aelia.model.LocationWeather;
import fr.alescis.aelia.model.WeatherCondition;

import java.util.Locale;

/**
 * Reverse-geocoded display data for a clicked map coordinate.
 */
public record ReverseGeocodeResult(
        String displayName,
        String city,
        String country,
        double latitude,
        double longitude
) {
    public ReverseGeocodeResult {
        displayName = normalize(displayName, coordinateLabel(latitude, longitude));
        city = normalize(city, coordinateLabel(latitude, longitude));
        country = normalize(country, "Coordonnées");
    }

    public static ReverseGeocodeResult coordinatesOnly(double latitude, double longitude) {
        String coordinates = coordinateLabel(latitude, longitude);
        return new ReverseGeocodeResult(coordinates, coordinates, "Coordonnées", latitude, longitude);
    }

    public LocationWeather toLocationWeather() {
        return new LocationWeather(city, country, WeatherCondition.CLOUDY, estimatedTemperature(), false, latitude, longitude);
    }

    private int estimatedTemperature() {
        double latitudeFactor = Math.max(0.0, 1.0 - Math.abs(latitude) / 90.0);
        return (int) Math.round(2.0 + latitudeFactor * 24.0);
    }

    private static String coordinateLabel(double latitude, double longitude) {
        return String.format(Locale.ROOT, "%.4f, %.4f", latitude, longitude);
    }

    private static String normalize(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }
}
