package fr.alescis.aelia.provider.openmeteo;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Converts OpenWeather current and 5-day forecast payloads to the forecast-shaped payload consumed by Aelia.
 */
public final class OpenWeatherForecastFallbackMapper {
    public OpenMeteoJsonValue toForecastLikePayload(OpenMeteoLocation location, OpenMeteoJsonValue payload) {
        Objects.requireNonNull(location, "location");
        OpenMeteoJsonValue current = payload.get("currentWeather");
        OpenMeteoJsonValue list = payload.get("forecast5").get("list");
        if (!current.isPresent() || !list.isArray() || list.size() == 0) {
            throw new OpenMeteoException("OpenWeather fallback payload does not contain currentWeather and forecast5.list sections.");
        }
        List<ForecastPoint> points = forecastPoints(location.zoneId(), list);
        if (points.isEmpty()) {
            throw new OpenMeteoException("OpenWeather 5-day forecast returned no usable forecast points.");
        }
        Map<String, OpenMeteoJsonValue> root = new LinkedHashMap<>();
        root.put("current", JsonValueBuilder.object(currentObject(location, current)));
        root.put("hourly", hourlyObject(points));
        root.put("daily", dailyObject(location, current, points));
        return JsonValueBuilder.object(root);
    }

    private Map<String, OpenMeteoJsonValue> currentObject(OpenMeteoLocation location, OpenMeteoJsonValue current) {
        ZoneId zoneId = location.zoneId();
        LocalDateTime time = fromUnix(current.get("dt").asDouble(Double.NaN), zoneId, LocalDateTime.now(zoneId));
        OpenMeteoJsonValue weather = current.get("weather").get(0);
        boolean day = isDayIcon(weather.get("icon").asString("01d"), time.toLocalTime());
        Map<String, OpenMeteoJsonValue> mapped = new LinkedHashMap<>();
        mapped.put("time", JsonValueBuilder.value(time.toString()));
        mapped.put("temperature_2m", JsonValueBuilder.value(current.get("main").get("temp").asDouble(location.fallbackTemperatureCelsius())));
        mapped.put("relative_humidity_2m", JsonValueBuilder.value(current.get("main").get("humidity").asDouble(0.0)));
        mapped.put("apparent_temperature", JsonValueBuilder.value(current.get("main").get("feels_like").asDouble(current.get("main").get("temp").asDouble(location.fallbackTemperatureCelsius()))));
        mapped.put("weather_code", JsonValueBuilder.value(weatherCode(weather.get("id").asRoundedInt(800))));
        mapped.put("pressure_msl", JsonValueBuilder.value(current.get("main").get("pressure").asDouble(1013.0)));
        mapped.put("wind_speed_10m", JsonValueBuilder.value(msToKmh(current.get("wind").get("speed").asDouble(0.0))));
        mapped.put("wind_direction_10m", JsonValueBuilder.value(current.get("wind").get("deg").asDouble(Double.NaN)));
        mapped.put("wind_gusts_10m", JsonValueBuilder.value(msToKmh(current.get("wind").get("gust").asDouble(current.get("wind").get("speed").asDouble(0.0)))));
        mapped.put("is_day", JsonValueBuilder.value(day ? 1 : 0));
        return mapped;
    }

    private OpenMeteoJsonValue hourlyObject(List<ForecastPoint> points) {
        List<ForecastPoint> usable = points.stream().limit(56).toList();
        Map<String, OpenMeteoJsonValue> hourly = new LinkedHashMap<>();
        hourly.put("time", JsonValueBuilder.array(usable.stream().map(point -> JsonValueBuilder.value(point.time().toString())).toList()));
        hourly.put("temperature_2m", JsonValueBuilder.array(usable.stream().map(point -> JsonValueBuilder.value(point.temperature())).toList()));
        hourly.put("weather_code", JsonValueBuilder.array(usable.stream().map(point -> JsonValueBuilder.value(point.weatherCode())).toList()));
        hourly.put("precipitation_probability", JsonValueBuilder.array(usable.stream().map(point -> JsonValueBuilder.value(point.precipitationProbability())).toList()));
        hourly.put("pressure_msl", JsonValueBuilder.array(usable.stream().map(point -> JsonValueBuilder.value(point.pressure())).toList()));
        return JsonValueBuilder.object(hourly);
    }

