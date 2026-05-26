package fr.alescis.aelia.provider.openmeteo;

import fr.alescis.aelia.model.DashboardSnapshot;
import fr.alescis.aelia.model.LocationWeather;
import fr.alescis.aelia.model.WeatherCondition;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class OpenMeteoMapperTest {
    @Test
    void mapsForecastAndAirQualityPayloadsToDashboardSnapshot() {
        OpenMeteoLocation paris = new OpenMeteoLocation("Paris", "France", 48.8566, 2.3522,
                ZoneId.of("Europe/Paris"), WeatherCondition.SUNNY, 24, true);
        OpenMeteoMapper mapper = new OpenMeteoMapper();

        DashboardSnapshot snapshot = mapper.toDashboardSnapshot(
                paris,
                OpenMeteoJsonParser.parse(forecastJson()),
                OpenMeteoJsonParser.parse(airQualityJson()),
                List.of(new LocationWeather("Paris", "France", WeatherCondition.SUNNY, 24, true)),
                OpenMeteoMetricCatalog.weatherMetrics(),
                OpenMeteoMetricCatalog.limits()
        );

        assertEquals("Paris", snapshot.currentWeather().city());
        assertEquals(24, snapshot.currentWeather().temperatureCelsius());
        assertEquals(27, snapshot.currentWeather().maximumTemperatureCelsius());
        assertEquals(15, snapshot.currentWeather().minimumTemperatureCelsius());
        assertEquals(42, snapshot.currentWeather().airQuality().airQualityIndex());
        assertEquals(9, snapshot.hourlyForecasts().size());
        assertEquals(7, snapshot.dailyForecasts().size());
        assertFalse(snapshot.pollenRisks().isEmpty());
    }

    private String forecastJson() {
        return """
                {
                  "current": {
                    "time":"2026-05-26T12:00",
                    "temperature_2m":24.1,
                    "relative_humidity_2m":62,
                    "apparent_temperature":22.4,
                    "weather_code":0,
                    "pressure_msl":1018,
                    "wind_speed_10m":18,
                    "wind_direction_10m":225,
                    "wind_gusts_10m":32,
                    "is_day":1
                  },
                  "hourly": {
                    "time":["2026-05-26T00:00","2026-05-26T03:00","2026-05-26T06:00","2026-05-26T09:00","2026-05-26T12:00","2026-05-26T15:00","2026-05-26T18:00","2026-05-26T21:00","2026-05-27T00:00","2026-05-27T03:00","2026-05-27T06:00","2026-05-27T09:00","2026-05-27T12:00"],
                    "temperature_2m":[14,15,18,21,24,27,23,19,16,14,15,20,24],
                    "weather_code":[0,0,0,0,0,0,2,0,0,0,0,0,0],
                    "pressure_msl":[1016,1016,1017,1018,1018,1019,1019,1018,1018,1017,1018,1018,1019]
                  },
                  "daily": {
                    "time":["2026-05-26","2026-05-27","2026-05-28","2026-05-29","2026-05-30","2026-05-31","2026-06-01"],
                    "weather_code":[0,2,61,0,0,0,0],
                    "temperature_2m_max":[27,22,19,27,29,26,28],
                    "temperature_2m_min":[15,14,13,16,18,15,17],
                    "sunrise":["2026-05-26T06:01","2026-05-27T06:00","2026-05-28T05:59","2026-05-29T05:58","2026-05-30T05:57","2026-05-31T05:57","2026-06-01T05:56"],
                    "sunset":["2026-05-26T21:45","2026-05-27T21:46","2026-05-28T21:47","2026-05-29T21:48","2026-05-30T21:49","2026-05-31T21:50","2026-06-01T21:51"],
                    "uv_index_max":[5,3,2,6,7,5,5],
                    "precipitation_probability_max":[5,40,75,0,0,10,0],
                    "wind_speed_10m_max":[14,22,28,16,12,15,18]
                  }
                }
                """;
    }

    private String airQualityJson() {
        return """
                {
                  "hourly": {
                    "time":["2026-05-26T00:00","2026-05-26T03:00","2026-05-26T06:00","2026-05-26T09:00","2026-05-26T12:00","2026-05-26T15:00"],
                    "european_aqi":[42,42,42,42,42,42],
                    "pm2_5":[12,12,12,12,12,12],
                    "pm10":[28,28,28,28,28,28],
                    "nitrogen_dioxide":[18,18,18,18,18,18],
                    "uv_index":[0,0,1,3,5,5],
                    "grass_pollen":[80,90,95,100,110,120],
                    "birch_pollen":[20,22,24,26,28,30],
                    "olive_pollen":[4,4,5,5,6,6],
                    "ragweed_pollen":[0,0,0,0,0,0]
                  }
                }
                """;
    }
}
