package fr.alescis.aelia.provider.openmeteo;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Converts Pirate Weather JSON to Open-Meteo-shaped dashboard data.
 */
public final class PirateWeatherForecastFallbackMapper {
    public OpenMeteoJsonValue toForecastLikePayload(OpenMeteoLocation location, OpenMeteoJsonValue payload) {
        Objects.requireNonNull(location, "location");
        OpenMeteoJsonValue current = payload.get("currently");
        OpenMeteoJsonValue hourly = payload.get("hourly").get("data");
        OpenMeteoJsonValue daily = payload.get("daily").get("data");
        if (!current.isPresent() || !hourly.isArray() || hourly.size() == 0 || !daily.isArray() || daily.size() == 0) {
            throw new OpenMeteoException("Pirate Weather payload does not contain currently, hourly.data and daily.data sections.");
        }
        Map<String, OpenMeteoJsonValue> root = new LinkedHashMap<>();
        root.put("current", JsonValueBuilder.object(currentObject(location, current)));
        root.put("hourly", hourlyObject(location.zoneId(), hourly));
        root.put("daily", dailyObject(location.zoneId(), daily));
        return JsonValueBuilder.object(root);
    }

    private Map<String, OpenMeteoJsonValue> currentObject(OpenMeteoLocation location, OpenMeteoJsonValue current) {
        ZoneId zoneId = location.zoneId();
        LocalDateTime time = fromUnix(current.get("time").asDouble(Double.NaN), zoneId, LocalDateTime.now(zoneId));
        Map<String, OpenMeteoJsonValue> mapped = new LinkedHashMap<>();
        mapped.put("time", JsonValueBuilder.value(time.toString()));
        mapped.put("temperature_2m", JsonValueBuilder.value(current.get("temperature").asDouble(location.fallbackTemperatureCelsius())));
        mapped.put("relative_humidity_2m", JsonValueBuilder.value(current.get("humidity").asDouble(0.0) * 100.0));
        mapped.put("apparent_temperature", JsonValueBuilder.value(current.get("apparentTemperature").asDouble(current.get("temperature").asDouble(location.fallbackTemperatureCelsius()))));
        mapped.put("weather_code", JsonValueBuilder.value(weatherCode(current.get("icon").asString("cloudy"), current.get("summary").asString(""))));
        mapped.put("pressure_msl", JsonValueBuilder.value(current.get("pressure").asDouble(1013.0)));
        mapped.put("wind_speed_10m", JsonValueBuilder.value(current.get("windSpeed").asDouble(0.0)));
        mapped.put("wind_direction_10m", JsonValueBuilder.value(current.get("windBearing").asDouble(Double.NaN)));
        mapped.put("wind_gusts_10m", JsonValueBuilder.value(current.get("windGust").asDouble(current.get("windSpeed").asDouble(0.0))));
        mapped.put("is_day", JsonValueBuilder.value(isDay(time.toLocalTime()) ? 1 : 0));
        return mapped;
    }

    private OpenMeteoJsonValue hourlyObject(ZoneId zoneId, OpenMeteoJsonValue hours) {
        List<OpenMeteoJsonValue> times = new ArrayList<>();
        List<OpenMeteoJsonValue> temperatures = new ArrayList<>();
        List<OpenMeteoJsonValue> weatherCodes = new ArrayList<>();
        List<OpenMeteoJsonValue> precipitationProbabilities = new ArrayList<>();
        List<OpenMeteoJsonValue> pressures = new ArrayList<>();
        int limit = Math.min(96, hours.size());
        for (int index = 0; index < limit; index++) {
            OpenMeteoJsonValue hour = hours.get(index);
            LocalDateTime time = fromUnix(hour.get("time").asDouble(Double.NaN), zoneId, LocalDateTime.now(zoneId).plusHours(index));
            times.add(JsonValueBuilder.value(time.toString()));
            temperatures.add(JsonValueBuilder.value(hour.get("temperature").asDouble(0.0)));
            weatherCodes.add(JsonValueBuilder.value(weatherCode(hour.get("icon").asString("cloudy"), hour.get("summary").asString(""))));
            precipitationProbabilities.add(JsonValueBuilder.value(hour.get("precipProbability").asDouble(0.0) * 100.0));
            pressures.add(JsonValueBuilder.value(hour.get("pressure").asDouble(1013.0)));
        }
        Map<String, OpenMeteoJsonValue> hourly = new LinkedHashMap<>();
        hourly.put("time", JsonValueBuilder.array(times));
        hourly.put("temperature_2m", JsonValueBuilder.array(temperatures));
        hourly.put("weather_code", JsonValueBuilder.array(weatherCodes));
        hourly.put("precipitation_probability", JsonValueBuilder.array(precipitationProbabilities));
        hourly.put("pressure_msl", JsonValueBuilder.array(pressures));
        return JsonValueBuilder.object(hourly);
    }

