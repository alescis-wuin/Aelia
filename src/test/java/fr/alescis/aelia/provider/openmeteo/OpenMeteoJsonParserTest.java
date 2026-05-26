package fr.alescis.aelia.provider.openmeteo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenMeteoJsonParserTest {
    @Test
    void parsesNestedObjectsArraysAndEscapes() {
        OpenMeteoJsonValue root = OpenMeteoJsonParser.parse("{\"city\":\"Par\\u0069s\",\"values\":[1,2.5,true,null]}");

        assertEquals("Paris", root.get("city").asString(""));
        assertEquals(4, root.get("values").size());
        assertTrue(root.get("values").get(2).asBoolean(false));
        assertFalse(root.get("missing").isPresent());
    }
}
