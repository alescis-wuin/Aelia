package fr.alescis.aelia.provider;

import java.time.Instant;

/**
 * Small console diagnostic helper for provider selection, HTTP calls and failovers.
 */
public final class ProviderDiagnostics {
    private static final boolean ENABLED = EnvironmentSettings.bool("aelia.diagnostics", "AELIA_DIAGNOSTICS", true);

    private ProviderDiagnostics() {
    }

    public static void info(String message) {
        if (ENABLED) {
            System.out.println(prefix("INFO") + message);
        }
    }

    public static void warn(String message, Throwable throwable) {
        if (ENABLED) {
            System.err.println(prefix("WARN") + message);
            if (throwable != null) {
                throwable.printStackTrace(System.err);
            }
        }
    }

    public static void error(String message, Throwable throwable) {
        System.err.println(prefix("ERROR") + message);
        if (throwable != null) {
            throwable.printStackTrace(System.err);
        }
    }

    private static String prefix(String level) {
        return "[Aelia] " + Instant.now() + " " + level + " ";
    }
}
