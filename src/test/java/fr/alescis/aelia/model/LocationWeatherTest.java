package fr.alescis.aelia.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocationWeatherTest {
    @Test
    void supportsOptionalCoordinates() {
        LocationWeather simple = new LocationWeather("Rouen", "France", WeatherCondition.SUNNY, 32, true);
        LocationWeather mapped = new LocationWeather("Rouen", "France", WeatherCondition.SUNNY, 32, true, 49.4432, 1.0993);

        assertFalse(simple.hasCoordinates());
        assertTrue(mapped.hasCoordinates());
    }

    @Test
    void rejectsInvalidCoordinates() {
        assertThrows(IllegalArgumentException.class,
                () -> new LocationWeather("X", "Y", WeatherCondition.CLOUDY, 20, false, 91.0, 0.0));
        assertThrows(IllegalArgumentException.class,
                () -> new LocationWeather("X", "Y", WeatherCondition.CLOUDY, 20, false, 0.0, 181.0));
    }
}
