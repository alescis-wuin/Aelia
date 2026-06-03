package fr.alescis.aelia.wealth.domain.model;

import fr.alescis.aelia.wealth.domain.common.CurrencyCode;

import java.math.BigDecimal;
import java.time.Instant;

import static fr.alescis.aelia.wealth.domain.common.DomainValidation.optionalText;
import static fr.alescis.aelia.wealth.domain.common.DomainValidation.required;
import static fr.alescis.aelia.wealth.domain.common.DomainValidation.strictlyPositive;

public record ExchangeQuote(
        EntityId id,
        CurrencyCode baseCurrency,
        CurrencyCode quoteCurrency,
        Instant observedAt,
        BigDecimal rate,
        DataQuality quality,
        String source
) {
    public ExchangeQuote {
        id = required(id, "id");
        baseCurrency = required(baseCurrency, "base currency");
        quoteCurrency = required(quoteCurrency, "quote currency");
        if (baseCurrency.equals(quoteCurrency)) {
            throw new IllegalArgumentException("base and quote currencies must differ");
        }
        observedAt = required(observedAt, "observed at");
        rate = strictlyPositive(rate, "rate");
        quality = required(quality, "quality");
        source = optionalText(source);
    }
}
