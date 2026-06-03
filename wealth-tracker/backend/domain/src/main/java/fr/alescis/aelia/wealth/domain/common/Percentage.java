package fr.alescis.aelia.wealth.domain.common;

import java.math.BigDecimal;
import java.math.MathContext;

import static fr.alescis.aelia.wealth.domain.common.DomainValidation.required;

public record Percentage(BigDecimal decimalValue) {
    public Percentage {
        decimalValue = required(decimalValue, "percentage decimal value").stripTrailingZeros();
    }

    public static Percentage ofDecimal(String decimalValue) {
        return new Percentage(new BigDecimal(decimalValue));
    }

    public BigDecimal asPercent(MathContext mathContext) {
        return decimalValue.multiply(new BigDecimal("100"), required(mathContext, "mathContext"));
    }
}
