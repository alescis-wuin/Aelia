package fr.alescis.aelia.model;

/**
 * Air quality summary displayed in the environment column.
 */
public record AirQuality(
        int airQualityIndex,
        String status,
        int pm25MicrogramsPerCubicMeter,
        int pm10MicrogramsPerCubicMeter,
        int no2MicrogramsPerCubicMeter
) {
    public AirQuality {
        if (airQualityIndex < 0) {
            throw new IllegalArgumentException("Air quality index cannot be negative.");
        }
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("Air quality status is required.");
        }
        if (pm25MicrogramsPerCubicMeter < 0 || pm10MicrogramsPerCubicMeter < 0 || no2MicrogramsPerCubicMeter < 0) {
            throw new IllegalArgumentException("Pollutant values cannot be negative.");
        }
    }
}
