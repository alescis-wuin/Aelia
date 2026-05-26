package fr.alescis.aelia.provider;

import java.io.PrintStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Lightweight runtime diagnostics used before a logging dependency is introduced.
 */
public final class ProviderDiagnostics {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String DIAGNOSTICS_PROPERTY = "aelia.diagnostics";
    private static final String DIAGNOSTICS_ENVIRONMENT = "AELIA_DIAGNOSTICS";

    private ProviderDiagnostics() {
    }

    public static void info(String message) {
        if (enabled()) {
            print(System.err, "INFO", message, null);
        }
    }

    public static void warn(String message, Throwable error) {
        print(System.err, "WARN", message, error);
    }

    public static void error(String message, Throwable error) {
        print(System.err, "ERROR", message, error);
    }

    public static boolean enabled() {
        return EnvironmentSettings.text(DIAGNOSTICS_PROPERTY, DIAGNOSTICS_ENVIRONMENT)
                .map(value -> !"false".equalsIgnoreCase(value.trim()))
                .orElse(true);
    }

    private static void print(PrintStream stream, String level, String message, Throwable error) {
        stream.println("[Aelia] " + FORMATTER.format(LocalDateTime.now()) + " " + level + " " + message);
        if (error != null) {
            error.printStackTrace(stream);
        }
    }
}
