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
import java.util.Map;
import java.util.Objects;

/**
 * Converts Weatherbit JSON to Open-Meteo-shaped dashboard data.
 */
public final class WeatherbitForecastFallbackMapper {
    public OpenMeteoJsonValue toForecastLikePayload(OpenMeteoLocation location, OpenMeteoJsonValue payload) {
        Objects.requireNonNull(location, "location");
        OpenMeteoJsonValue current = payload.get("current").get("data").get(0);
        OpenMeteoJsonValue daily = payload.get("daily").get("data");
        if (!current.isPresent() || !daily.isArray() || daily.size() == 0) {
            throw new OpenMeteoException("Weatherbit payload does not contain current.data[0] and daily.data sections.");
        }
        Map<String, OpenMeteoJsonValue> root = new LinkedHashMap<>();
        root.put("current", JsonValueBuilder.object(currentObject(location, current)));
        root.put("hourly", hourlyApproximation(location, current, daily));
        root.put("daily", dailyObject(location.zoneId(), daily));
        return JsonValueBuilder.object(root);
    }

    private Map<String, OpenMeteoJsonValue> currentObject(OpenMeteoLocation location, OpenMeteoJsonValue current) {
        ZoneId zoneId = location.zoneId();
        LocalDateTime time = fromUnix(current.get("ts").asDouble(Double.NaN), zoneId, LocalDateTime.now(zoneId));
        int code = weatherCode(current.get("weather").get("code").asRoundedInt(800));
        Map<String, OpenMeteoJsonValue> mapped = new LinkedHashMap<>();
        mapped.put("time", JsonValueBuilder.value(time.toString()));
        mapped.put("temperature_2m", JsonValueBuilder.value(current.get("temp").asDouble(location.fallbackTemperatureCelsius())));
        mapped.put("relative_humidity_2m", JsonValueBuilder.value(current.get("rh").asDouble(0.0)));
        mapped.put("apparent_temperature", JsonValueBuilder.value(current.get("app_temp").asDouble(current.get("temp").asDouble(location.fallbackTemperatureCelsius()))));
        mapped.put("weather_code", JsonValueBuilder.value(code));
        mapped.put("pressure_msl", JsonValueBuilder.value(current.get("slp").asDouble(current.get("pres").asDouble(1013.0))));
        mapped.put("wind_speed_10m", JsonValueBuilder.value(msToKmh(current.get("wind_spd").asDouble(0.0))));
        mapped.put("wind_direction_10m", JsonValueBuilder.value(current.get("wind_dir").asDouble(Double.NaN)));
        mapped.put("wind_gusts_10m", JsonValueBuilder.value(msToKmh(current.get("gust").asDouble(current.get("wind_spd").asDouble(0.0)))));
        mapped.put("is_day", JsonValueBuilder.value(isDay(time.toLocalTime()) ? 1 : 0));
        return mapped;
    }

