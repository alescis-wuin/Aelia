package fr.alescis.aelia.ui;

import fr.alescis.aelia.model.LocationWeather;

/**
 * Actions exposed by the JavaFX application shell to dashboard settings controls.
 */
public interface DashboardRuntimeActions {
    DashboardRuntimeActions NO_OP = new DashboardRuntimeActions() {
        @Override
        public void selectProviderMode(String mode) {
        }

        @Override
        public void refreshProviderData() {
        }

        @Override
        public void selectWeatherLocation(LocationWeather location) {
        }
    };

    void selectProviderMode(String mode);

    void refreshProviderData();

    void selectWeatherLocation(LocationWeather location);
}
