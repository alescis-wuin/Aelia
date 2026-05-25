package fr.seynax.aelia.service;

import fr.seynax.aelia.provider.simulation.SimulatedWeatherDataProvider;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WeatherServiceTest {

    @Test
    void unsupportedMetricIsRejected() {
        try (WeatherService service = new WeatherService(new SimulatedWeatherDataProvider())) {
            assertThrows(IllegalArgumentException.class, () -> service.currentValue("missing_metric"));
        }
    }

    @Test
    void intervalValidationRejectsSubSecondStreams() {
        try (WeatherService service = new WeatherService(new SimulatedWeatherDataProvider())) {
            assertThrows(IllegalArgumentException.class, () -> service.subscribe(
                    "temperature_air",
                    Duration.ofMillis(250),
                    new fr.seynax.aelia.ports.WeatherUpdateListener() {
                        @Override
                        public void onUpdate(java.util.UUID subscriptionId, fr.seynax.aelia.model.MetricValue value) {
                            throw new UnsupportedOperationException("The listener must not be invoked by invalid subscriptions.");
                        }

                        @Override
                        public void onError(java.util.UUID subscriptionId, Throwable error) {
                            throw new UnsupportedOperationException("The listener must not be invoked by invalid subscriptions.");
                        }
                    }
            ));
        }
    }

    @Test
    void catalogAndLimitsAreExposed() {
        try (WeatherService service = new WeatherService(new SimulatedWeatherDataProvider())) {
            assertFalse(service.supportedMetrics().isEmpty());
            assertFalse(service.limits().isEmpty());
            assertTrue(service.findMetric("temperature_air").isPresent());
        }
    }
}
