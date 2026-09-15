package fr.alescis.aelia.provider.simulation;

import fr.alescis.aelia.model.AirQuality;
import fr.alescis.aelia.model.ApiLimit;
import fr.alescis.aelia.model.CurrentWeather;
import fr.alescis.aelia.model.DailyForecast;
import fr.alescis.aelia.model.HourlyForecast;
import fr.alescis.aelia.model.LimitPeriod;
import fr.alescis.aelia.model.LocationWeather;
import fr.alescis.aelia.model.PollenLevel;
import fr.alescis.aelia.model.PollenRisk;
import fr.alescis.aelia.model.WeatherCondition;
import fr.alescis.aelia.model.WeatherMetric;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Central catalog for deterministic mockup values.
 */
public final class SimulationCatalog {
    private static final LocalDate MOCK_DATE = LocalDate.of(2026, 5, 26);
    private static final ZoneId PARIS_ZONE = ZoneId.of("Europe/Paris");

    private SimulationCatalog() {
    }

    public static List<WeatherMetric> metrics() {
        return List.of(
                new WeatherMetric("temperature", "Température", "Météo", "°C", "Actuel · Horaire · Quotidien", -20.0, 45.0),
                new WeatherMetric("humidity", "Humidité", "Atmosphère", "%", "Actuel · Horaire", 0.0, 100.0),
                new WeatherMetric("wind", "Vent", "Atmosphère", "km/h", "Actuel · Horaire · Quotidien", 0.0, 140.0),
                new WeatherMetric("pressure", "Pression", "Atmosphère", "hPa", "Actuel · Horaire", 940.0, 1060.0),
                new WeatherMetric("uv", "Indice UV", "Santé", "", "Actuel · Horaire · Quotidien", 0.0, 12.0),
                new WeatherMetric("aqi", "IQA", "Qualité de l'air", "", "Actuel · Horaire", 0.0, 500.0),
                new WeatherMetric("pm25", "PM2.5", "Qualité de l'air", "µg/m³", "Actuel · Horaire", 0.0, 250.0),
                new WeatherMetric("grass_pollen", "Pollen graminées", "Pollens", "niveau", "Actuel · Quotidien", 0.0, 4.0)
        );
    }

    public static List<ApiLimit> limits() {
        return List.of(
                ApiLimit.limited("Current snapshot", LimitPeriod.SECOND, 10, "requêtes"),
                ApiLimit.limited("Subscriptions", LimitPeriod.CLIENT, 25, "flux actifs"),
                ApiLimit.limited("Minimum interval", LimitPeriod.STREAM, 1, "seconde"),
                new ApiLimit("Remote calls", LimitPeriod.NETWORK.label(), "carte uniquement pour géocodage")
        );
    }

    public static Map<String, MetricProfile> metricProfiles() {
        CurrentWeather weather = currentWeather();
        return Map.of(
                "temperature", new MetricProfile("temperature", weather.temperatureCelsius(), 0.35, -20.0, 45.0, "°C"),
                "humidity", new MetricProfile("humidity", weather.humidityPercent(), 1.25, 0.0, 100.0, "%"),
                "wind", new MetricProfile("wind", weather.windSpeedKmh(), 1.5, 0.0, 140.0, "km/h"),
                "pressure", new MetricProfile("pressure", weather.pressureHpa(), 0.8, 940.0, 1060.0, "hPa"),
                "uv", new MetricProfile("uv", weather.uvIndex(), 0.2, 0.0, 12.0, ""),
                "aqi", new MetricProfile("aqi", weather.airQuality().airQualityIndex(), 1.4, 0.0, 500.0, ""),
                "pm25", new MetricProfile("pm25", weather.airQuality().pm25MicrogramsPerCubicMeter(), 0.8, 0.0, 250.0, "µg/m³"),
                "grass_pollen", new MetricProfile("grass_pollen", 3.0, 0.0, 0.0, 4.0, "niveau")
        );
    }

