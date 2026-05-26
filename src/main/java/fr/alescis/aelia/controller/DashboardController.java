package fr.alescis.aelia.controller;

import fr.alescis.aelia.model.DashboardSnapshot;
import fr.alescis.aelia.service.WeatherService;

import java.util.Objects;

/**
 * Thin controller for future interactive dashboard actions.
 */
public final class DashboardController {
    private final WeatherService weatherService;

    public DashboardController(WeatherService weatherService) {
        this.weatherService = Objects.requireNonNull(weatherService, "weatherService");
    }

    public DashboardSnapshot currentSnapshot() {
        return weatherService.currentSnapshot();
    }
}
