package fr.alescis.aelia.wealth.domain.common;

import java.math.BigDecimal;
import java.util.Objects;

public final class DomainValidation {
    private DomainValidation() {
    }

    public static <T> T required(T value, String fieldName) {
        return Objects.requireNonNull(value, fieldName + " is required");
    }

    public static String requiredText(String value, String fieldName) {
        String text = required(value, fieldName).trim();
        if (text.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return text;
    }

    public static String optionalText(String value) {
        return value == null ? "" : value.trim();
    }

    public static BigDecimal nonNegative(BigDecimal value, String fieldName) {
        BigDecimal number = required(value, fieldName).stripTrailingZeros();
        if (number.signum() < 0) {
            throw new IllegalArgumentException(fieldName + " must be positive or zero");
        }
        return number;
    }

    public static BigDecimal strictlyPositive(BigDecimal value, String fieldName) {
        BigDecimal number = required(value, fieldName).stripTrailingZeros();
        if (number.signum() <= 0) {
            throw new IllegalArgumentException(fieldName + " must be strictly positive");
        }
        return number;
    }
}
