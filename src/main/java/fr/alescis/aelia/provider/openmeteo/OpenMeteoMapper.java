package fr.alescis.aelia.provider.openmeteo;

import fr.alescis.aelia.model.AirQuality;
import fr.alescis.aelia.model.ApiLimit;
import fr.alescis.aelia.model.CurrentWeather;
import fr.alescis.aelia.model.DailyForecast;
import fr.alescis.aelia.model.DashboardDataStatus;
import fr.alescis.aelia.model.DashboardSnapshot;
import fr.alescis.aelia.model.HourlyForecast;
import fr.alescis.aelia.model.LocationWeather;
import fr.alescis.aelia.model.PollenLevel;
import fr.alescis.aelia.model.PollenRisk;
import fr.alescis.aelia.model.WeatherCondition;
import fr.alescis.aelia.model.WeatherMetric;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Maps Open-Meteo JSON documents to the dashboard read model.
 */
public final class OpenMeteoMapper {
    private static final Locale FRENCH = Locale.FRANCE;
    private static final DateTimeFormatter LOCAL_DATE_TIME = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public DashboardSnapshot toDashboardSnapshot(
            OpenMeteoLocation selectedLocation,
            OpenMeteoJsonValue forecastPayload,
            OpenMeteoJsonValue airQualityPayload,
            List<LocationWeather> locations,
            List<WeatherMetric> metrics,
            List<ApiLimit> limits
    ) {
        return toDashboardSnapshot(
                selectedLocation,
                forecastPayload,
                airQualityPayload,
                locations,
                metrics,
                limits,
                DashboardDataStatus.forecast("openmeteo")
        );
    }

    public DashboardSnapshot toDashboardSnapshot(
            OpenMeteoLocation selectedLocation,
            OpenMeteoJsonValue forecastPayload,
            OpenMeteoJsonValue airQualityPayload,
            List<LocationWeather> locations,
            List<WeatherMetric> metrics,
            List<ApiLimit> limits,
            DashboardDataStatus status
    ) {
        Objects.requireNonNull(selectedLocation, "selectedLocation");
        Objects.requireNonNull(forecastPayload, "forecastPayload");
        Objects.requireNonNull(airQualityPayload, "airQualityPayload");
        CurrentWeather currentWeather = toCurrentWeather(selectedLocation, forecastPayload, airQualityPayload);
        return new DashboardSnapshot(
                locations,
                currentWeather,
                toHourlyForecasts(selectedLocation, forecastPayload, currentWeather),
                toDailyForecasts(selectedLocation, forecastPayload),
                toPollenRisks(selectedLocation, airQualityPayload),
                metrics,
                limits,
                status
        );
    }

    public LocationWeather toLocationWeather(OpenMeteoLocation location, OpenMeteoJsonValue forecastPayload, boolean selected) {
        OpenMeteoJsonValue current = forecastPayload.get("current");
        int temperature = current.get("temperature_2m").asRoundedInt(location.fallbackTemperatureCelsius());
        int weatherCode = current.get("weather_code").asRoundedInt(0);
        boolean isDay = current.get("is_day").asBoolean(true);
        return new LocationWeather(location.city(), location.country(), conditionFor(weatherCode, isDay), temperature, selected);
    }

    public double metricValue(String metricId, DashboardSnapshot snapshot) {
        CurrentWeather weather = snapshot.currentWeather();
        return switch (metricId) {
            case "temperature" -> weather.temperatureCelsius();
            case "humidity" -> weather.humidityPercent();
            case "wind" -> weather.windSpeedKmh();
            case "pressure" -> weather.pressureHpa();
            case "uv" -> weather.uvIndex();
            case "aqi" -> weather.airQuality().airQualityIndex();
            case "pm25" -> weather.airQuality().pm25MicrogramsPerCubicMeter();
            case "pm10" -> weather.airQuality().pm10MicrogramsPerCubicMeter();
            case "no2" -> weather.airQuality().no2MicrogramsPerCubicMeter();
            case "grass_pollen" -> pollenLevel(snapshot, "Graminées");
            case "birch_pollen" -> pollenLevel(snapshot, "Bouleau");
            case "olive_pollen" -> pollenLevel(snapshot, "Olivier");
            case "ragweed_pollen" -> pollenLevel(snapshot, "Ambroisie");
            default -> throw new IllegalArgumentException("Unsupported metric id: " + metricId);
        };
    }

