package fr.alescis.aelia.provider.openmeteo;

import fr.alescis.aelia.model.WeatherCondition;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MetNorwayForecastFallbackMapperTest {
    @Test
    void mapsCompactLocationforecastToForecastLikePayload() {
        OpenMeteoLocation location = new OpenMeteoLocation(
                "Paris",
                "France",
                48.8566,
                2.3522,
                ZoneId.of("Europe/Paris"),
                WeatherCondition.SUNNY,
                24,
                true
        );
        OpenMeteoJsonValue payload = OpenMeteoJsonParser.parse("""
                {
                  "properties": {
                    "timeseries": [
                      {
                        "time": "2026-05-26T12:00:00Z",
                        "data": {
                          "instant": {"details": {
                            "air_temperature": 18.5,
                            "relative_humidity": 63,
                            "air_pressure_at_sea_level": 1012.4,
                            "wind_speed": 5.0,
                            "wind_from_direction": 210,
                            "wind_speed_of_gust": 9.0
                          }},
                          "next_1_hours": {
                            "summary": {"symbol_code": "rainshowers_day"},
                            "details": {"precipitation_amount": 0.5, "probability_of_precipitation": 70}
                          }
                        }
                      },
                      {
                        "time": "2026-05-26T15:00:00Z",
                        "data": {
                          "instant": {"details": {
                            "air_temperature": 20.0,
                            "relative_humidity": 55,
                            "air_pressure_at_sea_level": 1011.0,
                            "wind_speed": 4.0,
                            "wind_from_direction": 220
                          }},
                          "next_1_hours": {
                            "summary": {"symbol_code": "partlycloudy_day"},
                            "details": {"precipitation_amount": 0.0}
                          }
                        }
                      }
                    ]
                  }
                }
                """);

        OpenMeteoJsonValue mapped = new MetNorwayForecastFallbackMapper().toForecastLikePayload(location, payload);

        assertTrue(mapped.get("current").get("temperature_2m").asDouble(0.0) > 0.0);
        assertEquals(80, mapped.get("current").get("weather_code").asRoundedInt(0));
        assertEquals(18, mapped.get("current").get("wind_speed_10m").asRoundedInt(0));
        assertTrue(mapped.get("hourly").get("time").size() >= 1);
        assertEquals(1, mapped.get("daily").get("time").size());
    }
}
