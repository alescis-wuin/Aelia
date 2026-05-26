package fr.alescis.aelia.provider.openmeteo;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Converts MET Norway Locationforecast JSON to the forecast-shaped payload consumed by the dashboard mapper.
 */
public final class MetNorwayForecastFallbackMapper {
    public OpenMeteoJsonValue toForecastLikePayload(OpenMeteoLocation location, OpenMeteoJsonValue payload) {
        Objects.requireNonNull(location, "location");
        List<MetPoint> points = parsePoints(location.zoneId(), payload);
        if (points.isEmpty()) {
            throw new OpenMeteoException("MET Norway Locationforecast returned no timeseries points.");
        }
        LocalDateTime now = LocalDateTime.now(location.zoneId());
        MetPoint current = nearestPoint(points, now);
        Map<String, OpenMeteoJsonValue> root = new LinkedHashMap<>();
        root.put("current", object(currentObject(current)));
        root.put("hourly", hourlyObject(points, now));
        root.put("daily", dailyObject(points, location.zoneId()));
        return object(root);
    }

    private List<MetPoint> parsePoints(ZoneId zoneId, OpenMeteoJsonValue payload) {
        OpenMeteoJsonValue timeseries = payload.get("properties").get("timeseries");
        List<MetPoint> points = new ArrayList<>();
        for (OpenMeteoJsonValue item : timeseries.asArray()) {
            String rawTime = item.get("time").asString(null);
            LocalDateTime localTime = parseMetTime(rawTime, zoneId);
            if (localTime == null) {
                continue;
            }
            OpenMeteoJsonValue instant = item.get("data").get("instant").get("details");
            String symbolCode = symbolCode(item.get("data"));
            double temperature = instant.get("air_temperature").asDouble(Double.NaN);
            if (Double.isNaN(temperature)) {
                continue;
            }
            double humidity = instant.get("relative_humidity").asDouble(0.0);
            double pressure = instant.get("air_pressure_at_sea_level").asDouble(1013.0);
            double windSpeedKmh = instant.get("wind_speed").asDouble(0.0) * 3.6;
            double windDirection = instant.get("wind_from_direction").asDouble(Double.NaN);
            double gustKmh = instant.get("wind_speed_of_gust").asDouble(windSpeedKmh / 3.6) * 3.6;
            double uv = instant.get("ultraviolet_index_clear_sky").asDouble(0.0);
            double precipitation = precipitationAmount(item.get("data"));
            int precipitationProbability = precipitationProbability(item.get("data"), precipitation);
            boolean day = dayFlag(symbolCode, localTime.toLocalTime());
            points.add(new MetPoint(
                    localTime,
                    temperature,
                    humidity,
                    pressure,
                    windSpeedKmh,
                    windDirection,
                    gustKmh,
                    weatherCode(symbolCode),
                    day,
                    precipitationProbability,
                    uv
            ));
        }
        points.sort(Comparator.comparing(MetPoint::time));
        return List.copyOf(points);
    }

    private Map<String, OpenMeteoJsonValue> currentObject(MetPoint point) {
        Map<String, OpenMeteoJsonValue> current = new LinkedHashMap<>();
        current.put("time", value(point.time().toString()));
        current.put("temperature_2m", value(point.temperature()));
        current.put("relative_humidity_2m", value(point.humidity()));
        current.put("apparent_temperature", value(point.temperature()));
        current.put("weather_code", value(point.weatherCode()));
        current.put("pressure_msl", value(point.pressure()));
        current.put("wind_speed_10m", value(point.windSpeedKmh()));
        current.put("wind_direction_10m", value(point.windDirection()));
        current.put("wind_gusts_10m", value(point.gustKmh()));
        current.put("is_day", value(point.day() ? 1 : 0));
        return current;
    }

