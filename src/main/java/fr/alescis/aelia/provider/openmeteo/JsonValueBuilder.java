package fr.alescis.aelia.provider.openmeteo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Package-local helpers used by fallback mappers to produce Open-Meteo-shaped JSON values.
 */
final class JsonValueBuilder {
    private JsonValueBuilder() {
    }

    static OpenMeteoJsonValue object(Map<String, OpenMeteoJsonValue> values) {
        return new OpenMeteoJsonValue(new LinkedHashMap<>(values));
    }

    static OpenMeteoJsonValue array(List<OpenMeteoJsonValue> values) {
        return new OpenMeteoJsonValue(new ArrayList<>(values));
    }

    static OpenMeteoJsonValue value(Object value) {
        return new OpenMeteoJsonValue(value);
    }
}
