package fr.alescis.aelia.provider;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Centralized runtime setting resolver.
 *
 * <p>Resolution order is intentionally explicit and deterministic:</p>
 * <ol>
 *     <li>Java system property, for Maven and command-line overrides.</li>
 *     <li>Operating-system environment variable.</li>
 *     <li>Project-local dotenv file.</li>
 * </ol>
 *
 * <p>The dotenv file defaults to {@code .env} in the current working directory.
 * It can be overridden with {@code -Daelia.env.path=/path/to/.env} or
 * {@code AELIA_ENV_PATH=/path/to/.env}. Values are never copied into system
 * properties and are never logged, which keeps provider keys local to the
 * resolver.</p>
 */
public final class EnvironmentSettings {
    private static final String ENV_PATH_PROPERTY = "aelia.env.path";
    private static final String ENV_PATH_ENVIRONMENT = "AELIA_ENV_PATH";
    private static final Path DEFAULT_DOTENV_PATH = Path.of(".env");
    private static volatile Map<String, String> dotenvValues;

    private EnvironmentSettings() {
    }

    public static Optional<String> text(String propertyName, String environmentName) {
        String value = normalized(System.getProperty(propertyName));
        if (value != null) {
            return Optional.of(value);
        }

        value = normalized(System.getenv(environmentName));
        if (value != null) {
            return Optional.of(value);
        }

        Map<String, String> dotenv = dotenvValues();
        value = normalized(dotenv.get(environmentName));
        if (value != null) {
            return Optional.of(value);
        }

        value = normalized(dotenv.get(propertyName));
        return value == null ? Optional.empty() : Optional.of(value);
    }

    public static Optional<Path> dotenvPath() {
        String configured = normalized(System.getProperty(ENV_PATH_PROPERTY));
        if (configured == null) {
            configured = normalized(System.getenv(ENV_PATH_ENVIRONMENT));
        }
        Path path = configured == null ? DEFAULT_DOTENV_PATH : Path.of(configured);
        return Files.isRegularFile(path) ? Optional.of(path.toAbsolutePath().normalize()) : Optional.empty();
    }

    static Map<String, String> dotenvValues() {
        Map<String, String> current = dotenvValues;
        if (current == null) {
            synchronized (EnvironmentSettings.class) {
                current = dotenvValues;
                if (current == null) {
                    current = loadDotenv();
                    dotenvValues = current;
                }
            }
        }
        return current;
    }

    private static Map<String, String> loadDotenv() {
        String configured = normalized(System.getProperty(ENV_PATH_PROPERTY));
        if (configured == null) {
            configured = normalized(System.getenv(ENV_PATH_ENVIRONMENT));
        }
        Path path = configured == null ? DEFAULT_DOTENV_PATH : Path.of(configured);
        if (!Files.isRegularFile(path)) {
            return Map.of();
        }
        try {
            Map<String, String> values = new LinkedHashMap<>();
            for (String rawLine : Files.readAllLines(path, StandardCharsets.UTF_8)) {
                parseLine(rawLine).ifPresent(entry -> values.put(entry.name(), entry.value()));
            }
            return Map.copyOf(values);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read dotenv file: " + path.toAbsolutePath().normalize(), exception);
        }
    }

    private static Optional<DotenvEntry> parseLine(String rawLine) {
        if (rawLine == null) {
            return Optional.empty();
        }
        String line = stripBom(rawLine).trim();
        if (line.isEmpty() || line.startsWith("#")) {
            return Optional.empty();
        }
        if (line.startsWith("export ")) {
            line = line.substring("export ".length()).trim();
        }
        int separator = line.indexOf('=');
        if (separator <= 0) {
            return Optional.empty();
        }
        String name = line.substring(0, separator).trim();
        String value = line.substring(separator + 1).trim();
        if (name.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new DotenvEntry(name, normalizeDotenvValue(value)));
    }

    private static String normalizeDotenvValue(String value) {
        if (value.length() >= 2) {
            char first = value.charAt(0);
            char last = value.charAt(value.length() - 1);
            if (first == '\'' && last == '\'') {
                return value.substring(1, value.length() - 1);
            }
            if (first == '"' && last == '"') {
                return unescapeDoubleQuoted(value.substring(1, value.length() - 1));
            }
        }
        return stripInlineComment(value).trim();
    }

    private static String stripInlineComment(String value) {
        for (int index = 0; index < value.length(); index++) {
            if (value.charAt(index) == '#' && (index == 0 || Character.isWhitespace(value.charAt(index - 1)))) {
                return value.substring(0, index);
            }
        }
        return value;
    }

    private static String unescapeDoubleQuoted(String value) {
        StringBuilder builder = new StringBuilder(value.length());
        boolean escaping = false;
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (escaping) {
                builder.append(switch (current) {
                    case 'n' -> '\n';
                    case 'r' -> '\r';
                    case 't' -> '\t';
                    case '"' -> '"';
                    case '\\' -> '\\';
                    default -> current;
                });
                escaping = false;
            } else if (current == '\\') {
                escaping = true;
            } else {
                builder.append(current);
            }
        }
        if (escaping) {
            builder.append('\\');
        }
        return builder.toString();
    }

    private static String normalized(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private static String stripBom(String line) {
        return line.startsWith("\uFEFF") ? line.substring(1) : line;
    }

    private record DotenvEntry(String name, String value) {
    }
}