    private OpenMeteoJsonValue hourlyObject(List<MetPoint> points, LocalDateTime now) {
        List<MetPoint> selected = points.stream()
                .filter(point -> !point.time().isBefore(now.minusHours(1)))
                .limit(96)
                .toList();
        List<MetPoint> usable = selected.isEmpty() ? points.stream().limit(96).toList() : selected;
        Map<String, OpenMeteoJsonValue> hourly = new LinkedHashMap<>();
        hourly.put("time", array(usable.stream().map(point -> value(point.time().toString())).toList()));
        hourly.put("temperature_2m", array(usable.stream().map(point -> value(point.temperature())).toList()));
        hourly.put("weather_code", array(usable.stream().map(point -> value(point.weatherCode())).toList()));
        hourly.put("precipitation_probability", array(usable.stream().map(point -> value(point.precipitationProbability())).toList()));
        hourly.put("pressure_msl", array(usable.stream().map(point -> value(point.pressure())).toList()));
        return object(hourly);
    }

    private OpenMeteoJsonValue dailyObject(List<MetPoint> points, ZoneId zoneId) {
        LocalDate today = LocalDate.now(zoneId);
        Map<LocalDate, List<MetPoint>> byDate = new LinkedHashMap<>();
        for (MetPoint point : points) {
            LocalDate date = point.time().toLocalDate();
            if (date.isBefore(today)) {
                continue;
            }
            byDate.computeIfAbsent(date, ignored -> new ArrayList<>()).add(point);
        }
        if (byDate.isEmpty()) {
            for (MetPoint point : points) {
                byDate.computeIfAbsent(point.time().toLocalDate(), ignored -> new ArrayList<>()).add(point);
            }
        }
        List<DailyAggregate> aggregates = byDate.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .limit(7)
                .map(entry -> aggregate(entry.getKey(), entry.getValue()))
                .toList();
        Map<String, OpenMeteoJsonValue> daily = new LinkedHashMap<>();
        daily.put("time", array(aggregates.stream().map(day -> value(day.date().toString())).toList()));
        daily.put("weather_code", array(aggregates.stream().map(day -> value(day.weatherCode())).toList()));
        daily.put("temperature_2m_max", array(aggregates.stream().map(day -> value(day.maximumTemperature())).toList()));
        daily.put("temperature_2m_min", array(aggregates.stream().map(day -> value(day.minimumTemperature())).toList()));
        daily.put("uv_index_max", array(aggregates.stream().map(day -> value(day.maximumUv())).toList()));
        daily.put("precipitation_probability_max", array(aggregates.stream().map(day -> value(day.precipitationProbability())).toList()));
        daily.put("wind_speed_10m_max", array(aggregates.stream().map(day -> value(day.maximumWindKmh())).toList()));
        daily.put("wind_direction_10m_dominant", array(aggregates.stream().map(day -> value(day.dominantWindDirection())).toList()));
        return object(daily);
    }

    private DailyAggregate aggregate(LocalDate date, List<MetPoint> points) {
        double max = points.stream().mapToDouble(MetPoint::temperature).max().orElse(0.0);
        double min = points.stream().mapToDouble(MetPoint::temperature).min().orElse(0.0);
        double wind = points.stream().mapToDouble(MetPoint::windSpeedKmh).max().orElse(0.0);
        int rain = points.stream().mapToInt(MetPoint::precipitationProbability).max().orElse(0);
        int code = representativeCode(points);
        int uv = (int) Math.round(points.stream().mapToDouble(MetPoint::uvIndex).max().orElse(0.0));
        double direction = dominantDirection(points);
        return new DailyAggregate(date, max, min, rain, wind, direction, code, uv);
    }

    private int representativeCode(List<MetPoint> points) {
        return points.stream()
                .min(Comparator.comparingLong(point -> Math.abs(Duration.between(point.time().toLocalTime(), LocalTime.NOON).toMinutes())))
                .map(MetPoint::weatherCode)
                .orElse(points.get(0).weatherCode());
    }

