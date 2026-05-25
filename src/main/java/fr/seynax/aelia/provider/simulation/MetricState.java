package fr.seynax.aelia.provider.simulation;

import fr.seynax.aelia.model.MetricValue;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Random;

/**
 * Smooth bounded state used for one numeric metric.
 */
final class MetricState {

    private final MetricProfile profile;
    private final Random random;
    private double currentValue;
    private double targetValue;
    private double velocity;
    private Instant lastSample;

    MetricState(MetricProfile profile, Random random, Instant initialInstant) {
        this.profile = Objects.requireNonNull(profile, "profile");
        this.random = Objects.requireNonNull(random, "random");
        this.lastSample = Objects.requireNonNull(initialInstant, "initialInstant");
        this.currentValue = randomBetween(profile.initialMinimum(), profile.initialMaximum());
        this.targetValue = randomBetween(profile.targetMinimum(), profile.targetMaximum());
        this.velocity = 0.0;
    }

    synchronized MetricValue sample(Instant now) {
        Objects.requireNonNull(now, "now");
        advance(now);
        return MetricValue.numeric(profile.metric(), now, currentValue, 0.94);
    }

    private void advance(Instant now) {
        double seconds = Math.max(0.0, Duration.between(lastSample, now).toMillis() / 1_000.0);
        if (seconds <= 0.0) {
            return;
        }

        if (random.nextDouble() < Math.min(0.35, seconds / 900.0)) {
            targetValue = randomBetween(profile.targetMinimum(), profile.targetMaximum());
        }

        double range = profile.metric().maximum() - profile.metric().minimum();
        double noise = random.nextGaussian() * range * profile.volatility() * Math.sqrt(seconds / 60.0);
        double attraction = (targetValue - currentValue) * profile.attraction() * seconds;
        velocity = (velocity * 0.82) + noise + attraction;

        double maximumDelta = profile.maximumStepPerSecond() * seconds * Math.max(1.0, range / 10.0);
        double delta = clamp(velocity, -maximumDelta, maximumDelta);
        currentValue = clamp(currentValue + delta, profile.metric().minimum(), profile.metric().maximum());
        lastSample = now;
    }

    private double randomBetween(double minimum, double maximum) {
        return minimum + random.nextDouble() * (maximum - minimum);
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