    private CurrentWeather toCurrentWeather(OpenMeteoLocation location, OpenMeteoJsonValue forecast, OpenMeteoJsonValue airQuality) {
        OpenMeteoJsonValue current = forecast.get("current");
        OpenMeteoJsonValue daily = forecast.get("daily");
        LocalDateTime currentTime = parseDateTime(current.get("time").asString(null), LocalDateTime.now(location.zoneId()));
        LocalDate date = currentTime.toLocalDate();
        int weatherCode = current.get("weather_code").asRoundedInt(0);
        boolean isDay = current.get("is_day").asBoolean(true);
        WeatherCondition condition = conditionFor(weatherCode, isDay);
        LocalTime sunrise = parseTime(firstString(daily, "sunrise", date.atTime(6, 0).toString()), LocalTime.of(6, 0));
        LocalTime sunset = parseTime(firstString(daily, "sunset", date.atTime(21, 0).toString()), LocalTime.of(21, 0));
        int temperature = current.get("temperature_2m").asRoundedInt(location.fallbackTemperatureCelsius());
        int maximum = daily.get("temperature_2m_max").get(0).asRoundedInt(Math.max(temperature, temperature + 3));
        int minimum = daily.get("temperature_2m_min").get(0).asRoundedInt(Math.min(temperature, temperature - 4));
        int apparent = current.get("apparent_temperature").asRoundedInt(temperature);
        int humidity = current.get("relative_humidity_2m").asRoundedInt(0);
        int windSpeed = current.get("wind_speed_10m").asRoundedInt(0);
        int windGust = current.get("wind_gusts_10m").asRoundedInt(windSpeed);
        double windDirectionDegrees = current.get("wind_direction_10m").asDouble(Double.NaN);
        int pressure = current.get("pressure_msl").asRoundedInt(1013);
        int uvIndex = currentUvIndex(location, airQuality, daily);
        AirQuality mappedAirQuality = toAirQuality(location, airQuality);
        return new CurrentWeather(
                location.city(),
                condition.label(),
                date,
                location.zoneId(),
                temperature,
                maximum,
                minimum,
                apparent,
                sunrise,
                sunset,
                clamp(humidity, 0, 100),
                windSpeed,
                windDirectionLabel(windDirectionDegrees),
                windGust,
                pressure,
                pressureTrend(forecast, pressure),
                uvIndex,
                uvAdvice(uvIndex),
                mappedAirQuality,
                LocalTime.now(location.zoneId())
        );
    }

    private List<HourlyForecast> toHourlyForecasts(OpenMeteoLocation location, OpenMeteoJsonValue forecast, CurrentWeather currentWeather) {
        OpenMeteoJsonValue hourly = forecast.get("hourly");
        OpenMeteoJsonValue times = hourly.get("time");
        int start = nearestTimeIndex(times, ZonedDateTime.now(location.zoneId()).toLocalDateTime());
        List<HourlyForecast> result = new ArrayList<>();
        for (int offset = 0; offset < 9; offset++) {
            int index = Math.min(times.size() - 1, start + offset * 3);
            LocalDateTime time = parseDateTime(times.get(index).asString(null), currentWeather.date().atStartOfDay().plusHours(offset * 3L));
            int code = hourly.get("weather_code").get(index).asRoundedInt(0);
            boolean day = !time.toLocalTime().isBefore(currentWeather.sunriseTime()) && !time.toLocalTime().isAfter(currentWeather.sunsetTime());
            int temperature = hourly.get("temperature_2m").get(index).asRoundedInt(currentWeather.temperatureCelsius());
            result.add(new HourlyForecast(time.toLocalTime(), conditionFor(code, day), temperature, offset == 0));
        }
        return result;
    }