    private double dominantDirection(List<MetPoint> points) {
        double x = 0.0;
        double y = 0.0;
        for (MetPoint point : points) {
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

    private MetPoint nearestPoint(List<MetPoint> points, LocalDateTime target) {
        return points.stream()
                .min(Comparator.comparingLong(point -> Math.abs(Duration.between(point.time(), target).toMinutes())))
                .orElseThrow(() -> new OpenMeteoException("MET Norway Locationforecast returned no usable points."));
    }

    private String symbolCode(OpenMeteoJsonValue data) {
        String oneHour = data.get("next_1_hours").get("summary").get("symbol_code").asString(null);
        if (oneHour != null && !oneHour.isBlank()) {
            return oneHour;
        }
        String sixHours = data.get("next_6_hours").get("summary").get("symbol_code").asString(null);
        if (sixHours != null && !sixHours.isBlank()) {
            return sixHours;
        }
        return data.get("next_12_hours").get("summary").get("symbol_code").asString("cloudy");
    }

    private double precipitationAmount(OpenMeteoJsonValue data) {
        double oneHour = data.get("next_1_hours").get("details").get("precipitation_amount").asDouble(Double.NaN);
        if (!Double.isNaN(oneHour)) {
            return oneHour;
        }
        double sixHours = data.get("next_6_hours").get("details").get("precipitation_amount").asDouble(Double.NaN);
        if (!Double.isNaN(sixHours)) {
            return sixHours / 6.0;
        }
        return 0.0;
    }

    private int precipitationProbability(OpenMeteoJsonValue data, double precipitationAmount) {
        double explicitProbability = data.get("next_1_hours").get("details").get("probability_of_precipitation").asDouble(Double.NaN);
        if (Double.isNaN(explicitProbability)) {
            explicitProbability = data.get("next_6_hours").get("details").get("probability_of_precipitation").asDouble(Double.NaN);
        }
        if (!Double.isNaN(explicitProbability)) {
            return clamp((int) Math.round(explicitProbability), 0, 100);
        }
        if (precipitationAmount <= 0.0) {
            return 0;
        }
        return clamp((int) Math.round(35.0 + precipitationAmount * 20.0), 0, 100);
    }

    private boolean dayFlag(String symbolCode, LocalTime time) {
        String normalized = symbolCode == null ? "" : symbolCode.toLowerCase(java.util.Locale.ROOT);
        if (normalized.endsWith("_day")) {
            return true;
        }
        if (normalized.endsWith("_night")) {
            return false;
        }
        return !time.isBefore(LocalTime.of(7, 0)) && !time.isAfter(LocalTime.of(21, 0));
    }

    private int weatherCode(String symbolCode) {
        String normalized = symbolCode == null ? "" : symbolCode.toLowerCase(java.util.Locale.ROOT)
                .replace("_day", "")
                .replace("_night", "")
                .replace("_polartwilight", "");
        if (normalized.contains("thunder")) {
            return 95;
        }
        if (normalized.contains("snow")) {
            return normalized.contains("heavy") ? 75 : 71;
        }
        if (normalized.contains("sleet")) {
            return 67;
        }
        if (normalized.contains("rainshowers") || normalized.contains("showers")) {
            return 80;
        }
        if (normalized.contains("rain")) {
            return normalized.contains("heavy") ? 65 : 61;
        }
        if (normalized.contains("fog")) {
            return 45;
        }
        if (normalized.contains("partlycloudy") || normalized.contains("fair")) {
            return 2;
        }
        if (normalized.contains("cloudy")) {
            return 3;
        }
        if (normalized.contains("clearsky")) {
            return 0;
        }
        return 3;
    }

    private LocalDateTime parseMetTime(String rawTime, ZoneId zoneId) {
        if (rawTime == null || rawTime.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(rawTime).atZone(zoneId).toLocalDateTime();
        } catch (DateTimeParseException exception) {
            try {
                return ZonedDateTime.parse(rawTime).withZoneSameInstant(zoneId).toLocalDateTime();
            } catch (DateTimeParseException ignored) {
                return null;
            }
        }
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
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

    private record MetPoint(
            LocalDateTime time,
            double temperature,
            double humidity,
            double pressure,
            double windSpeedKmh,
            double windDirection,
            double gustKmh,
            int weatherCode,
            boolean day,
            int precipitationProbability,
            double uvIndex
    ) {
    }

    private record DailyAggregate(
            LocalDate date,
            double maximumTemperature,
            double minimumTemperature,
            int precipitationProbability,
            double maximumWindKmh,
            double dominantWindDirection,
            int weatherCode,
            int maximumUv
    ) {
    }
}
