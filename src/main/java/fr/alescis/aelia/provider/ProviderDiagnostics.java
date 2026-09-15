package fr.alescis.aelia.provider;

import java.time.Instant;
import java.util.Locale;

/**
 * Small console diagnostic helper for provider selection, HTTP calls and failovers.
 */
public final class ProviderDiagnostics {
    private ProviderDiagnostics() {
    }

    public static void info(String message) {
        if (enabled()) {
            System.out.println(prefix("INFO") + message);
        }
    }

    public static void warn(String message, Throwable throwable) {
        if (enabled()) {
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

    private static boolean enabled() {
        String value = System.getProperty("aelia.diagnostics");
        if (value == null || value.isBlank()) {
            value = System.getenv("AELIA_DIAGNOSTICS");
        }
        if (value == null || value.isBlank()) {
            return true;
        }
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "0", "false", "no", "off", "disabled" -> false;
            default -> true;
        };
    }

    private static String prefix(String level) {
        return "[Aelia] " + Instant.now() + " " + level + " ";
    }
}
