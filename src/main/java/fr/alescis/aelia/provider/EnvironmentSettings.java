package fr.alescis.aelia.provider;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Reads runtime settings from Java system properties, environment variables and an optional .env file.
 */
public final class EnvironmentSettings {
    private static final Map<String, String> DOT_ENV = loadDotEnv();

    private EnvironmentSettings() {
    }

    public static Optional<String> text(String propertyName, String environmentName) {
        String property = System.getProperty(propertyName);
        if (property != null && !property.isBlank()) {
            return Optional.of(property.trim());
        }
        String environment = System.getenv(environmentName);
        if (environment != null && !environment.isBlank()) {
            return Optional.of(environment.trim());
        }
        String dotenv = DOT_ENV.get(environmentName);
        if (dotenv != null && !dotenv.isBlank()) {
            return Optional.of(dotenv.trim());
        }
        return Optional.empty();
    }

    public static String text(String propertyName, String environmentName, String fallback) {
        return text(propertyName, environmentName).orElse(fallback);
    }

    public static boolean bool(String propertyName, String environmentName, boolean fallback) {
        return text(propertyName, environmentName)
                .map(value -> switch (value.trim().toLowerCase(Locale.ROOT)) {
                    case "1", "true", "yes", "y", "on", "enabled" -> true;
                    case "0", "false", "no", "n", "off", "disabled" -> false;
                    default -> fallback;
                })
                .orElse(fallback);
    }

    public static Optional<Double> decimal(String propertyName, String environmentName) {
        return text(propertyName, environmentName).flatMap(value -> {
            try {
                return Optional.of(Double.parseDouble(value));
            } catch (NumberFormatException exception) {
                ProviderDiagnostics.warn("Invalid decimal setting " + propertyName + "/" + environmentName + "=" + value + ".", exception);
                return Optional.empty();
            }
        });
    }

    public static int integer(String propertyName, String environmentName, int fallback, int minimum, int maximum) {
        return text(propertyName, environmentName).map(value -> {
            try {
                int parsed = Integer.parseInt(value.trim());
                return Math.max(minimum, Math.min(maximum, parsed));
            } catch (NumberFormatException exception) {
                ProviderDiagnostics.warn("Invalid integer setting " + propertyName + "/" + environmentName + "=" + value + ".", exception);
                return fallback;
            }
        }).orElse(fallback);
    }

    public static Duration seconds(String propertyName, String environmentName, Duration fallback, int minimumSeconds, int maximumSeconds) {
        int seconds = integer(propertyName, environmentName, (int) fallback.toSeconds(), minimumSeconds, maximumSeconds);
        return Duration.ofSeconds(seconds);
    }

    private static Map<String, String> loadDotEnv() {
        Path path = envPath();
        if (!Files.isRegularFile(path)) {
            return Map.of();
        }
        Map<String, String> values = new LinkedHashMap<>();
        try {
            for (String rawLine : Files.readAllLines(path)) {
                String line = rawLine.strip();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                int separator = line.indexOf('=');
                if (separator <= 0) {
                    continue;
                }
                String key = line.substring(0, separator).trim();
                String value = stripQuotes(line.substring(separator + 1).trim());
                if (!key.isBlank()) {
                    values.put(key, value);
                }
            }
        } catch (IOException exception) {
            ProviderDiagnostics.warn("Unable to read .env file at " + path + ".", exception);
        }
        return Map.copyOf(values);
    }

    private static Path envPath() {
        String configured = System.getProperty("aelia.env.path");
        if (configured == null || configured.isBlank()) {
            configured = System.getenv("AELIA_ENV_PATH");
        }
        return configured == null || configured.isBlank() ? Path.of(".env") : Path.of(configured.trim());
    }

    private static String stripQuotes(String value) {
        if (value.length() >= 2) {
            char first = value.charAt(0);
            char last = value.charAt(value.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return value.substring(1, value.length() - 1);
            }
        }
        return value;
    }
}
