package fr.alescis.aelia.provider.simulation;

import fr.alescis.aelia.model.DataMetric;
import fr.alescis.aelia.model.MetricValue;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Objects;

/**
 * Deterministic local sun-cycle approximation for the simulated provider.
 */
final class SunCycleState {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final Map<String, DataMetric> metricsById;
    private final ZoneId zoneId;

    SunCycleState(Map<String, DataMetric> metricsById, ZoneId zoneId) {
        this.metricsById = Map.copyOf(metricsById);
        this.zoneId = Objects.requireNonNull(zoneId, "zoneId");
    }

    MetricValue sample(String metricId, Instant now) {
        DataMetric metric = Objects.requireNonNull(metricsById.get(metricId), "metric");
        LocalDate date = LocalDate.ofInstant(now, zoneId);
        int day = date.getDayOfYear();
        double seasonal = Math.sin(((day - 80.0d) / 365.0d) * Math.PI * 2.0d);
        int sunrise = (int) Math.round(390.0d - seasonal * 95.0d);
        int sunset = (int) Math.round(1110.0d + seasonal * 115.0d);
        int minuteOfDay = "sunrise".equals(metricId) ? sunrise : sunset;
        LocalTime time = LocalTime.of(minuteOfDay / 60, minuteOfDay % 60);
        return MetricValue.text(metric, now, FORMATTER.format(time));
    }
}
