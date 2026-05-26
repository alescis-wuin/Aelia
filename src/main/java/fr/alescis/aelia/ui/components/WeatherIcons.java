package fr.alescis.aelia.ui.components;

import fr.alescis.aelia.model.WeatherCondition;
import fr.alescis.aelia.ui.Palette;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.scene.shape.CubicCurve;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;

/**
 * Lightweight JavaFX vector icons used by the mockup.
 */
public final class WeatherIcons {
    private WeatherIcons() {
    }

    public static Node logoCloud() {
        Group group = new Group();
        Polyline cloud = new Polyline(12, 22, 12, 18, 17, 14, 23, 14, 27, 8, 36, 8, 41, 14, 46, 14, 51, 18, 51, 23, 12, 23);
        styleStroke(cloud, Palette.CYAN, 2.0);
        group.getChildren().add(cloud);
        return group;
    }

    public static Node searchIcon() {
        Group group = new Group();
        Circle lens = new Circle(0, 0, 7);
        lens.setFill(Color.TRANSPARENT);
        lens.setStroke(Palette.withOpacity(Palette.TEXT_MUTED, 0.55));
        lens.setStrokeWidth(1.8);
        Line handle = new Line(5, 5, 12, 12);
        styleStroke(handle, Palette.withOpacity(Palette.TEXT_MUTED, 0.55), 1.8);
        group.getChildren().addAll(lens, handle);
        return group;
    }

    public static Node homeIcon() {
        Group group = new Group();
        Polygon roof = new Polygon(0, 10, 12, 0, 24, 10);
        roof.setFill(Color.TRANSPARENT);
        roof.setStroke(Palette.CYAN);
        roof.setStrokeWidth(2.0);
        roof.setStrokeLineJoin(StrokeLineJoin.ROUND);
        Rectangle body = new Rectangle(4, 10, 16, 16);
        body.setFill(Color.TRANSPARENT);
        body.setStroke(Palette.CYAN);
        body.setStrokeWidth(2.0);
        group.getChildren().addAll(roof, body);
        return group;
    }

    public static Node mapIcon() {
        Group group = new Group();
        Polyline map = new Polyline(0, 5, 8, 2, 17, 5, 26, 2, 26, 21, 17, 24, 8, 21, 0, 24, 0, 5);
        styleStroke(map, Palette.withOpacity(Palette.TEXT_MUTED, 0.55), 2.0);
        Line fold1 = new Line(8, 2, 8, 21);
        Line fold2 = new Line(17, 5, 17, 24);
        styleStroke(fold1, Palette.withOpacity(Palette.TEXT_MUTED, 0.55), 1.5);
        styleStroke(fold2, Palette.withOpacity(Palette.TEXT_MUTED, 0.55), 1.5);
        group.getChildren().addAll(map, fold1, fold2);
        return group;
    }

    public static Node gearIcon() {
        Group group = new Group();
        Circle outer = new Circle(10, 10, 7);
        outer.setFill(Color.TRANSPARENT);
        outer.setStroke(Palette.withOpacity(Palette.TEXT_MUTED, 0.55));
        outer.setStrokeWidth(2.0);
        Circle inner = new Circle(10, 10, 2.5, Color.TRANSPARENT);
        inner.setStroke(Palette.withOpacity(Palette.TEXT_MUTED, 0.55));
        inner.setStrokeWidth(1.5);
        for (int i = 0; i < 8; i++) {
            double angle = Math.toRadians(i * 45.0);
            Line tick = new Line(10 + Math.cos(angle) * 9, 10 + Math.sin(angle) * 9,
                    10 + Math.cos(angle) * 11, 10 + Math.sin(angle) * 11);
            styleStroke(tick, Palette.withOpacity(Palette.TEXT_MUTED, 0.55), 1.6);
            group.getChildren().add(tick);
        }
        group.getChildren().addAll(outer, inner);
        return group;
    }

    public static Node droplet() {
        Group group = new Group();
        CubicCurve left = new CubicCurve(0, 18, -9, 6, -1, -8, 0, -14);
        CubicCurve right = new CubicCurve(0, -14, 12, 0, 9, 13, 0, 18);
        styleStroke(left, Palette.CYAN, 0.0);
        styleStroke(right, Palette.CYAN, 0.0);
        left.setFill(Palette.CYAN_DARK);
        right.setFill(Palette.CYAN_DARK);
        group.getChildren().addAll(left, right);
        return group;
    }

    public static Node windMark() {
        Group group = new Group();
        Arc arc1 = new Arc(0, 9, 12, 6, 25, 120);
        arc1.setFill(Color.TRANSPARENT);
        arc1.setStroke(Palette.LIME);
        arc1.setStrokeWidth(2.0);
        arc1.setType(ArcType.OPEN);
        arc1.setStrokeLineCap(StrokeLineCap.ROUND);
        Arc arc2 = new Arc(-6, 17, 10, 5, 30, 110);
        arc2.setFill(Color.TRANSPARENT);
        arc2.setStroke(Palette.LIME);
        arc2.setStrokeWidth(1.8);
        arc2.setType(ArcType.OPEN);
        arc2.setStrokeLineCap(StrokeLineCap.ROUND);
        group.getChildren().addAll(arc1, arc2);
        return group;
    }

