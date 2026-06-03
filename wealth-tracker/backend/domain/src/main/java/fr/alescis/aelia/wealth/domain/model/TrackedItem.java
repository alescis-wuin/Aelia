package fr.alescis.aelia.wealth.domain.model;

import fr.alescis.aelia.wealth.domain.common.CurrencyCode;

import static fr.alescis.aelia.wealth.domain.common.DomainValidation.required;
import static fr.alescis.aelia.wealth.domain.common.DomainValidation.requiredText;

public record TrackedItem(
        EntityId id,
        String symbol,
        String name,
        AssetType type,
        CurrencyCode valuationCurrency,
        boolean active
) {
    public TrackedItem {
        id = required(id, "id");
        symbol = requiredText(symbol, "symbol").toUpperCase();
        name = requiredText(name, "name");
        type = required(type, "type");
        valuationCurrency = required(valuationCurrency, "valuation currency");
    }
}
