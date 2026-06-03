package fr.alescis.aelia.wealth.domain.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CurrencyCodeTest {
    @Test
    void shouldNormalizeCurrencyCode() {
        CurrencyCode code = CurrencyCode.of("eur");

        assertEquals("EUR", code.value());
    }

    @Test
    void shouldRejectInvalidCurrencyCode() {
        assertThrows(IllegalArgumentException.class, () -> CurrencyCode.of("not-a-currency"));
    }
}