    public static List<LocationWeather> locations() {
        return List.of(
                new LocationWeather("Rouen", "France", WeatherCondition.SUNNY, 32, true, 49.4432, 1.0993),
                new LocationWeather("Tokyo", "Japon", WeatherCondition.PARTLY_CLOUDY, 19, false, 35.6762, 139.6503),
                new LocationWeather("New York", "USA", WeatherCondition.RAINY, 11, false, 40.7128, -74.0060),
                new LocationWeather("Dubaï", "EAU", WeatherCondition.SUNNY, 38, false, 25.2048, 55.2708)
        );
    }

    public static CurrentWeather currentWeather() {
        return new CurrentWeather(
                "Rouen",
                "Ensoleillé",
                MOCK_DATE,
                PARIS_ZONE,
                32,
                33,
                18,
                33,
                LocalTime.of(5, 58),
                LocalTime.of(21, 46),
                33,
                4,
                "NE → SO",
                12,
                1024,
                "↘ En baisse",
                7,
                "Risque UV modéré à élevé selon l'heure.",
                new AirQuality(53, "MODÉRÉ", 10, 21, 4),
                LocalTime.of(19, 13)
        );
    }

    public static List<HourlyForecast> hourlyForecasts() {
        return List.of(
                new HourlyForecast(LocalTime.of(19, 0), WeatherCondition.SUNNY, 32, true),
                new HourlyForecast(LocalTime.of(22, 0), WeatherCondition.CLEAR_NIGHT, 28, false),
                new HourlyForecast(LocalTime.of(1, 0), WeatherCondition.CLEAR_NIGHT, 22, false),
                new HourlyForecast(LocalTime.of(4, 0), WeatherCondition.CLEAR_NIGHT, 20, false),
                new HourlyForecast(LocalTime.of(7, 0), WeatherCondition.SUNNY, 18, false),
                new HourlyForecast(LocalTime.of(10, 0), WeatherCondition.SUNNY, 25, false),
                new HourlyForecast(LocalTime.of(13, 0), WeatherCondition.SUNNY, 30, false),
                new HourlyForecast(LocalTime.of(16, 0), WeatherCondition.SUNNY, 32, false)
        );
    }

    public static List<DailyForecast> dailyForecasts() {
        return List.of(
                new DailyForecast(MOCK_DATE, "Mar. 26", WeatherCondition.PARTLY_CLOUDY, 33, 18, 0, 9, 7, true),
                new DailyForecast(MOCK_DATE.plusDays(1), "Mer. 27", WeatherCondition.PARTLY_CLOUDY, 32, 18, 0, 11, 7, false),
                new DailyForecast(MOCK_DATE.plusDays(2), "Jeu. 28", WeatherCondition.PARTLY_CLOUDY, 33, 21, 5, 13, 7, false),
                new DailyForecast(MOCK_DATE.plusDays(3), "Ven. 29", WeatherCondition.PARTLY_CLOUDY, 34, 18, 3, 13, 7, false),
                new DailyForecast(MOCK_DATE.plusDays(4), "Sam. 30", WeatherCondition.PARTLY_CLOUDY, 29, 17, 3, 17, 7, false),
                new DailyForecast(MOCK_DATE.plusDays(5), "Dim. 31", WeatherCondition.PARTLY_CLOUDY, 20, 14, 18, 27, 4, false),
                new DailyForecast(MOCK_DATE.plusDays(6), "Lun. 1", WeatherCondition.PARTLY_CLOUDY, 22, 13, 22, 14, 5, false)
        );
    }

    public static List<PollenRisk> pollenRisks() {
        return new ArrayList<>(List.of(
                new PollenRisk("Graminées", PollenLevel.MODERATE),
                new PollenRisk("Bouleau", PollenLevel.NONE),
                new PollenRisk("Olivier", PollenLevel.LOW),
                new PollenRisk("Ambroisie", PollenLevel.NONE)
        ));
    }
}
