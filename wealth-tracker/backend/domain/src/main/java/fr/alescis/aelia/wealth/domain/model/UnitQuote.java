package fr.alescis.aelia.wealth.domain.model;

import fr.alescis.aelia.wealth.domain.common.MoneyAmount;

import java.time.Instant;

import static fr.alescis.aelia.wealth.domain.common.DomainValidation.optionalText;
import static fr.alescis.aelia.wealth.domain.common.DomainValidation.required;

public record UnitQuote(
        EntityId id,
        EntityId trackedItemId,
        Instant observedAt,
        MoneyAmount unitValue,
        DataQuality quality,
        String source
) {
    public UnitQuote {
        id = required(id, "id");
        trackedItemId = required(trackedItemId, "tracked item id");
        observedAt = required(observedAt, "observed at");
        unitValue = required(unitValue, "unit value");
        if (unitValue.isNegative()) {
            throw new IllegalArgumentException("unit value must be positive or zero");
        }
        quality = required(quality, "quality");
        source = optionalText(source);
    }
}
