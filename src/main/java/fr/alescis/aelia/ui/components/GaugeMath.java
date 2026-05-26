package fr.alescis.aelia.ui.components;

/**
 * Numeric helpers for gauges and arcs.
 */
public final class GaugeMath {
    private GaugeMath() {
    }

    public static double clamp(double value, double minimum, double maximum) {
        if (maximum < minimum) {
            throw new IllegalArgumentException("Maximum must be greater than or equal to minimum.");
        }
        return Math.max(minimum, Math.min(maximum, value));
    }

    public static double normalize(double value, double minimum, double maximum) {
        if (Double.compare(maximum, minimum) == 0) {
            return 0.0;
        }
        return clamp((value - minimum) / (maximum - minimum), 0.0, 1.0);
    }

    public static GaugePoint semicirclePoint(double centerX, double centerY, double radiusX, double radiusY, double progress) {
        double normalized = clamp(progress, 0.0, 1.0);
        double radians = Math.toRadians(180.0 - normalized * 180.0);
        double x = centerX + Math.cos(radians) * radiusX;
        double y = centerY - Math.sin(radians) * radiusY;
        return new GaugePoint(x, y);
    }

    public static GaugePoint circlePoint(double centerX, double centerY, double radius, double progress) {
        double radians = Math.toRadians(90.0 - clamp(progress, 0.0, 1.0) * 360.0);
        double x = centerX + Math.cos(radians) * radius;
        double y = centerY - Math.sin(radians) * radius;
        return new GaugePoint(x, y);
    }
}
