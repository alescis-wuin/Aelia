package fr.alescis.aelia.provider.openmeteo;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Converts Visual Crossing Timeline Weather API JSON to Open-Meteo-shaped dashboard data.
 */
public final class VisualCrossingForecastFallbackMapper {
    public OpenMeteoJsonValue toForecastLikePayload(OpenMeteoLocation location, OpenMeteoJsonValue payload) {
        Objects.requireNonNull(location, "location");
        OpenMeteoJsonValue days = payload.get("days");
        OpenMeteoJsonValue current = payload.get("currentConditions");
        if (!days.isArray() || days.size() == 0 || !current.isPresent()) {
            throw new OpenMeteoException("Visual Crossing payload does not contain currentConditions and days sections.");
        }
        Map<String, OpenMeteoJsonValue> root = new LinkedHashMap<>();
        root.put("current", JsonValueBuilder.object(currentObject(location, current, days.get(0))));
        root.put("hourly", hourlyObject(location.zoneId(), days));
        root.put("daily", dailyObject(location.zoneId(), days));
        return JsonValueBuilder.object(root);
    }

    private Map<String, OpenMeteoJsonValue> currentObject(OpenMeteoLocation location, OpenMeteoJsonValue current, OpenMeteoJsonValue today) {
        ZoneId zoneId = location.zoneId();
        LocalDateTime time = fromEpoch(current.get("datetimeEpoch").asDouble(Double.NaN), zoneId, LocalDateTime.now(zoneId));
        String icon = current.get("icon").asString(today.get("icon").asString("cloudy"));
        boolean day = !time.toLocalTime().isBefore(parseTime(today.get("sunrise").asString(null), LocalTime.of(6, 0)))
                && !time.toLocalTime().isAfter(parseTime(today.get("sunset").asString(null), LocalTime.of(21, 0)));
        Map<String, OpenMeteoJsonValue> mapped = new LinkedHashMap<>();
        mapped.put("time", JsonValueBuilder.value(time.toString()));
        mapped.put("temperature_2m", JsonValueBuilder.value(current.get("temp").asDouble(location.fallbackTemperatureCelsius())));
        mapped.put("relative_humidity_2m", JsonValueBuilder.value(current.get("humidity").asDouble(0.0)));
        mapped.put("apparent_temperature", JsonValueBuilder.value(current.get("feelslike").asDouble(current.get("temp").asDouble(location.fallbackTemperatureCelsius()))));
        mapped.put("weather_code", JsonValueBuilder.value(weatherCode(icon, current.get("conditions").asString(""))));
        mapped.put("pressure_msl", JsonValueBuilder.value(current.get("pressure").asDouble(1013.0)));
        mapped.put("wind_speed_10m", JsonValueBuilder.value(current.get("windspeed").asDouble(0.0)));
        mapped.put("wind_direction_10m", JsonValueBuilder.value(current.get("winddir").asDouble(Double.NaN)));
        mapped.put("wind_gusts_10m", JsonValueBuilder.value(current.get("windgust").asDouble(current.get("windspeed").asDouble(0.0))));
        mapped.put("is_day", JsonValueBuilder.value(day ? 1 : 0));
        return mapped;
    }

