package fr.alescis.aelia.provider.openmeteo;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Converts Historical Weather API payloads into the subset of Forecast API shape used by the dashboard mapper.
 */
public final class OpenMeteoArchiveFallbackMapper {
    public OpenMeteoJsonValue toForecastLikePayload(OpenMeteoLocation location, OpenMeteoJsonValue archivePayload) {
        OpenMeteoJsonValue hourly = archivePayload.get("hourly");
        OpenMeteoJsonValue daily = archivePayload.get("daily");
        int currentIndex = nearestTimeIndex(hourly.get("time"), ZonedDateTime.now(location.zoneId()).toLocalDateTime());

        Map<String, OpenMeteoJsonValue> root = new LinkedHashMap<>();
        root.put("current", currentFromHourly(hourly, currentIndex));
        root.put("hourly", hourly);
        root.put("daily", forecastLikeDaily(daily));
        return new OpenMeteoJsonValue(root);
    }

    private OpenMeteoJsonValue currentFromHourly(OpenMeteoJsonValue hourly, int index) {
        Map<String, OpenMeteoJsonValue> current = new LinkedHashMap<>();
        copyHourlyValue(hourly, current, "time", index);
        copyHourlyValue(hourly, current, "temperature_2m", index);
        copyHourlyValue(hourly, current, "relative_humidity_2m", index);
        copyHourlyValue(hourly, current, "apparent_temperature", index);
        copyHourlyValue(hourly, current, "precipitation", index);
        copyHourlyValue(hourly, current, "weather_code", index);
        copyHourlyValue(hourly, current, "pressure_msl", index);
        copyHourlyValue(hourly, current, "wind_speed_10m", index);
        copyHourlyValue(hourly, current, "wind_direction_10m", index);
        copyHourlyValue(hourly, current, "wind_gusts_10m", index);
        copyHourlyValue(hourly, current, "is_day", index);
        return new OpenMeteoJsonValue(current);
    }

    private OpenMeteoJsonValue forecastLikeDaily(OpenMeteoJsonValue daily) {
        Map<String, OpenMeteoJsonValue> mapped = new LinkedHashMap<>(daily.asObject());
        if (!mapped.containsKey("precipitation_probability_max")) {
            mapped.put("precipitation_probability_max", zeroArray(daily.get("time").size()));
        }
        if (!mapped.containsKey("uv_index_max")) {
            mapped.put("uv_index_max", zeroArray(daily.get("time").size()));
        }
        return new OpenMeteoJsonValue(mapped);
    }

    private OpenMeteoJsonValue zeroArray(int size) {
        List<OpenMeteoJsonValue> values = new ArrayList<>();
        for (int index = 0; index < Math.max(1, size); index++) {
            values.add(new OpenMeteoJsonValue(0.0));
        }
        return new OpenMeteoJsonValue(values);
    }

    private void copyHourlyValue(OpenMeteoJsonValue hourly, Map<String, OpenMeteoJsonValue> target, String variable, int index) {
        OpenMeteoJsonValue values = hourly.get(variable);
        target.put(variable, values.get(index));
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

    private LocalDateTime parseDateTime(String value, LocalDateTime fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return LocalDateTime.parse(value);
    }
}
