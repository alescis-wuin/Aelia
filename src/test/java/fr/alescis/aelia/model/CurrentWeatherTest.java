package fr.alescis.aelia.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CurrentWeatherTest {
    @Test
    void daylightProgressIsDerivedFromTypedTimes() {
        CurrentWeather weather = new CurrentWeather(
                "Paris",
                "Ensoleillé",
                LocalDate.of(2025, 5, 25),
                ZoneId.of("Europe/Paris"),
                24,
                28,
                17,
                22,
                LocalTime.of(6, 4),
                LocalTime.of(21, 47),
                62,
                18,
                "SO → NE",
                32,
                1018,
                "↗ En hausse",
                5,
                "Protection solaire recommandée entre 11h et 16h",
                new AirQuality(42, "BON", 12, 28, 18),
                LocalTime.of(15, 30)
        );

        assertEquals(943, weather.daylightDurationValue().toMinutes());
        assertTrue(weather.daylightProgress() > 0.59 && weather.daylightProgress() < 0.61);
    }
}
