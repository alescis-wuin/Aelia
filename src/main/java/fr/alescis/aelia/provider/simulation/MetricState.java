package fr.alescis.aelia.provider.simulation;

import fr.alescis.aelia.model.MetricValue;

import java.time.Duration;
import java.time.Instant;
import java.util.Random;

/**
 * Thread-safe random-walk state for a numeric metric.
 */
final class MetricState {
    private final MetricProfile profile;
    private final Random random;
    private Instant lastSample;
    private double value;

    MetricState(MetricProfile profile, long seed, Instant initialSample) {
        this.profile = profile;
        this.random = new Random(seed);
        this.lastSample = initialSample;
        this.value = profile.baseValue();
    }

    synchronized MetricValue sample(Instant now) {
        double elapsedSeconds = elapsedSeconds(now);
        if (elapsedSeconds > 0.0d) {
            double target = targetValue(now);
            double pull = (target - value) * Math.min(1.0d, profile.recovery() * elapsedSeconds);
            double noise = random.nextGaussian() * profile.volatility() * Math.sqrt(elapsedSeconds);
            value = clamp(value + pull + noise, profile.minimum(), profile.maximum());
            lastSample = now;
        }
        return MetricValue.numeric(profile.metric(), now, value);
    }

    private double elapsedSeconds(Instant now) {
        if (now.isBefore(lastSample)) {
            return 0.0d;
        }
        return Duration.between(lastSample, now).toMillis() / 1000.0d;
    }

    private double targetValue(Instant now) {
        double period = profile.cycleDuration().toSeconds();
        double phase = (now.getEpochSecond() % (long) period) / period;
        return profile.baseValue() + Math.sin(phase * Math.PI * 2.0d) * profile.cycleAmplitude();
    }

    private double clamp(double candidate, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, candidate));
    }
}
