package fr.alescis.aelia.wealth.domain.model;

import java.time.Instant;

import static fr.alescis.aelia.wealth.domain.common.DomainValidation.optionalText;
import static fr.alescis.aelia.wealth.domain.common.DomainValidation.required;

public record QuantitySample(
        EntityId id,
        EntityId linkId,
        Instant observedAt,
        Quantity quantity,
        DataQuality quality,
        String source
) {
    public QuantitySample {
        id = required(id, "id");
        linkId = required(linkId, "link id");
        observedAt = required(observedAt, "observed at");
        quantity = required(quantity, "quantity");
        quality = required(quality, "quality");
        source = optionalText(source);
    }
}
