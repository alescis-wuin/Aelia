package fr.alescis.aelia.wealth.domain.common;

import java.util.Currency;
import java.util.Locale;

import static fr.alescis.aelia.wealth.domain.common.DomainValidation.requiredText;

public record CurrencyCode(String value) {
    public CurrencyCode {
        String normalized = requiredText(value, "currency code").toUpperCase(Locale.ROOT);
        Currency.getInstance(normalized);
        value = normalized;
    }

    public static CurrencyCode of(String value) {
        return new CurrencyCode(value);
    }

    public Currency javaCurrency() {
        return Currency.getInstance(value);
    }
}
