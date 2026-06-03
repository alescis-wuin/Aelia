package fr.alescis.aelia.wealth.domain.common;

import java.math.BigDecimal;
import java.math.MathContext;

import static fr.alescis.aelia.wealth.domain.common.DomainValidation.required;

public record MoneyAmount(BigDecimal amount, CurrencyCode currency) implements Comparable<MoneyAmount> {
    public MoneyAmount {
        amount = required(amount, "amount").stripTrailingZeros();
        currency = required(currency, "currency");
    }

    public static MoneyAmount of(String amount, String currencyCode) {
        return new MoneyAmount(new BigDecimal(amount), CurrencyCode.of(currencyCode));
    }

    public static MoneyAmount zero(CurrencyCode currency) {
        return new MoneyAmount(BigDecimal.ZERO, currency);
    }

    public MoneyAmount plus(MoneyAmount other) {
        ensureSameCurrency(other);
        return new MoneyAmount(amount.add(other.amount), currency);
    }

    public MoneyAmount minus(MoneyAmount other) {
        ensureSameCurrency(other);
        return new MoneyAmount(amount.subtract(other.amount), currency);
    }

    public MoneyAmount negate() {
        return new MoneyAmount(amount.negate(), currency);
    }

    public MoneyAmount multiply(BigDecimal multiplier, MathContext mathContext) {
        return new MoneyAmount(amount.multiply(required(multiplier, "multiplier"), required(mathContext, "mathContext")), currency);
    }

    public boolean isNegative() {
        return amount.signum() < 0;
    }

    public boolean isPositive() {
        return amount.signum() > 0;
    }

    public boolean isZero() {
        return amount.signum() == 0;
    }

    @Override
    public int compareTo(MoneyAmount other) {
        ensureSameCurrency(other);
        return amount.compareTo(other.amount);
    }

    private void ensureSameCurrency(MoneyAmount other) {
        MoneyAmount checked = required(other, "other money amount");
        if (!currency.equals(checked.currency)) {
            throw new IllegalArgumentException("currency mismatch: " + currency.value() + " != " + checked.currency.value());
        }
    }
}