    private List<DailyForecast> toDailyForecasts(OpenMeteoLocation location, OpenMeteoJsonValue forecast) {
        OpenMeteoJsonValue daily = forecast.get("daily");
        OpenMeteoJsonValue times = daily.get("time");
        int size = Math.min(7, Math.max(0, times.size()));
        List<DailyForecast> result = new ArrayList<>();
        for (int index = 0; index < size; index++) {
            LocalDate date = parseDate(times.get(index).asString(null), LocalDate.now(location.zoneId()).plusDays(index));
            int code = daily.get("weather_code").get(index).asRoundedInt(0);
            int max = daily.get("temperature_2m_max").get(index).asRoundedInt(0);
            int min = daily.get("temperature_2m_min").get(index).asRoundedInt(0);
            int rain = daily.get("precipitation_probability_max").get(index).asRoundedInt(0);
            int wind = daily.get("wind_speed_10m_max").get(index).asRoundedInt(0);
            int uv = daily.get("uv_index_max").get(index).asRoundedInt(0);
            result.add(new DailyForecast(date, dayLabel(date), conditionFor(code, true), max, min, clamp(rain, 0, 100), wind, uv, index == 0));
        }
        return result;
    }

    private AirQuality toAirQuality(OpenMeteoLocation location, OpenMeteoJsonValue airQuality) {
        OpenMeteoJsonValue hourly = airQuality.get("hourly");
        int index = nearestTimeIndex(hourly.get("time"), ZonedDateTime.now(location.zoneId()).toLocalDateTime());
        int aqi = hourly.get("european_aqi").get(index).asRoundedInt(0);
        int pm25 = hourly.get("pm2_5").get(index).asRoundedInt(0);
        int pm10 = hourly.get("pm10").get(index).asRoundedInt(0);
        int no2 = hourly.get("nitrogen_dioxide").get(index).asRoundedInt(0);
        return new AirQuality(Math.max(0, aqi), airQualityStatus(aqi), Math.max(0, pm25), Math.max(0, pm10), Math.max(0, no2));
    }

    private List<PollenRisk> toPollenRisks(OpenMeteoLocation location, OpenMeteoJsonValue airQuality) {
        OpenMeteoJsonValue hourly = airQuality.get("hourly");
        int index = nearestTimeIndex(hourly.get("time"), ZonedDateTime.now(location.zoneId()).toLocalDateTime());
        return List.of(
                new PollenRisk("Graminées", pollenLevel(hourly.get("grass_pollen").get(index).asDouble(0.0))),
                new PollenRisk("Bouleau", pollenLevel(hourly.get("birch_pollen").get(index).asDouble(0.0))),
                new PollenRisk("Olivier", pollenLevel(hourly.get("olive_pollen").get(index).asDouble(0.0))),
                new PollenRisk("Ambroisie", pollenLevel(hourly.get("ragweed_pollen").get(index).asDouble(0.0)))
        );
    }

    private int currentUvIndex(OpenMeteoLocation location, OpenMeteoJsonValue airQuality, OpenMeteoJsonValue daily) {
        OpenMeteoJsonValue hourly = airQuality.get("hourly");
        int airIndex = nearestTimeIndex(hourly.get("time"), ZonedDateTime.now(location.zoneId()).toLocalDateTime());
        int airUv = hourly.get("uv_index").get(airIndex).asRoundedInt(-1);
        if (airUv >= 0) {
            return airUv;
        }
        return daily.get("uv_index_max").get(0).asRoundedInt(0);
    }

    private String pressureTrend(OpenMeteoJsonValue forecast, int currentPressure) {
        OpenMeteoJsonValue hourly = forecast.get("hourly");
        int index = nearestTimeIndex(hourly.get("time"), LocalDateTime.now());
        int nextIndex = Math.min(index + 1, Math.max(0, hourly.get("time").size() - 1));
        int nextPressure = hourly.get("pressure_msl").get(nextIndex).asRoundedInt(currentPressure);
        if (nextPressure > currentPressure) {
            return "↗ En hausse";
        }
        if (nextPressure < currentPressure) {
            return "↘ En baisse";
        }
        return "→ Stable";
    }

    private int nearestTimeIndex(OpenMeteoJsonValue times, LocalDateTime target) {
        if (times == null || !times.isArray() || times.size() == 0) {
            return 0;
        }
        int bestIndex = 0;
        long bestDistance = Long.MAX_VALUE;
        for (int index = 0; index < times.size(); index++) {
            LocalDateTime candidate = parseDateTime(times.get(index).asString(null), target);
            long distance = Math.abs(Duration.between(candidate, target).toMinutes());
            if (distance < bestDistance) {
                bestDistance = distance;
                bestIndex = index;
            }
        }
        return bestIndex;
    }

