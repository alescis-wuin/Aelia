package fr.alescis.aelia.wealth.domain.model;

import fr.alescis.aelia.wealth.domain.common.MoneyAmount;

import java.time.Instant;

import static fr.alescis.aelia.wealth.domain.common.DomainValidation.optionalText;
import static fr.alescis.aelia.wealth.domain.common.DomainValidation.required;
import static fr.alescis.aelia.wealth.domain.common.DomainValidation.requiredText;

public record FlowEntry(
        EntityId id,
        EntityId bucketId,
        Instant occurredAt,
        CashFlowType type,
        MoneyAmount value,
        String label,
        String source
) {
    public FlowEntry {
        id = required(id, "id");
        bucketId = required(bucketId, "bucket id");
        occurredAt = required(occurredAt, "occurred at");
        type = required(type, "type");
        value = required(value, "value");
        if (!value.isPositive()) {
            throw new IllegalArgumentException("flow value must be strictly positive; direction is carried by type");
        }
        label = requiredText(label, "label");
        source = optionalText(source);
    }

    public boolean increasesExternalCapital() {
        return type == CashFlowType.DEPOSIT || type == CashFlowType.TRANSFER_IN || type == CashFlowType.CASHBACK || type == CashFlowType.REWARD;
    }

    public boolean decreasesExternalCapital() {
        return type == CashFlowType.WITHDRAWAL || type == CashFlowType.TRANSFER_OUT || type == CashFlowType.FEE || type == CashFlowType.TAX;
    }
}
