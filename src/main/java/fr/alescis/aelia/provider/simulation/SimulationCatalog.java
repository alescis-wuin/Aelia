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
    private static final LocalDate MOCK_DATE = LocalDate.of(2025, 5, 25);
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
                new ApiLimit("Remote calls", LimitPeriod.NETWORK.label(), "aucun en simulation")
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
                new LocationWeather("Paris", "France", WeatherCondition.SUNNY, 24, true),
                new LocationWeather("Tokyo", "Japon", WeatherCondition.PARTLY_CLOUDY, 19, false),
                new LocationWeather("New York", "USA", WeatherCondition.RAINY, 11, false),
                new LocationWeather("Dubaï", "EAU", WeatherCondition.SUNNY, 38, false)
        );
    }

    public static CurrentWeather currentWeather() {
        return new CurrentWeather(
                "Paris",
                "Ensoleillé",
                MOCK_DATE,
                PARIS_ZONE,
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
    }

    public static List<HourlyForecast> hourlyForecasts() {
        return List.of(
                new HourlyForecast(LocalTime.of(9, 0), WeatherCondition.SUNNY, 18, false),
                new HourlyForecast(LocalTime.of(12, 0), WeatherCondition.SUNNY, 24, true),
                new HourlyForecast(LocalTime.of(15, 0), WeatherCondition.SUNNY, 27, false),
                new HourlyForecast(LocalTime.of(18, 0), WeatherCondition.PARTLY_CLOUDY, 23, false),
                new HourlyForecast(LocalTime.of(21, 0), WeatherCondition.CLEAR_NIGHT, 19, false),
                new HourlyForecast(LocalTime.MIDNIGHT, WeatherCondition.CLEAR_NIGHT, 16, false),
                new HourlyForecast(LocalTime.of(3, 0), WeatherCondition.CLEAR_NIGHT, 14, false),
                new HourlyForecast(LocalTime.of(6, 0), WeatherCondition.SUNNY, 15, false),
                new HourlyForecast(LocalTime.of(9, 0), WeatherCondition.SUNNY, 20, false)
        );
    }

    public static List<DailyForecast> dailyForecasts() {
        return List.of(
                new DailyForecast(MOCK_DATE.plusDays(1), "Lun. 26", WeatherCondition.SUNNY, 26, 15, 5, 14, 4, true),
                new DailyForecast(MOCK_DATE.plusDays(2), "Mar. 27", WeatherCondition.PARTLY_CLOUDY, 22, 14, 40, 22, 3, false),
                new DailyForecast(MOCK_DATE.plusDays(3), "Mer. 28", WeatherCondition.RAINY, 19, 13, 75, 28, 2, false),
                new DailyForecast(MOCK_DATE.plusDays(4), "Jeu. 29", WeatherCondition.SUNNY, 27, 16, 0, 16, 6, false),
                new DailyForecast(MOCK_DATE.plusDays(5), "Ven. 30", WeatherCondition.SUNNY, 29, 18, 0, 12, 7, false),
                new DailyForecast(MOCK_DATE.plusDays(6), "Sam. 31", WeatherCondition.SUNNY, 26, 15, 10, 15, 5, false),
                new DailyForecast(MOCK_DATE.plusDays(7), "Dim. 01", WeatherCondition.SUNNY, 28, 17, 0, 18, 5, false)
        );
    }

    public static List<PollenRisk> pollenRisks() {
        return new ArrayList<>(List.of(
                new PollenRisk("Graminées", PollenLevel.HIGH),
                new PollenRisk("Bouleau", PollenLevel.MODERATE),
                new PollenRisk("Olivier", PollenLevel.LOW),
                new PollenRisk("Ambroisie", PollenLevel.NONE)
        ));
    }
}