    private WeatherCondition conditionFor(int weatherCode, boolean day) {
        if (weatherCode == 0) {
            return day ? WeatherCondition.SUNNY : WeatherCondition.CLEAR_NIGHT;
        }
        if (weatherCode == 1 || weatherCode == 2) {
            return WeatherCondition.PARTLY_CLOUDY;
        }
        if (weatherCode == 3 || weatherCode == 45 || weatherCode == 48) {
            return WeatherCondition.CLOUDY;
        }
        if ((weatherCode >= 51 && weatherCode <= 67) || (weatherCode >= 80 && weatherCode <= 82)) {
            return WeatherCondition.RAINY;
        }
        if ((weatherCode >= 71 && weatherCode <= 77) || (weatherCode >= 85 && weatherCode <= 86)) {
            return WeatherCondition.SNOWY;
        }
        if (weatherCode >= 95 && weatherCode <= 99) {
            return WeatherCondition.STORMY;
        }
        return WeatherCondition.CLOUDY;
    }

    private String windDirectionLabel(double degrees) {
        if (Double.isNaN(degrees)) {
            return "N/A";
        }
        String from = compassPoint(degrees);
        String to = compassPoint((degrees + 180.0) % 360.0);
        return from + " → " + to;
    }

    private String compassPoint(double degrees) {
        String[] labels = {"N", "NE", "E", "SE", "S", "SO", "O", "NO"};
        int index = (int) Math.round((((degrees % 360.0) + 360.0) % 360.0) / 45.0) % labels.length;
        return labels[index];
    }

    private String airQualityStatus(int aqi) {
        if (aqi <= 20) {
            return "BON";
        }
        if (aqi <= 40) {
            return "CORRECT";
        }
        if (aqi <= 60) {
            return "MODÉRÉ";
        }
        if (aqi <= 80) {
            return "MAUVAIS";
        }
        if (aqi <= 100) {
            return "TRÈS MAUVAIS";
        }
        return "EXTRÊME";
    }

    private String uvAdvice(int uvIndex) {
        if (uvIndex <= 2) {
            return "Risque UV faible.";
        }
        if (uvIndex <= 5) {
            return "Protection solaire recommandée en exposition prolongée.";
        }
        if (uvIndex <= 7) {
            return "Protection solaire recommandée entre 11h et 16h";
        }
        if (uvIndex <= 10) {
            return "Éviter l'exposition prolongée entre 11h et 16h";
        }
        return "Exposition UV extrême : éviter le soleil direct.";
    }

    private PollenLevel pollenLevel(double grainsPerCubicMeter) {
        if (grainsPerCubicMeter <= 0.0) {
            return PollenLevel.NONE;
        }
        if (grainsPerCubicMeter <= 10.0) {
            return PollenLevel.LOW;
        }
        if (grainsPerCubicMeter <= 50.0) {
            return PollenLevel.MODERATE;
        }
        if (grainsPerCubicMeter <= 150.0) {
            return PollenLevel.HIGH;
        }
        return PollenLevel.EXTREME;
    }

    private double pollenLevel(DashboardSnapshot snapshot, String name) {
        return snapshot.pollenRisks().stream()
                .filter(risk -> risk.name().equals(name))
                .findFirst()
                .map(risk -> risk.level().normalizedValue() * 4.0)
                .orElse(0.0);
    }

    private String firstString(OpenMeteoJsonValue object, String arrayName, String fallback) {
        return object.get(arrayName).get(0).asString(fallback);
    }

    private LocalDateTime parseDateTime(String value, LocalDateTime fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return LocalDateTime.parse(value, LOCAL_DATE_TIME);
    }

    private LocalDate parseDate(String value, LocalDate fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return LocalDate.parse(value);
    }

    private LocalTime parseTime(String value, LocalTime fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return parseDateTime(value, LocalDate.now().atTime(fallback)).toLocalTime();
    }

    private String dayLabel(LocalDate date) {
        String day = date.getDayOfWeek().getDisplayName(TextStyle.SHORT, FRENCH);
        return capitalize(day) + " " + date.getDayOfMonth();
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private static String capitalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.substring(0, 1).toUpperCase(FRENCH) + value.substring(1);
    }
}
