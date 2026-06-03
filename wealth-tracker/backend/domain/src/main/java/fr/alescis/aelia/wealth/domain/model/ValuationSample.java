package fr.alescis.aelia.wealth.domain.model;

import fr.alescis.aelia.wealth.domain.common.MoneyAmount;

import java.time.Instant;

import static fr.alescis.aelia.wealth.domain.common.DomainValidation.optionalText;
import static fr.alescis.aelia.wealth.domain.common.DomainValidation.required;

public record ValuationSample(
        EntityId id,
        EntityId targetId,
        Instant observedAt,
        MoneyAmount value,
        DataQuality quality,
        ValuationMethod method,
        String source
) {
    public ValuationSample {
        id = required(id, "id");
        targetId = required(targetId, "target id");
        observedAt = required(observedAt, "observed at");
        value = required(value, "value");
        quality = required(quality, "quality");
        method = required(method, "method");
        source = optionalText(source);
    }
}
