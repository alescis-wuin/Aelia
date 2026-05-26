package fr.alescis.aelia.ui.components;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GaugeMathTest {
    @Test
    void normalizeClampsToDisplayedRange() {
        assertEquals(0.0, GaugeMath.normalize(-10.0, 0.0, 100.0));
        assertEquals(0.18, GaugeMath.normalize(18.0, 0.0, 100.0), 0.0001);
        assertEquals(1.0, GaugeMath.normalize(150.0, 0.0, 100.0));
    }

    @Test
    void semicirclePointReturnsLeftTopRightPath() {
        GaugePoint left = GaugeMath.semicirclePoint(100, 100, 50, 50, 0.0);
        GaugePoint top = GaugeMath.semicirclePoint(100, 100, 50, 50, 0.5);
        GaugePoint right = GaugeMath.semicirclePoint(100, 100, 50, 50, 1.0);

        assertEquals(50.0, left.x(), 0.0001);
        assertEquals(100.0, left.y(), 0.0001);
        assertEquals(100.0, top.x(), 0.0001);
        assertEquals(50.0, top.y(), 0.0001);
        assertEquals(150.0, right.x(), 0.0001);
        assertEquals(100.0, right.y(), 0.0001);
    }

    @Test
    void clampRejectsInvalidBounds() {
        assertThrows(IllegalArgumentException.class, () -> GaugeMath.clamp(1.0, 2.0, 1.0));
    }
}
