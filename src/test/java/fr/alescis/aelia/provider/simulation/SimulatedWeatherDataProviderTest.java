package fr.alescis.aelia.provider.simulation;

import fr.alescis.aelia.model.MetricValue;
import fr.alescis.aelia.ports.WeatherUpdateListener;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SimulatedWeatherDataProviderTest {

    @Test
    void currentNumericValuesStayInsideDeclaredBounds() {
        try (SimulatedWeatherDataProvider provider = new SimulatedWeatherDataProvider()) {
            MetricValue value = provider.currentValue("air_temperature");

            assertTrue(value.numericValue().isPresent());
            assertTrue(value.metric().minimum().isPresent());
            assertTrue(value.metric().maximum().isPresent());
            assertTrue(value.numericValue().getAsDouble() >= value.metric().minimum().getAsDouble());
            assertTrue(value.numericValue().getAsDouble() <= value.metric().maximum().getAsDouble());
        }
    }

    @Test
    void subscriptionPublishesValuesAndCanBeCancelled() throws InterruptedException {
        try (SimulatedWeatherDataProvider provider = new SimulatedWeatherDataProvider()) {
            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<MetricValue> received = new AtomicReference<>();

            UUID subscriptionId = provider.subscribe("humidity", Duration.ofSeconds(1), new WeatherUpdateListener() {
                @Override
                public void onUpdate(UUID subscriptionId, MetricValue value) {
                    received.set(value);
                    latch.countDown();
                }

                @Override
                public void onError(UUID subscriptionId, Throwable error) {
                    latch.countDown();
                }
            });

            assertTrue(latch.await(2, TimeUnit.SECONDS));
            assertTrue(received.get().numericValue().isPresent());

            provider.unsubscribe(subscriptionId);
            assertFalse(provider.subscriptions().stream().anyMatch(snapshot -> snapshot.id().equals(subscriptionId)));
        }
    }
}
