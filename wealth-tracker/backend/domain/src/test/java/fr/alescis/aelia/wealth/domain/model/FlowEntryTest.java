package fr.alescis.aelia.wealth.domain.model;

import fr.alescis.aelia.wealth.domain.common.MoneyAmount;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlowEntryTest {
    @Test
    void shouldClassifyDepositAsExternalCapitalIncrease() {
        FlowEntry entry = new FlowEntry(
                EntityId.random(),
                EntityId.random(),
                Instant.parse("2026-01-01T00:00:00Z"),
                CashFlowType.DEPOSIT,
                MoneyAmount.of("100", "EUR"),
                "Initial funding",
                "manual"
        );

        assertTrue(entry.increasesExternalCapital());
        assertFalse(entry.decreasesExternalCapital());
    }

    @Test
    void shouldRejectZeroFlowValue() {
        assertThrows(IllegalArgumentException.class, () -> new FlowEntry(
                EntityId.random(),
                EntityId.random(),
                Instant.parse("2026-01-01T00:00:00Z"),
                CashFlowType.DEPOSIT,
                MoneyAmount.of("0", "EUR"),
                "Invalid movement",
                "manual"
        ));
    }
}
