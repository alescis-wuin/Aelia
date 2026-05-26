package fr.alescis.aelia.service;

import fr.alescis.aelia.model.MetricValue;
import fr.alescis.aelia.ports.WeatherUpdateListener;
import fr.alescis.aelia.provider.simulation.SimulatedWeatherDataProvider;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class WeatherServiceTest {

    @Test
    void intervalValidationRejectsSubSecondStreams() {
        try (WeatherService service = new WeatherService(new SimulatedWeatherDataProvider())) {
            assertThrows(IllegalArgumentException.class, () -> service.subscribe(
                    "air_temperature",
                    Duration.ofMillis(500),
                    new NoopWeatherUpdateListener()
            ));
        }
    }

    @Test
    void currentValueRejectsUnsupportedMetrics() {
        try (WeatherService service = new WeatherService(new SimulatedWeatherDataProvider())) {
            assertThrows(IllegalArgumentException.class, () -> service.currentValue("unknown_metric"));
        }
    }

    @Test
    void currentValueAcceptsSupportedMetrics() {
        try (WeatherService service = new WeatherService(new SimulatedWeatherDataProvider())) {
            assertDoesNotThrow(() -> service.currentValue("wind_speed"));
        }
    }

    private static final class NoopWeatherUpdateListener implements WeatherUpdateListener {
        @Override
        public void onUpdate(UUID subscriptionId, MetricValue value) {
        }

        @Override
        public void onError(UUID subscriptionId, Throwable error) {
        }
    }
}
