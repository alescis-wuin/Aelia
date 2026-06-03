package fr.alescis.aelia.wealth.domain.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MoneyAmountTest {
    @Test
    void shouldAddAmountsWithSameCurrency() {
        MoneyAmount result = MoneyAmount.of("12.50", "EUR").plus(MoneyAmount.of("7.50", "EUR"));

        assertEquals(0, result.amount().compareTo(new java.math.BigDecimal("20")));
        assertEquals(CurrencyCode.of("EUR"), result.currency());
    }

    @Test
    void shouldSubtractAmountsWithSameCurrency() {
        MoneyAmount result = MoneyAmount.of("12.50", "EUR").minus(MoneyAmount.of("2.25", "EUR"));

        assertEquals(0, result.amount().compareTo(new java.math.BigDecimal("10.25")));
    }

    @Test
    void shouldRejectMixedCurrencyArithmetic() {
        MoneyAmount eur = MoneyAmount.of("10", "EUR");
        MoneyAmount usd = MoneyAmount.of("10", "USD");

        assertThrows(IllegalArgumentException.class, () -> eur.plus(usd));
    }
}
