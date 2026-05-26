package fr.alescis.aelia.ui;

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
    };

    void selectProviderMode(String mode);

    void refreshProviderData();
}
