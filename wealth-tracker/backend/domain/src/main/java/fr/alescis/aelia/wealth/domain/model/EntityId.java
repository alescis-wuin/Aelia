package fr.alescis.aelia.wealth.domain.model;

import java.util.UUID;

import static fr.alescis.aelia.wealth.domain.common.DomainValidation.required;

public record EntityId(UUID value) {
    public EntityId {
        value = required(value, "entity id");
    }

    public static EntityId random() {
        return new EntityId(UUID.randomUUID());
    }

    public static EntityId of(UUID value) {
        return new EntityId(value);
    }
}
