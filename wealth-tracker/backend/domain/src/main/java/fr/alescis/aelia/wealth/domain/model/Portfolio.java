package fr.alescis.aelia.wealth.domain.model;

import fr.alescis.aelia.wealth.domain.common.CurrencyCode;

import static fr.alescis.aelia.wealth.domain.common.DomainValidation.required;
import static fr.alescis.aelia.wealth.domain.common.DomainValidation.requiredText;

public record Portfolio(
        EntityId id,
        String name,
        CurrencyCode reportingCurrency
) {
    public Portfolio {
        id = required(id, "portfolio id");
        name = requiredText(name, "portfolio name");
        reportingCurrency = required(reportingCurrency, "reporting currency");
    }
}