    private OpenMeteoJsonValue hourlyApproximation(OpenMeteoLocation location, OpenMeteoJsonValue current, OpenMeteoJsonValue daily) {
        List<OpenMeteoJsonValue> times = new ArrayList<>();
        List<OpenMeteoJsonValue> temperatures = new ArrayList<>();
        List<OpenMeteoJsonValue> weatherCodes = new ArrayList<>();
        List<OpenMeteoJsonValue> precipitationProbabilities = new ArrayList<>();
        List<OpenMeteoJsonValue> pressures = new ArrayList<>();
        LocalDateTime start = LocalDateTime.now(location.zoneId()).withMinute(0).withSecond(0).withNano(0);
        for (int offset = 0; offset < 72; offset += 3) {
            LocalDateTime time = start.plusHours(offset);
            OpenMeteoJsonValue day = nearestDay(daily, time.toLocalDate(), location.zoneId());
            double minimum = day.get("min_temp").asDouble(current.get("temp").asDouble(location.fallbackTemperatureCelsius()));
            double maximum = day.get("max_temp").asDouble(current.get("temp").asDouble(location.fallbackTemperatureCelsius()));
            double temperature = estimatedTemperature(minimum, maximum, time.toLocalTime());
            times.add(JsonValueBuilder.value(time.toString()));
            temperatures.add(JsonValueBuilder.value(temperature));
            weatherCodes.add(JsonValueBuilder.value(weatherCode(day.get("weather").get("code").asRoundedInt(current.get("weather").get("code").asRoundedInt(800)))));
            precipitationProbabilities.add(JsonValueBuilder.value(day.get("pop").asDouble(0.0)));
            pressures.add(JsonValueBuilder.value(current.get("slp").asDouble(current.get("pres").asDouble(1013.0))));
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
            LocalDate date = parseDate(day.get("valid_date").asString(null), LocalDate.now(zoneId).plusDays(index));
            LocalDateTime sunrise = fromUnix(day.get("sunrise_ts").asDouble(Double.NaN), zoneId, date.atTime(6, 0));
            LocalDateTime sunset = fromUnix(day.get("sunset_ts").asDouble(Double.NaN), zoneId, date.atTime(21, 0));
            dates.add(JsonValueBuilder.value(date.toString()));
            weatherCodes.add(JsonValueBuilder.value(weatherCode(day.get("weather").get("code").asRoundedInt(800))));
            maxTemperatures.add(JsonValueBuilder.value(day.get("max_temp").asDouble(day.get("temp").asDouble(0.0))));
            minTemperatures.add(JsonValueBuilder.value(day.get("min_temp").asDouble(day.get("temp").asDouble(0.0))));
            sunrises.add(JsonValueBuilder.value(sunrise.toString()));
            sunsets.add(JsonValueBuilder.value(sunset.toString()));
            daylightDurations.add(JsonValueBuilder.value(Math.max(0L, Duration.between(sunrise, sunset).toSeconds())));
            uv.add(JsonValueBuilder.value(day.get("uv").asDouble(0.0)));
            rain.add(JsonValueBuilder.value(day.get("pop").asDouble(0.0)));
            wind.add(JsonValueBuilder.value(msToKmh(day.get("wind_spd").asDouble(0.0))));
            windDirection.add(JsonValueBuilder.value(day.get("wind_dir").asDouble(0.0)));
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

    private OpenMeteoJsonValue nearestDay(OpenMeteoJsonValue days, LocalDate date, ZoneId zoneId) {
        for (OpenMeteoJsonValue day : days.asArray()) {
            if (parseDate(day.get("valid_date").asString(null), LocalDate.now(zoneId)).equals(date)) {
                return day;
            }
        }
        return days.get(Math.max(0, Math.min(days.size() - 1, 0)));
    }

    private double estimatedTemperature(double minimum, double maximum, LocalTime time) {
        double radians = ((time.getHour() - 5.0) / 24.0) * Math.PI * 2.0;
        double normalized = (Math.sin(radians) + 1.0) / 2.0;
        return minimum + (maximum - minimum) * normalized;
    }

    private int weatherCode(int code) {
        if (code >= 200 && code < 300) {
            return 95;
        }
        if (code >= 300 && code < 700) {
            return code >= 600 ? 71 : 61;
        }
        if (code >= 700 && code < 800) {
            return 45;
        }
        if (code == 800) {
            return 0;
        }
        if (code == 801 || code == 802) {
            return 2;
        }
        return 3;
    }

    private LocalDateTime fromUnix(double epochSeconds, ZoneId zoneId, LocalDateTime fallback) {
        if (Double.isNaN(epochSeconds) || epochSeconds <= 0.0) {
            return fallback;
        }
        return Instant.ofEpochSecond((long) epochSeconds).atZone(zoneId).toLocalDateTime();
    }

    private LocalDate parseDate(String value, LocalDate fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return LocalDate.parse(value);
    }

    private boolean isDay(LocalTime time) {
        return !time.isBefore(LocalTime.of(7, 0)) && !time.isAfter(LocalTime.of(21, 0));
    }

    private double msToKmh(double value) {
        return value * 3.6;
    }
}
