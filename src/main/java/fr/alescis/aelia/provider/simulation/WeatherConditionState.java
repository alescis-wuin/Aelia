package fr.alescis.aelia.provider.simulation;

import fr.alescis.aelia.model.DataMetric;
import fr.alescis.aelia.model.MetricValue;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Random;

/**
 * Slowly changing text state for simulated weather conditions.
 */
final class WeatherConditionState {
    private static final List<String> CONDITIONS = List.of(
            "Clear",
            "Bright clouds",
            "Cloudy",
            "Light rain",
            "Windy",
            "Dense clouds"
    );

    private final DataMetric metric;
    private final Random random;
    private String currentCondition = "Bright clouds";
    private Instant nextChange;

    WeatherConditionState(DataMetric metric, long seed, Instant now) {
        this.metric = metric;
        this.random = new Random(seed);
        this.nextChange = now.plus(Duration.ofMinutes(8));
    }

    synchronized MetricValue sample(Instant now) {
        if (!now.isBefore(nextChange)) {
            currentCondition = CONDITIONS.get(random.nextInt(CONDITIONS.size()));
            nextChange = now.plus(Duration.ofMinutes(6 + random.nextInt(16)));
        }
        return MetricValue.text(metric, now, currentCondition);
    }
}
