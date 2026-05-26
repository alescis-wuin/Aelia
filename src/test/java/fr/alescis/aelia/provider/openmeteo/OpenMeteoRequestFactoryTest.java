package fr.alescis.aelia.provider.openmeteo;

import fr.alescis.aelia.model.WeatherCondition;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Duration;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenMeteoRequestFactoryTest {
    @Test
    void buildsForecastUriWithExpectedVariablesAndUnits() {
        OpenMeteoRequestFactory factory = new OpenMeteoRequestFactory(configuration());
        URI uri = factory.fullForecastUri(location());
        String value = uri.toString();

        assertTrue(value.startsWith("https://api.open-meteo.com/v1/forecast?"));
        assertTrue(value.contains("latitude=48.8566"));
        assertTrue(value.contains("longitude=2.3522"));
        assertTrue(value.contains("temperature_2m"));
        assertTrue(value.contains("apparent_temperature"));
        assertTrue(value.contains("sunrise"));
        assertTrue(value.contains("timezone=Europe%2FParis"));
        assertTrue(value.contains("wind_speed_unit=kmh"));
    }



    @Test
    void buildsReducedForecastUriWithoutSecondaryCurrentVariables() {
        OpenMeteoRequestFactory factory = new OpenMeteoRequestFactory(configuration());
        String value = factory.reducedForecastUri(location()).toString();

        assertTrue(value.contains("temperature_2m"));
        assertTrue(value.contains("relative_humidity_2m"));
        assertTrue(value.contains("sunrise"));
        assertTrue(value.contains("precipitation_probability_max"));
        assertTrue(!value.contains("cloud_cover"));
        assertTrue(!value.contains("showers"));
    }

    @Test
    void buildsAirQualityUriWithPollensAndEuropeanAqi() {
        OpenMeteoRequestFactory factory = new OpenMeteoRequestFactory(configuration());
        String value = factory.airQualityUri(location()).toString();

        assertTrue(value.contains("european_aqi"));
        assertTrue(value.contains("pm2_5"));
        assertTrue(value.contains("nitrogen_dioxide"));
        assertTrue(value.contains("grass_pollen"));
        assertTrue(value.contains("ragweed_pollen"));
    }

    @Test
    void buildsHistoricalArchiveUriForFallbackValues() {
        OpenMeteoRequestFactory factory = new OpenMeteoRequestFactory(configuration());
        String value = factory.archiveFallbackUri(location()).toString();

        assertTrue(value.startsWith("https://archive-api.open-meteo.com/v1/archive?"));
        assertTrue(value.contains("start_date="));
        assertTrue(value.contains("end_date="));
        assertTrue(value.contains("relative_humidity_2m"));
        assertTrue(value.contains("apparent_temperature"));
        assertTrue(value.contains("pressure_msl"));
        assertTrue(value.contains("wind_gusts_10m"));
    }

    private OpenMeteoConfiguration configuration() {
        return new OpenMeteoConfiguration(
                URI.create("https://api.open-meteo.com/v1/forecast"),
                URI.create("https://air-quality-api.open-meteo.com/v1/air-quality"),
                URI.create("https://archive-api.open-meteo.com/v1/archive"),
                URI.create("https://geocoding-api.open-meteo.com/v1/search"),
                "",
                "Aelia-Test/1.0",
                List.of(location()),
                Duration.ofSeconds(1),
                Duration.ofSeconds(1),
                Duration.ofMinutes(1),
                Duration.ofMinutes(1),
                Duration.ofMinutes(1),
                Duration.ofMinutes(1),
                2
        );
    }

    private OpenMeteoLocation location() {
        return new OpenMeteoLocation("Paris", "France", 48.8566, 2.3522,
                ZoneId.of("Europe/Paris"), WeatherCondition.SUNNY, 24, true);
    }
}