    private OpenMeteoJsonValue dailyObject(ZoneId zoneId, OpenMeteoJsonValue days) {
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
        int limit = Math.min(7, days.size());
        for (int index = 0; index < limit; index++) {
            OpenMeteoJsonValue day = days.get(index);
            LocalDateTime dateTime = fromUnix(day.get("time").asDouble(Double.NaN), zoneId, LocalDate.now(zoneId).plusDays(index).atStartOfDay());
            LocalDate date = dateTime.toLocalDate();
            LocalDateTime sunrise = fromUnix(day.get("sunriseTime").asDouble(Double.NaN), zoneId, date.atTime(6, 0));
            LocalDateTime sunset = fromUnix(day.get("sunsetTime").asDouble(Double.NaN), zoneId, date.atTime(21, 0));
            dates.add(JsonValueBuilder.value(date.toString()));
            weatherCodes.add(JsonValueBuilder.value(weatherCode(day.get("icon").asString("cloudy"), day.get("summary").asString(""))));
            maxTemperatures.add(JsonValueBuilder.value(day.get("temperatureHigh").asDouble(day.get("temperatureMax").asDouble(0.0))));
            minTemperatures.add(JsonValueBuilder.value(day.get("temperatureLow").asDouble(day.get("temperatureMin").asDouble(0.0))));
            sunrises.add(JsonValueBuilder.value(sunrise.toString()));
            sunsets.add(JsonValueBuilder.value(sunset.toString()));
            daylightDurations.add(JsonValueBuilder.value(Math.max(0L, Duration.between(sunrise, sunset).toSeconds())));
            uv.add(JsonValueBuilder.value(day.get("uvIndex").asDouble(0.0)));
            rain.add(JsonValueBuilder.value(day.get("precipProbability").asDouble(0.0) * 100.0));
            wind.add(JsonValueBuilder.value(day.get("windSpeed").asDouble(0.0)));
            windDirection.add(JsonValueBuilder.value(day.get("windBearing").asDouble(0.0)));
        }
        Map<String, OpenMeteoJsonValue> daily = new LinkedHashMap<>();
        daily.put("time", JsonValueBuilder.array(dates));
        daily.put("weather_code", JsonValueBuilder.array(weatherCodes));
        daily.put("temperature_2m_max", JsonValueBuilder.array(maxTemperatures));
        daily.put("temperature_2m_min", JsonValueBuilder.array(minTemperatures));
        daily.put("sunrise", JsonValueBuilder.array(sunrises));
        daily.put("sunset", JsonValueBuilder.array(sunsets));
        daily.put("daylight_duration", JsonValueBuilder.array(daylightDurations));
        daily.put("uv_index_max", JsonValueBuilder.array(uv));
        daily.put("precipitation_probability_max", JsonValueBuilder.array(rain));
        daily.put("wind_speed_10m_max", JsonValueBuilder.array(wind));
        daily.put("wind_direction_10m_dominant", JsonValueBuilder.array(windDirection));
        return JsonValueBuilder.object(daily);
    }

    private int weatherCode(String icon, String summary) {
        String text = ((icon == null ? "" : icon) + " " + (summary == null ? "" : summary)).toLowerCase(Locale.ROOT);
        if (text.contains("thunder")) {
            return 95;
        }
        if (text.contains("snow") || text.contains("sleet") || text.contains("ice")) {
            return 71;
        }
        if (text.contains("rain") || text.contains("drizzle") || text.contains("shower")) {
            return 61;
        }
        if (text.contains("fog") || text.contains("haze") || text.contains("mist")) {
            return 45;
        }
        if (text.contains("partly") || text.contains("fair")) {
            return 2;
        }
        if (text.contains("cloud") || text.contains("overcast")) {
            return 3;
        }
        if (text.contains("clear")) {
            return 0;
        }
        return 3;
    }

    private LocalDateTime fromUnix(double epochSeconds, ZoneId zoneId, LocalDateTime fallback) {
        if (Double.isNaN(epochSeconds) || epochSeconds <= 0.0) {
            return fallback;
        }
        return Instant.ofEpochSecond((long) epochSeconds).atZone(zoneId).toLocalDateTime();
    }

    private boolean isDay(LocalTime time) {
        return !time.isBefore(LocalTime.of(7, 0)) && !time.isAfter(LocalTime.of(21, 0));
    }
}
