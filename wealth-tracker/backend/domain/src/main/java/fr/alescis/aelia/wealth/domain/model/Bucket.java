package fr.alescis.aelia.wealth.domain.model;

import static fr.alescis.aelia.wealth.domain.common.DomainValidation.required;
import static fr.alescis.aelia.wealth.domain.common.DomainValidation.requiredText;

public record Bucket(EntityId id, EntityId portfolioId, String name, boolean active) {
    public Bucket {
        id = required(id, "id");
        portfolioId = required(portfolioId, "portfolio id");
        name = requiredText(name, "name");
    }
}
