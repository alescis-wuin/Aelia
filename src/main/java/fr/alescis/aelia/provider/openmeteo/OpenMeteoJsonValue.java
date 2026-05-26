package fr.alescis.aelia.provider.openmeteo;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Small JSON value wrapper tailored to provider payload mapping without external dependencies.
 */
public final class OpenMeteoJsonValue {
    private static final Object MISSING_MARKER = new Object();
    private static final OpenMeteoJsonValue MISSING = new OpenMeteoJsonValue(MISSING_MARKER);

    private final Object value;

    OpenMeteoJsonValue(Object value) {
        this.value = value;
    }

    public static OpenMeteoJsonValue missing() {
        return MISSING;
    }

    public boolean isMissing() {
        return value == MISSING_MARKER;
    }

    public boolean isNull() {
        return value == null;
    }

    public boolean isPresent() {
        return !isMissing() && !isNull();
    }

    public OpenMeteoJsonValue get(String name) {
        if (!(value instanceof Map<?, ?> map)) {
            return MISSING;
        }
        Object child = map.get(name);
        return child instanceof OpenMeteoJsonValue jsonValue ? jsonValue : MISSING;
    }

    public OpenMeteoJsonValue get(int index) {
        if (!(value instanceof List<?> list) || index < 0 || index >= list.size()) {
            return MISSING;
        }
        Object child = list.get(index);
        return child instanceof OpenMeteoJsonValue jsonValue ? jsonValue : MISSING;
    }

    public boolean has(String name) {
        return get(name).isPresent();
    }

    public String asString(String fallback) {
        if (value instanceof String string) {
            return string;
        }
        if (value instanceof Number number) {
            return String.valueOf(number);
        }
        if (value instanceof Boolean bool) {
            return String.valueOf(bool);
        }
        return fallback;
    }

    public double asDouble(double fallback) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String string && !string.isBlank()) {
            try {
                return Double.parseDouble(string);
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    public int asRoundedInt(int fallback) {
        double number = asDouble(Double.NaN);
        return Double.isNaN(number) ? fallback : (int) Math.round(number);
    }

    public boolean asBoolean(boolean fallback) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof Number number) {
            return number.intValue() != 0;
        }
        if (value instanceof String string) {
            return Boolean.parseBoolean(string);
        }
        return fallback;
    }

    public int size() {
        if (value instanceof List<?> list) {
            return list.size();
        }
        if (value instanceof Map<?, ?> map) {
            return map.size();
        }
        return 0;
    }

    public boolean isArray() {
        return value instanceof List<?>;
    }

    public List<OpenMeteoJsonValue> asArray() {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        return list.stream()
                .filter(OpenMeteoJsonValue.class::isInstance)
                .map(OpenMeteoJsonValue.class::cast)
                .toList();
    }

    public Map<String, OpenMeteoJsonValue> asObject() {
        if (!(value instanceof Map<?, ?> map)) {
            return Map.of();
        }
        java.util.LinkedHashMap<String, OpenMeteoJsonValue> object = new java.util.LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (entry.getKey() instanceof String key && entry.getValue() instanceof OpenMeteoJsonValue jsonValue) {
                object.put(key, jsonValue);
            }
        }
        return Map.copyOf(object);
    }

    @Override
    public String toString() {
        return isMissing() ? "<missing>" : Objects.toString(value);
    }
}
