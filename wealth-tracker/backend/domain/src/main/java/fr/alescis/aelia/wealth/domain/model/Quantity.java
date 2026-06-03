package fr.alescis.aelia.wealth.domain.model;

import java.math.BigDecimal;

import static fr.alescis.aelia.wealth.domain.common.DomainValidation.nonNegative;

public record Quantity(BigDecimal value) {
    public Quantity {
        value = nonNegative(value, "quantity");
    }

    public static Quantity zero() {
        return new Quantity(BigDecimal.ZERO);
    }

    public static Quantity of(String value) {
        return new Quantity(new BigDecimal(value));
    }
}
