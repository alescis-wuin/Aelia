package fr.seynax.aelia.provider.simulation;

import fr.seynax.aelia.model.MetricValue;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * Slowly evolving categorical weather condition state.
 */
final class WeatherConditionState {

    private static final List<String> CONDITIONS = List.of(
            "Clear sky",
            "Mostly sunny",
            "Partly cloudy",
            "Overcast",
            "Light rain",
            "Moderate rain",
            "Heavy rain",
            "Thunderstorm",
            "Fog"
    );

    private final Random random;
    private int index;
    private Instant nextChange;

    WeatherConditionState(Random random, Instant initialInstant) {
        this.random = Objects.requireNonNull(random, "random");
        this.index = 2;
        this.nextChange = initialInstant.plus(randomDuration());
    }

    synchronized MetricValue sample(Instant now) {
        Objects.requireNonNull(now, "now");
        while (!now.isBefore(nextChange)) {
            index = nextIndex();
            nextChange = nextChange.plus(randomDuration());
        }
        return MetricValue.text(SimulationCatalog.weatherConditionMetric(), now, CONDITIONS.get(index), 0.92);
    }

    private int nextIndex() {
        double roll = random.nextDouble();
        if (roll < 0.60) {
            return index;
        }
        int offset = roll < 0.85 ? (random.nextBoolean() ? 1 : -1) : (random.nextBoolean() ? 2 : -2);
        int next = index + offset;
        if (next < 0) {
            return 0;
        }
        if (next >= CONDITIONS.size()) {
            return CONDITIONS.size() - 1;
        }
        return next;
    }

    private Duration randomDuration() {
        return Duration.ofMinutes(8L + random.nextInt(28));
    }
}
