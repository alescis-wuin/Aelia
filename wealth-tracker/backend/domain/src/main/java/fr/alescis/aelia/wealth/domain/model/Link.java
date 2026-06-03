package fr.alescis.aelia.wealth.domain.model;

import static fr.alescis.aelia.wealth.domain.common.DomainValidation.required;

public record Link(
        EntityId id,
        EntityId portfolioId,
        EntityId bucketId,
        EntityId trackedItemId,
        boolean active
) {
    public Link {
        id = required(id, "id");
        portfolioId = required(portfolioId, "portfolio id");
        bucketId = required(bucketId, "bucket id");
        trackedItemId = required(trackedItemId, "tracked item id");
    }
}