    private OpenMeteoJsonValue hourlyObject(ZoneId zoneId, OpenMeteoJsonValue days) {
        List<HourPoint> points = new ArrayList<>();
        for (OpenMeteoJsonValue day : days.asArray()) {
            LocalDate date = parseDate(day.get("datetime").asString(null), LocalDate.now(zoneId));
            for (OpenMeteoJsonValue hour : day.get("hours").asArray()) {
                LocalDateTime time = parseHour(date, hour.get("datetime").asString(null));
                points.add(new HourPoint(
                        time,
                        hour.get("temp").asDouble(0.0),
                        weatherCode(hour.get("icon").asString("cloudy"), hour.get("conditions").asString("")),
                        clamp((int) Math.round(hour.get("precipprob").asDouble(0.0)), 0, 100),
                        hour.get("pressure").asDouble(1013.0)
                ));
            }
        }
        points.sort(Comparator.comparing(HourPoint::time));
        LocalDateTime now = LocalDateTime.now(zoneId).minusHours(1);
        List<HourPoint> usable = points.stream().filter(point -> !point.time().isBefore(now)).limit(96).toList();
        if (usable.isEmpty()) {
            usable = points.stream().limit(96).toList();
        }
        Map<String, OpenMeteoJsonValue> hourly = new LinkedHashMap<>();
        hourly.put("time", JsonValueBuilder.array(usable.stream().map(point -> JsonValueBuilder.value(point.time().toString())).toList()));
        hourly.put("temperature_2m", JsonValueBuilder.array(usable.stream().map(point -> JsonValueBuilder.value(point.temperature())).toList()));
        hourly.put("weather_code", JsonValueBuilder.array(usable.stream().map(point -> JsonValueBuilder.value(point.weatherCode())).toList()));
        hourly.put("precipitation_probability", JsonValueBuilder.array(usable.stream().map(point -> JsonValueBuilder.value(point.precipitationProbability())).toList()));
        hourly.put("pressure_msl", JsonValueBuilder.array(usable.stream().map(point -> JsonValueBuilder.value(point.pressure())).toList()));
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
            LocalDate date = parseDate(day.get("datetime").asString(null), LocalDate.now(zoneId).plusDays(index));
            LocalTime sunrise = parseTime(day.get("sunrise").asString(null), LocalTime.of(6, 0));
            LocalTime sunset = parseTime(day.get("sunset").asString(null), LocalTime.of(21, 0));
            dates.add(JsonValueBuilder.value(date.toString()));
            weatherCodes.add(JsonValueBuilder.value(weatherCode(day.get("icon").asString("cloudy"), day.get("conditions").asString(""))));
            maxTemperatures.add(JsonValueBuilder.value(day.get("tempmax").asDouble(day.get("temp").asDouble(0.0))));
            minTemperatures.add(JsonValueBuilder.value(day.get("tempmin").asDouble(day.get("temp").asDouble(0.0))));
            sunrises.add(JsonValueBuilder.value(LocalDateTime.of(date, sunrise).toString()));
            sunsets.add(JsonValueBuilder.value(LocalDateTime.of(date, sunset).toString()));
            daylightDurations.add(JsonValueBuilder.value(Math.max(0L, Duration.between(sunrise, sunset).toSeconds())));
            uv.add(JsonValueBuilder.value(day.get("uvindex").asDouble(0.0)));
            rain.add(JsonValueBuilder.value(day.get("precipprob").asDouble(0.0)));
            wind.add(JsonValueBuilder.value(day.get("windspeed").asDouble(0.0)));
            windDirection.add(JsonValueBuilder.value(day.get("winddir").asDouble(0.0)));
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

    private int weatherCode(String icon, String conditions) {
        String text = ((icon == null ? "" : icon) + " " + (conditions == null ? "" : conditions)).toLowerCase(Locale.ROOT);
        if (text.contains("thunder") || text.contains("storm")) {
            return 95;
        }
        if (text.contains("snow") || text.contains("ice") || text.contains("sleet")) {
            return 71;
        }
        if (text.contains("rain") || text.contains("shower") || text.contains("drizzle")) {
            return 61;
        }
        if (text.contains("fog") || text.contains("mist") || text.contains("haze")) {
            return 45;
        }
        if (text.contains("partly") || text.contains("partially")) {
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

    private LocalDateTime fromEpoch(double epochSeconds, ZoneId zoneId, LocalDateTime fallback) {
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

    private LocalTime parseTime(String value, LocalTime fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return LocalTime.parse(value);
        } catch (DateTimeParseException exception) {
            return fallback;
        }
    }

    private LocalDateTime parseHour(LocalDate date, String value) {
        if (value == null || value.isBlank()) {
            return date.atStartOfDay();
        }
        return LocalDateTime.of(date, parseTime(value, LocalTime.MIDNIGHT));
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private record HourPoint(LocalDateTime time, double temperature, int weatherCode, int precipitationProbability, double pressure) {
    }
}
