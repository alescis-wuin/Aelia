package fr.seynax.aelia.provider.simulation;

import fr.seynax.aelia.model.DataMetric;
import fr.seynax.aelia.model.MetricValue;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;

/**
 * Lightweight non-geospatial approximation for sunrise and sunset values.
 */
final class SunCycleState {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT);

    private final ZoneId zoneId;

    SunCycleState(ZoneId zoneId) {
        this.zoneId = Objects.requireNonNull(zoneId, "zoneId");
    }

    MetricValue sunrise(Instant now) {
        return timeValue(SimulationCatalog.sunriseMetric(), now, true);
    }

    MetricValue sunset(Instant now) {
        return timeValue(SimulationCatalog.sunsetMetric(), now, false);
    }

    private MetricValue timeValue(DataMetric metric, Instant now, boolean sunrise) {
        LocalDate date = LocalDate.ofInstant(now, zoneId);
        double seasonal = Math.sin(((date.getDayOfYear() - 80.0) / 365.25) * Math.PI * 2.0);
        int shiftMinutes = (int) Math.round(seasonal * 105.0);
        LocalTime base = sunrise ? LocalTime.of(7, 20) : LocalTime.of(18, 35);
        LocalTime value = sunrise ? base.minusMinutes(shiftMinutes) : base.plusMinutes(shiftMinutes);
        return MetricValue.text(metric, now, value.format(FORMATTER), 0.86);
    }
}
