package fr.alescis.aelia.provider.openmeteo;

import fr.alescis.aelia.model.WeatherCondition;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WeatherApiComFallbackMapperTest {
    @Test
    void mapsWeatherApiForecastToForecastLikePayload() {
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
                  "current": {
                    "last_updated": "2026-05-26 15:00",
                    "temp_c": 21.2,
                    "feelslike_c": 20.8,
                    "humidity": 58,
                    "wind_kph": 14.4,
                    "wind_degree": 230,
                    "gust_kph": 25.0,
                    "pressure_mb": 1016,
                    "uv": 5.0,
                    "is_day": 1,
                    "condition": {"text": "Light rain shower", "code": 1240},
                    "air_quality": {"pm2_5": 9.1, "pm10": 17.2, "no2": 12.0, "us-epa-index": 2}
                  },
                  "forecast": {
                    "forecastday": [
                      {
                        "date": "2026-05-26",
                        "day": {
                          "maxtemp_c": 24.0,
                          "mintemp_c": 15.0,
                          "daily_chance_of_rain": 60,
                          "maxwind_kph": 28.0,
                          "uv": 5,
                          "condition": {"text": "Patchy rain nearby", "code": 1063}
                        },
                        "astro": {"sunrise": "05:56 AM", "sunset": "09:41 PM"},
                        "hour": [
                          {"time": "2026-05-26 15:00", "temp_c": 21.2, "chance_of_rain": 60, "pressure_mb": 1016, "wind_degree": 230, "condition": {"text": "Light rain shower", "code": 1240}},
                          {"time": "2026-05-26 18:00", "temp_c": 19.5, "chance_of_rain": 40, "pressure_mb": 1017, "wind_degree": 240, "condition": {"text": "Partly cloudy", "code": 1003}}
                        ]
                      }
                    ]
                  }
                }
                """);

        WeatherApiComFallbackMapper mapper = new WeatherApiComFallbackMapper();
        OpenMeteoJsonValue forecast = mapper.toForecastLikePayload(location, payload);
        OpenMeteoJsonValue air = mapper.toAirQualityLikePayload(location, payload);

        assertEquals(21, forecast.get("current").get("temperature_2m").asRoundedInt(0));
        assertEquals(61, forecast.get("current").get("weather_code").asRoundedInt(0));
        assertEquals(1, forecast.get("daily").get("time").size());
        assertTrue(air.get("hourly").get("pm2_5").get(0).asDouble(0.0) > 0.0);
    }
}
