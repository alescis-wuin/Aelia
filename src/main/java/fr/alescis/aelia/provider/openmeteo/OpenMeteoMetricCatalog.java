package fr.alescis.aelia.provider.openmeteo;

import fr.alescis.aelia.model.ApiLimit;
import fr.alescis.aelia.model.DataCategory;
import fr.alescis.aelia.model.DataMetric;
import fr.alescis.aelia.model.LimitPeriod;
import fr.alescis.aelia.model.WeatherMetric;

import java.util.List;

/**
 * Shared metric and limit catalog for Open-Meteo dashboard and data-provider ports.
 */
public final class OpenMeteoMetricCatalog {
    private OpenMeteoMetricCatalog() {
    }

    public static List<WeatherMetric> weatherMetrics() {
        return List.of(
                new WeatherMetric("temperature", "Température", "Météo", "°C", "Actuel · Horaire · Quotidien", -30.0, 50.0),
                new WeatherMetric("humidity", "Humidité", "Atmosphère", "%", "Actuel", 0.0, 100.0),
                new WeatherMetric("wind", "Vent", "Atmosphère", "km/h", "Actuel · Quotidien", 0.0, 180.0),
                new WeatherMetric("pressure", "Pression", "Atmosphère", "hPa", "Actuel · Horaire", 870.0, 1085.0),
                new WeatherMetric("uv", "Indice UV", "Santé", "", "Actuel · Quotidien", 0.0, 12.0),
                new WeatherMetric("aqi", "IQA européen", "Qualité de l'air", "", "Horaire", 0.0, 150.0),
                new WeatherMetric("pm25", "PM2.5", "Qualité de l'air", "µg/m³", "Horaire", 0.0, 250.0),
                new WeatherMetric("pm10", "PM10", "Qualité de l'air", "µg/m³", "Horaire", 0.0, 400.0),
                new WeatherMetric("no2", "NO₂", "Qualité de l'air", "µg/m³", "Horaire", 0.0, 500.0),
                new WeatherMetric("grass_pollen", "Pollen graminées", "Pollens", "niveau", "Horaire · Europe", 0.0, 4.0),
                new WeatherMetric("birch_pollen", "Pollen bouleau", "Pollens", "niveau", "Horaire · Europe", 0.0, 4.0),
                new WeatherMetric("olive_pollen", "Pollen olivier", "Pollens", "niveau", "Horaire · Europe", 0.0, 4.0),
                new WeatherMetric("ragweed_pollen", "Pollen ambroisie", "Pollens", "niveau", "Horaire · Europe", 0.0, 4.0)
        );
    }

    public static List<DataMetric> dataMetrics() {
        return List.of(
                DataMetric.numeric("temperature", "Température", DataCategory.WEATHER, "°C"),
                DataMetric.numeric("humidity", "Humidité", DataCategory.ATMOSPHERE, "%"),
                DataMetric.numeric("wind", "Vent", DataCategory.ATMOSPHERE, "km/h"),
                DataMetric.numeric("pressure", "Pression", DataCategory.ATMOSPHERE, "hPa"),
                DataMetric.numeric("uv", "Indice UV", DataCategory.HEALTH, ""),
                DataMetric.numeric("aqi", "IQA européen", DataCategory.AIR_QUALITY, ""),
                DataMetric.numeric("pm25", "PM2.5", DataCategory.AIR_QUALITY, "µg/m³"),
                DataMetric.numeric("pm10", "PM10", DataCategory.AIR_QUALITY, "µg/m³"),
                DataMetric.numeric("no2", "NO₂", DataCategory.AIR_QUALITY, "µg/m³"),
                DataMetric.numeric("grass_pollen", "Pollen graminées", DataCategory.POLLEN, "niveau"),
                DataMetric.numeric("birch_pollen", "Pollen bouleau", DataCategory.POLLEN, "niveau"),
                DataMetric.numeric("olive_pollen", "Pollen olivier", DataCategory.POLLEN, "niveau"),
                DataMetric.numeric("ragweed_pollen", "Pollen ambroisie", DataCategory.POLLEN, "niveau")
        );
    }

    public static List<ApiLimit> limits() {
        return List.of(
                ApiLimit.limited("Free API minute limit", LimitPeriod.MINUTE, 600, "requêtes"),
                ApiLimit.limited("Free API hourly limit", LimitPeriod.HOUR, 5000, "requêtes"),
                ApiLimit.limited("Free API daily limit", LimitPeriod.DAY, 10000, "requêtes"),
                ApiLimit.limited("Free API monthly limit", LimitPeriod.MONTH, 300000, "requêtes"),
                new ApiLimit("Commercial use", LimitPeriod.NETWORK.label(), "clé client Open-Meteo requise"),
                new ApiLimit("WeatherAPI.com fallback", LimitPeriod.MONTH.label(), "optionnel · clé requise · Free 100K appels/mois"),
                new ApiLimit("Visual Crossing fallback", LimitPeriod.DAY.label(), "optionnel · clé requise · Free 1000 records/jour"),
                new ApiLimit("OpenWeather fallback", LimitPeriod.MONTH.label(), "optionnel · clé requise · Free 1M appels/mois pour Current/5-day"),
                new ApiLimit("Pirate Weather fallback", LimitPeriod.NETWORK.label(), "optionnel · clé requise · API Dark Sky compatible"),
                new ApiLimit("Weatherbit fallback", LimitPeriod.DAY.label(), "optionnel · clé requise · Free 50 req/jour"),
                new ApiLimit("MET Norway fallback", LimitPeriod.NETWORK.label(), "sans clé · désactivé par défaut · User-Agent et cache obligatoires"),
                new ApiLimit("Attribution", LimitPeriod.NETWORK.label(), "Open-Meteo CC BY 4.0 · sources tierces selon clés configurées")
        );
    }
}
