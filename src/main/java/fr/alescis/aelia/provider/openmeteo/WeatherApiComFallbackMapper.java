package fr.alescis.aelia.provider.openmeteo;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Converts WeatherAPI.com forecast JSON to the forecast-shaped payload consumed by the dashboard mapper.
 */
public final class WeatherApiComFallbackMapper {
    private static final DateTimeFormatter WEATHER_API_DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd H:mm", Locale.ROOT);
    private static final DateTimeFormatter WEATHER_API_TIME = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);

    public OpenMeteoJsonValue toForecastLikePayload(OpenMeteoLocation location, OpenMeteoJsonValue payload) {
        Objects.requireNonNull(location, "location");
        OpenMeteoJsonValue current = payload.get("current");
        OpenMeteoJsonValue forecastDays = payload.get("forecast").get("forecastday");
        if (!current.isPresent() || !forecastDays.isArray() || forecastDays.size() == 0) {
            throw new OpenMeteoException("WeatherAPI.com forecast payload does not contain current and forecastday sections.");
        }
        Map<String, OpenMeteoJsonValue> root = new LinkedHashMap<>();
        root.put("current", object(currentObject(location, current)));
        root.put("hourly", hourlyObject(forecastDays));
        root.put("daily", dailyObject(forecastDays));
        return object(root);
    }

    public OpenMeteoJsonValue toAirQualityLikePayload(OpenMeteoLocation location, OpenMeteoJsonValue payload) {
        Objects.requireNonNull(location, "location");
        OpenMeteoJsonValue airQuality = payload.get("current").get("air_quality");
        if (!airQuality.isPresent()) {
            return OpenMeteoJsonValue.missing();
        }
        LocalDateTime now = LocalDateTime.now(location.zoneId());
        Map<String, OpenMeteoJsonValue> hourly = new LinkedHashMap<>();
        hourly.put("time", array(List.of(value(now.toString()))));
        hourly.put("pm2_5", array(List.of(value(airQuality.get("pm2_5").asDouble(0.0)))));
        hourly.put("pm10", array(List.of(value(airQuality.get("pm10").asDouble(0.0)))));
        hourly.put("nitrogen_dioxide", array(List.of(value(airQuality.get("no2").asDouble(0.0)))));
        hourly.put("uv_index", array(List.of(value(payload.get("current").get("uv").asDouble(0.0)))));
        hourly.put("european_aqi", array(List.of(value(epaIndexToApproximateEuropeanAqi(airQuality.get("us-epa-index").asRoundedInt(0))))));
        hourly.put("grass_pollen", array(List.of(value(0.0))));
        hourly.put("birch_pollen", array(List.of(value(0.0))));
        hourly.put("olive_pollen", array(List.of(value(0.0))));
        hourly.put("ragweed_pollen", array(List.of(value(0.0))));
        return object(Map.of("hourly", object(hourly)));
    }

    private Map<String, OpenMeteoJsonValue> currentObject(OpenMeteoLocation location, OpenMeteoJsonValue current) {
        LocalDateTime currentTime = parseDateTime(current.get("last_updated").asString(null), LocalDateTime.now(location.zoneId()));
        Map<String, OpenMeteoJsonValue> mapped = new LinkedHashMap<>();
        mapped.put("time", value(currentTime.toString()));
        mapped.put("temperature_2m", value(current.get("temp_c").asDouble(location.fallbackTemperatureCelsius())));
        mapped.put("relative_humidity_2m", value(current.get("humidity").asDouble(0.0)));
        mapped.put("apparent_temperature", value(current.get("feelslike_c").asDouble(current.get("temp_c").asDouble(location.fallbackTemperatureCelsius()))));
        mapped.put("weather_code", value(weatherCode(current.get("condition"))));
        mapped.put("pressure_msl", value(current.get("pressure_mb").asDouble(1013.0)));
        mapped.put("wind_speed_10m", value(current.get("wind_kph").asDouble(0.0)));
        mapped.put("wind_direction_10m", value(current.get("wind_degree").asDouble(Double.NaN)));
        mapped.put("wind_gusts_10m", value(current.get("gust_kph").asDouble(current.get("wind_kph").asDouble(0.0))));
        mapped.put("is_day", value(current.get("is_day").asRoundedInt(1)));
        return mapped;
    }

    private OpenMeteoJsonValue hourlyObject(OpenMeteoJsonValue forecastDays) {
        List<OpenMeteoJsonValue> times = new ArrayList<>();
        List<OpenMeteoJsonValue> temperatures = new ArrayList<>();
        List<OpenMeteoJsonValue> weatherCodes = new ArrayList<>();
        List<OpenMeteoJsonValue> precipitationProbabilities = new ArrayList<>();
        List<OpenMeteoJsonValue> pressures = new ArrayList<>();
        for (OpenMeteoJsonValue forecastDay : forecastDays.asArray()) {
            for (OpenMeteoJsonValue hour : forecastDay.get("hour").asArray()) {
                LocalDateTime time = parseDateTime(hour.get("time").asString(null), LocalDateTime.now());
                times.add(value(time.toString()));
                temperatures.add(value(hour.get("temp_c").asDouble(0.0)));
                weatherCodes.add(value(weatherCode(hour.get("condition"))));
                precipitationProbabilities.add(value(hour.get("chance_of_rain").asDouble(0.0)));
                pressures.add(value(hour.get("pressure_mb").asDouble(1013.0)));
            }
        }
        Map<String, OpenMeteoJsonValue> hourly = new LinkedHashMap<>();
        hourly.put("time", array(times));
        hourly.put("temperature_2m", array(temperatures));
        hourly.put("weather_code", array(weatherCodes));
        hourly.put("precipitation_probability", array(precipitationProbabilities));
        hourly.put("pressure_msl", array(pressures));
        return object(hourly);
    }

    private OpenMeteoJsonValue dailyObject(OpenMeteoJsonValue forecastDays) {
        List<OpenMeteoJsonValue> dates = new ArrayList<>();
        List<OpenMeteoJsonValue> weatherCodes = new ArrayList<>();
        List<OpenMeteoJsonValue> maxTemperatures = new ArrayList<>();
        List<OpenMeteoJsonValue> minTemperatures = new ArrayList<>();
        List<OpenMeteoJsonValue> sunrises = new ArrayList<>();
        List<OpenMeteoJsonValue> sunsets = new ArrayList<>();
        List<OpenMeteoJsonValue> daylightDurations = new ArrayList<>();
        List<OpenMeteoJsonValue> uv = new ArrayList<>();
        List<OpenMeteoJsonValue> rain = new ArrayList<>();
        List<OpenMeteoJsonValue> wind = new ArrayList<>();
        List<OpenMeteoJsonValue> windDirection = new ArrayList<>();
        for (OpenMeteoJsonValue forecastDay : forecastDays.asArray()) {
            LocalDate date = parseDate(forecastDay.get("date").asString(null), LocalDate.now());
            OpenMeteoJsonValue day = forecastDay.get("day");
            OpenMeteoJsonValue astro = forecastDay.get("astro");
            LocalTime sunrise = parseTime(astro.get("sunrise").asString(null), LocalTime.of(6, 0));
            LocalTime sunset = parseTime(astro.get("sunset").asString(null), LocalTime.of(21, 0));
            dates.add(value(date.toString()));
            weatherCodes.add(value(weatherCode(day.get("condition"))));
            maxTemperatures.add(value(day.get("maxtemp_c").asDouble(0.0)));
            minTemperatures.add(value(day.get("mintemp_c").asDouble(0.0)));
            sunrises.add(value(LocalDateTime.of(date, sunrise).toString()));
            sunsets.add(value(LocalDateTime.of(date, sunset).toString()));
            daylightDurations.add(value(Math.max(0L, Duration.between(sunrise, sunset).toSeconds())));
            uv.add(value(day.get("uv").asDouble(0.0)));
            rain.add(value(day.get("daily_chance_of_rain").asDouble(0.0)));
            wind.add(value(day.get("maxwind_kph").asDouble(0.0)));
            windDirection.add(value(dominantWindDirection(forecastDay.get("hour"))));
        }
        Map<String, OpenMeteoJsonValue> daily = new LinkedHashMap<>();
        daily.put("time", array(dates));
        daily.put("weather_code", array(weatherCodes));
        daily.put("temperature_2m_max", array(maxTemperatures));
        daily.put("temperature_2m_min", array(minTemperatures));
        daily.put("sunrise", array(sunrises));
        daily.put("sunset", array(sunsets));
        daily.put("daylight_duration", array(daylightDurations));
        daily.put("uv_index_max", array(uv));
        daily.put("precipitation_probability_max", array(rain));
        daily.put("wind_speed_10m_max", array(wind));
        daily.put("wind_direction_10m_dominant", array(windDirection));
        return object(daily);
    }

    private double dominantWindDirection(OpenMeteoJsonValue hours) {
        double x = 0.0;
        double y = 0.0;
        for (OpenMeteoJsonValue hour : hours.asArray()) {
            double degrees = hour.get("wind_degree").asDouble(Double.NaN);
            if (Double.isNaN(degrees)) {
                continue;
            }
            double radians = Math.toRadians(degrees);
            x += Math.sin(radians);
            y += Math.cos(radians);
        }
        if (x == 0.0 && y == 0.0) {
            return 0.0;
        }
        double degrees = Math.toDegrees(Math.atan2(x, y));
        return degrees < 0.0 ? degrees + 360.0 : degrees;
    }

    private int weatherCode(OpenMeteoJsonValue condition) {
        int code = condition.get("code").asRoundedInt(-1);
        String text = condition.get("text").asString("").toLowerCase(Locale.ROOT);
        if (code == 1000 || text.contains("clear") || text.contains("sunny") || text.contains("ensoleillé")) {
            return 0;
        }
        if (code == 1003 || text.contains("partly")) {
            return 2;
        }
        if (code == 1006 || code == 1009 || text.contains("cloud") || text.contains("couvert") || text.contains("nuage")) {
            return 3;
        }
        if (text.contains("thunder") || text.contains("orage")) {
            return 95;
        }
        if (text.contains("snow") || text.contains("neige")) {
            return text.contains("heavy") ? 75 : 71;
        }
        if (text.contains("sleet") || text.contains("ice pellets") || text.contains("grésil")) {
            return 67;
        }
        if (text.contains("rain") || text.contains("drizzle") || text.contains("pluie")) {
            return text.contains("heavy") || text.contains("forte") ? 65 : 61;
        }
        if (text.contains("fog") || text.contains("mist") || text.contains("brouillard") || code == 1030 || code == 1135 || code == 1147) {
            return 45;
        }
        return 3;
    }

    private int epaIndexToApproximateEuropeanAqi(int epaIndex) {
        return switch (epaIndex) {
            case 1 -> 20;
            case 2 -> 40;
            case 3 -> 60;
            case 4 -> 80;
            case 5 -> 100;
            case 6 -> 150;
            default -> 0;
        };
    }

    private LocalDateTime parseDateTime(String value, LocalDateTime fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return LocalDateTime.parse(value.replace(' ', 'T'));
        } catch (DateTimeParseException ignored) {
            try {
                return LocalDateTime.parse(value, WEATHER_API_DATE_TIME);
            } catch (DateTimeParseException exception) {
                return fallback;
            }
        }
    }

    private LocalDate parseDate(String value, LocalDate fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException exception) {
            return fallback;
        }
    }

    private LocalTime parseTime(String value, LocalTime fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return LocalTime.parse(value.toUpperCase(Locale.ENGLISH), WEATHER_API_TIME);
        } catch (DateTimeParseException exception) {
            return fallback;
        }
    }

    private static OpenMeteoJsonValue object(Map<String, OpenMeteoJsonValue> values) {
        return new OpenMeteoJsonValue(new LinkedHashMap<>(values));
    }

    private static OpenMeteoJsonValue array(List<OpenMeteoJsonValue> values) {
        return new OpenMeteoJsonValue(new ArrayList<>(values));
    }

    private static OpenMeteoJsonValue value(Object value) {
        return new OpenMeteoJsonValue(value);
    }
}