    public static Node alertTriangle() {
        Polygon triangle = new Polygon(0, 18, 12, 0, 24, 18);
        triangle.setFill(Color.TRANSPARENT);
        triangle.setStroke(Palette.ORANGE);
        triangle.setStrokeWidth(2.0);
        triangle.setStrokeLineJoin(StrokeLineJoin.ROUND);
        return triangle;
    }

    public static Node arcSun() {
        Group group = new Group();
        Circle halo = new Circle(0, 0, 72, Palette.withOpacity(Palette.YELLOW, 0.05));
        Circle sun = new Circle(0, 0, 29, Palette.YELLOW);
        group.getChildren().addAll(halo, sun);
        for (int i = 0; i < 8; i++) {
            double angle = Math.toRadians(i * 45.0);
            Line ray = new Line(Math.cos(angle) * 45, Math.sin(angle) * 45,
                    Math.cos(angle) * 56, Math.sin(angle) * 56);
            styleStroke(ray, Palette.withOpacity(Palette.YELLOW, 0.60), 2.0);
            group.getChildren().add(ray);
        }
        return group;
    }

    public static Node conditionIcon(WeatherCondition condition, double size) {
        return switch (condition) {
            case SUNNY -> smallSun(size);
            case PARTLY_CLOUDY, CLOUDY -> smallCloud(size);
            case RAINY -> smallRain(size);
            case CLEAR_NIGHT -> smallMoon(size);
            case STORMY -> smallStorm(size);
            case SNOWY -> smallSnow(size);
        };
    }

    private static Node smallSun(double size) {
        Group group = new Group();
        double radius = size * 0.28;
        Circle sun = new Circle(size / 2.0, size / 2.0, radius, Palette.YELLOW);
        group.getChildren().add(sun);
        return group;
    }

    private static Node smallMoon(double size) {
        Group group = new Group();
        Circle moon = new Circle(size / 2.0, size / 2.0, size * 0.28, Palette.withOpacity(Palette.TEXT_MUTED, 0.85));
        Circle cut = new Circle(size / 2.0 + size * 0.15, size / 2.0 - size * 0.05, size * 0.28, Palette.SURFACE);
        group.getChildren().addAll(moon, cut);
        return group;
    }

    private static Node smallCloud(double size) {
        Group group = new Group();
        Color cloudColor = Color.web("#8BA7C1");
        Circle left = new Circle(size * 0.38, size * 0.52, size * 0.16, cloudColor);
        Circle center = new Circle(size * 0.52, size * 0.46, size * 0.20, cloudColor);
        Circle right = new Circle(size * 0.66, size * 0.53, size * 0.15, cloudColor);
        Rectangle base = new Rectangle(size * 0.30, size * 0.50, size * 0.46, size * 0.15);
        base.setArcWidth(size * 0.12);
        base.setArcHeight(size * 0.12);
        base.setFill(cloudColor);
        group.getChildren().addAll(left, center, right, base);
        return group;
    }

    private static Node smallRain(double size) {
        Group group = new Group(smallCloud(size));
        for (int i = 0; i < 3; i++) {
            Line drop = new Line(size * (0.38 + i * 0.12), size * 0.72, size * (0.34 + i * 0.12), size * 0.88);
            styleStroke(drop, Palette.CYAN_DARK, 2.0);
            group.getChildren().add(drop);
        }
        return group;
    }

    private static Node smallStorm(double size) {
        Group group = new Group(smallCloud(size));
        Polygon bolt = new Polygon(size * 0.53, size * 0.62, size * 0.43, size * 0.86, size * 0.55, size * 0.81, size * 0.47, size * 1.0, size * 0.68, size * 0.72);
        bolt.setFill(Palette.YELLOW);
        group.getChildren().add(bolt);
        return group;
    }

    private static Node smallSnow(double size) {
        Group group = new Group(smallCloud(size));
        Circle snow = new Circle(size * 0.5, size * 0.82, size * 0.04, Palette.TEXT);
        group.getChildren().add(snow);
        return group;
    }

    private static void styleStroke(Line line, Color color, double width) {
        line.setStroke(color);
        line.setStrokeWidth(width);
        line.setStrokeLineCap(StrokeLineCap.ROUND);
    }

    private static void styleStroke(Polyline line, Color color, double width) {
        line.setFill(Color.TRANSPARENT);
        line.setStroke(color);
        line.setStrokeWidth(width);
        line.setStrokeLineCap(StrokeLineCap.ROUND);
        line.setStrokeLineJoin(StrokeLineJoin.ROUND);
    }

    private static void styleStroke(CubicCurve curve, Color color, double width) {
        curve.setStroke(color);
        curve.setStrokeWidth(width);
        curve.setStrokeLineCap(StrokeLineCap.ROUND);
    }
}