    private OpenMeteoJsonValue dailyObject(OpenMeteoLocation location, OpenMeteoJsonValue current, List<ForecastPoint> points) {
        LocalDate today = LocalDate.now(location.zoneId());
        Map<LocalDate, List<ForecastPoint>> byDate = new LinkedHashMap<>();
        for (ForecastPoint point : points) {
            if (!point.time().toLocalDate().isBefore(today)) {
                byDate.computeIfAbsent(point.time().toLocalDate(), ignored -> new ArrayList<>()).add(point);
            }
        }
        List<DailyAggregate> aggregates = byDate.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .limit(7)
                .map(entry -> aggregate(entry.getKey(), entry.getValue()))
                .toList();
        if (aggregates.isEmpty()) {
            throw new OpenMeteoException("OpenWeather fallback returned no daily forecast aggregates.");
        }
        Map<String, OpenMeteoJsonValue> daily = new LinkedHashMap<>();
        daily.put("time", JsonValueBuilder.array(aggregates.stream().map(day -> JsonValueBuilder.value(day.date().toString())).toList()));
        daily.put("weather_code", JsonValueBuilder.array(aggregates.stream().map(day -> JsonValueBuilder.value(day.weatherCode())).toList()));
        daily.put("temperature_2m_max", JsonValueBuilder.array(aggregates.stream().map(day -> JsonValueBuilder.value(day.maximumTemperature())).toList()));
        daily.put("temperature_2m_min", JsonValueBuilder.array(aggregates.stream().map(day -> JsonValueBuilder.value(day.minimumTemperature())).toList()));
        daily.put("sunrise", JsonValueBuilder.array(aggregates.stream().map(day -> JsonValueBuilder.value(sunriseFor(current, day.date(), location.zoneId()).toString())).toList()));
        daily.put("sunset", JsonValueBuilder.array(aggregates.stream().map(day -> JsonValueBuilder.value(sunsetFor(current, day.date(), location.zoneId()).toString())).toList()));
        daily.put("daylight_duration", JsonValueBuilder.array(aggregates.stream().map(day -> JsonValueBuilder.value(daylightSeconds(current, location.zoneId()))).toList()));
        daily.put("uv_index_max", JsonValueBuilder.array(aggregates.stream().map(day -> JsonValueBuilder.value(0)).toList()));
        daily.put("precipitation_probability_max", JsonValueBuilder.array(aggregates.stream().map(day -> JsonValueBuilder.value(day.precipitationProbability())).toList()));
        daily.put("wind_speed_10m_max", JsonValueBuilder.array(aggregates.stream().map(day -> JsonValueBuilder.value(day.maximumWindKmh())).toList()));
        daily.put("wind_direction_10m_dominant", JsonValueBuilder.array(aggregates.stream().map(day -> JsonValueBuilder.value(day.dominantWindDirection())).toList()));
        return JsonValueBuilder.object(daily);
    }

    private List<ForecastPoint> forecastPoints(ZoneId zoneId, OpenMeteoJsonValue list) {
        List<ForecastPoint> points = new ArrayList<>();
        for (OpenMeteoJsonValue item : list.asArray()) {
            LocalDateTime time = fromUnix(item.get("dt").asDouble(Double.NaN), zoneId, parseDtText(item.get("dt_txt").asString(null), zoneId));
            OpenMeteoJsonValue weather = item.get("weather").get(0);
            int code = weatherCode(weather.get("id").asRoundedInt(800));
            double speed = msToKmh(item.get("wind").get("speed").asDouble(0.0));
            double direction = item.get("wind").get("deg").asDouble(Double.NaN);
            int probability = clamp((int) Math.round(item.get("pop").asDouble(0.0) * 100.0), 0, 100);
            points.add(new ForecastPoint(
                    time,
                    item.get("main").get("temp").asDouble(0.0),
                    item.get("main").get("pressure").asDouble(1013.0),
                    speed,
                    direction,
                    code,
                    probability
            ));
        }
        points.sort(Comparator.comparing(ForecastPoint::time));
        return points;
    }

    private DailyAggregate aggregate(LocalDate date, List<ForecastPoint> points) {
        double maximumTemperature = points.stream().mapToDouble(ForecastPoint::temperature).max().orElse(0.0);
        double minimumTemperature = points.stream().mapToDouble(ForecastPoint::temperature).min().orElse(0.0);
        double maximumWind = points.stream().mapToDouble(ForecastPoint::windSpeedKmh).max().orElse(0.0);
        int precipitationProbability = points.stream().mapToInt(ForecastPoint::precipitationProbability).max().orElse(0);
        int code = points.stream()
                .min(Comparator.comparingLong(point -> Math.abs(Duration.between(point.time().toLocalTime(), LocalTime.NOON).toMinutes())))
                .map(ForecastPoint::weatherCode)
                .orElse(3);
        return new DailyAggregate(date, maximumTemperature, minimumTemperature, precipitationProbability, maximumWind, dominantDirection(points), code);
    }

