package fr.alescis.aelia.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NominatimJsonTest {
    @Test
    void parseCityAndCountryFromAddress() {
        String json = """
                {
                  "display_name": "Rouen, Seine-Maritime, Normandie, France",
                  "address": {
                    "city": "Rouen",
                    "country": "France"
                  }
                }
                """;

        ReverseGeocodeResult result = NominatimJson.parse(json, 49.4432, 1.0993);

        assertEquals("Rouen", result.city());
        assertEquals("France", result.country());
        assertEquals(49.4432, result.latitude());
        assertEquals(1.0993, result.longitude());
    }

    @Test
    void fallbackToDisplayNameFirstPartWhenNoCityIsPresent() {
        String json = """
                {
                  "display_name": "Mont Saint-Michel, Avranches, France",
                  "address": {"country": "France"}
                }
                """;

        ReverseGeocodeResult result = NominatimJson.parse(json, 48.636, -1.511);

        assertEquals("Mont Saint-Michel", result.city());
        assertEquals("France", result.country());
    }
}
