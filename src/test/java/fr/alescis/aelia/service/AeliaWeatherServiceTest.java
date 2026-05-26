package fr.alescis.aelia.service;

import fr.alescis.aelia.provider.simulation.SimulatedWeatherDashboardProvider;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class AeliaWeatherServiceTest {
    @Test
    void serviceDelegatesToProvider() {
        try (AeliaWeatherService service = new AeliaWeatherService(new SimulatedWeatherDashboardProvider())) {
            assertEquals("Paris", service.currentSnapshot().currentWeather().city());
            assertFalse(service.supportedMetrics().isEmpty());
            assertFalse(service.apiLimits().isEmpty());
        }
    }
}