    private LocalDateTime sunriseFor(OpenMeteoJsonValue current, LocalDate date, ZoneId zoneId) {
        LocalTime fallback = LocalTime.of(6, 0);
        LocalDateTime sameDay = fromUnix(current.get("sys").get("sunrise").asDouble(Double.NaN), zoneId, LocalDateTime.of(date, fallback));
        return LocalDateTime.of(date, sameDay.toLocalTime());
    }

    private LocalDateTime sunsetFor(OpenMeteoJsonValue current, LocalDate date, ZoneId zoneId) {
        LocalTime fallback = LocalTime.of(21, 0);
        LocalDateTime sameDay = fromUnix(current.get("sys").get("sunset").asDouble(Double.NaN), zoneId, LocalDateTime.of(date, fallback));
        return LocalDateTime.of(date, sameDay.toLocalTime());
    }

    private long daylightSeconds(OpenMeteoJsonValue current, ZoneId zoneId) {
        LocalDate today = LocalDate.now(zoneId);
        LocalDateTime sunrise = sunriseFor(current, today, zoneId);
        LocalDateTime sunset = sunsetFor(current, today, zoneId);
        return Math.max(0L, Duration.between(sunrise, sunset).toSeconds());
    }

    private double dominantDirection(List<ForecastPoint> points) {
        double x = 0.0;
        double y = 0.0;
        for (ForecastPoint point : points) {
            if (Double.isNaN(point.windDirection())) {
                continue;
            }
            double radians = Math.toRadians(point.windDirection());
            x += Math.sin(radians);
            y += Math.cos(radians);
        }
        if (x == 0.0 && y == 0.0) {
            return 0.0;
        }
        double degrees = Math.toDegrees(Math.atan2(x, y));
        return degrees < 0.0 ? degrees + 360.0 : degrees;
    }

    private LocalDateTime fromUnix(double epochSeconds, ZoneId zoneId, LocalDateTime fallback) {
        if (Double.isNaN(epochSeconds) || epochSeconds <= 0.0) {
            return fallback;
        }
        return Instant.ofEpochSecond((long) epochSeconds).atZone(zoneId).toLocalDateTime();
    }

    private LocalDateTime parseDtText(String value, ZoneId zoneId) {
        if (value == null || value.isBlank()) {
            return LocalDateTime.now(zoneId);
        }
        return LocalDateTime.parse(value.replace(' ', 'T'));
    }

    private boolean isDayIcon(String icon, LocalTime time) {
        if (icon != null && icon.endsWith("n")) {
            return false;
        }
        if (icon != null && icon.endsWith("d")) {
            return true;
        }
        return !time.isBefore(LocalTime.of(7, 0)) && !time.isAfter(LocalTime.of(21, 0));
    }

    private int weatherCode(int openWeatherId) {
        if (openWeatherId >= 200 && openWeatherId < 300) {
            return 95;
        }
        if (openWeatherId >= 300 && openWeatherId < 400) {
            return 51;
        }
        if (openWeatherId >= 500 && openWeatherId < 600) {
            return openWeatherId == 511 ? 67 : openWeatherId >= 520 ? 80 : 61;
        }
        if (openWeatherId >= 600 && openWeatherId < 700) {
            return 71;
        }
        if (openWeatherId >= 700 && openWeatherId < 800) {
            return 45;
        }
        if (openWeatherId == 800) {
            return 0;
        }
        if (openWeatherId == 801 || openWeatherId == 802) {
            return 2;
        }
        return 3;
    }

    private double msToKmh(double value) {
        return value * 3.6;
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private record ForecastPoint(
            LocalDateTime time,
            double temperature,
            double pressure,
            double windSpeedKmh,
            double windDirection,
            int weatherCode,
            int precipitationProbability
    ) {
    }

    private record DailyAggregate(
            LocalDate date,
            double maximumTemperature,
            double minimumTemperature,
            int precipitationProbability,
            double maximumWindKmh,
            double dominantWindDirection,
            int weatherCode
    ) {
    }
}
