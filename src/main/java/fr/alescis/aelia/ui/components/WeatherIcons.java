package fr.alescis.aelia.ui.components;

import fr.alescis.aelia.model.WeatherCondition;
import fr.alescis.aelia.ui.Palette;
import javafx.geometry.Bounds;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;

/**
 * Lightweight vector icon factory used by the dashboard cards.
 */
public final class WeatherIcons {
    private WeatherIcons() {
    }

    public static Node logoCloud() {
        SVGPath path = path("M14 22 Q14 14 22 14 Q20 8 28 6 Q36 4 38 12 Q44 10 46 16 Q52 16 52 22 Q52 28 46 28 L18 28 Q14 28 14 22Z");
        path.setFill(Color.TRANSPARENT);
        path.setStroke(Palette.CYAN);
        path.setStrokeWidth(2.0);
        return path;
    }

    public static Node sun(double radius) {
        Group group = new Group();
        Circle circle = new Circle(radius, Palette.YELLOW);
        circle.setOpacity(0.88);
        group.getChildren().add(circle);
        for (int i = 0; i < 8; i++) {
            double angle = Math.toRadians(i * 45.0);
            double inner = radius + 13.0;
            double outer = radius + 23.0;
            Line ray = new Line(Math.cos(angle) * inner, Math.sin(angle) * inner, Math.cos(angle) * outer, Math.sin(angle) * outer);
            ray.setStroke(Palette.withOpacity(Palette.YELLOW, 0.45));
            ray.setStrokeWidth(2.0);
            ray.setStrokeLineCap(StrokeLineCap.ROUND);
            group.getChildren().add(ray);
        }
        return group;
    }

    public static Node smallCondition(WeatherCondition condition) {
        return switch (condition) {
            case SUNNY -> centeredIcon(rawSmallSun(9.0, 0.85));
            case PARTLY_CLOUDY -> centeredIcon(rawCloud(false));
            case RAINY -> centeredIcon(rawRainCloud());
            case CLEAR_NIGHT -> centeredIcon(rawMoon());
        };
    }

    public static Node smallSun(double radius, double opacity) {
        return centeredIcon(rawSmallSun(radius, opacity));
    }

    public static Node moon() {
        return centeredIcon(rawMoon());
    }

    public static Node smallCloud(boolean darker) {
        return centeredIcon(rawCloud(darker));
    }

    public static Node rainCloud() {
        return centeredIcon(rawRainCloud());
    }

    public static Node droplet() {
        SVGPath drop = path("M8 0 C14 6 17 12 17 17 C17 25 12 29 8 29 C4 29 0 25 0 17 C0 12 3 6 8 0Z");
        drop.setFill(Palette.withOpacity(Palette.CYAN_DARK, 0.85));
        return drop;
    }

    public static Node windMark() {
        Group group = new Group();
        SVGPath top = path("M0 8 Q7 4 14 8");
        SVGPath bottom = path("M-2 14 Q7 8 17 14");
        for (SVGPath currentPath : new SVGPath[]{top, bottom}) {
            currentPath.setFill(Color.TRANSPARENT);
            currentPath.setStroke(Palette.LIME);
            currentPath.setStrokeWidth(2.0);
            currentPath.setStrokeLineCap(StrokeLineCap.ROUND);
        }
        group.getChildren().addAll(top, bottom);
        return group;
    }

    public static Node alertTriangle() {
        SVGPath path = path("M10 0 L20 16 L0 16Z");
        path.setFill(Color.TRANSPARENT);
        path.setStroke(Palette.ORANGE);
        path.setStrokeWidth(1.7);
        path.setStrokeLineJoin(StrokeLineJoin.ROUND);
        return path;
    }

    public static Node homeIcon() {
        Group group = new Group();
        SVGPath house = path("M0 12 L10 2 L20 12 L20 26 L0 26Z");
        house.setFill(Color.TRANSPARENT);
        house.setStroke(Palette.CYAN);
        house.setStrokeWidth(1.8);
        Rectangle door = new Rectangle(7, 16, 6, 10);
        door.setArcWidth(2);
        door.setArcHeight(2);
        door.setFill(Palette.CYAN);
        group.getChildren().addAll(house, door);
        return centeredIcon(group);
    }

    public static Node mapIcon() {
        SVGPath map = path("M0 2 L7 0 L14 2 L21 0 L21 18 L14 16 L7 18 L0 16Z");
        map.setFill(Color.TRANSPARENT);
        map.setStroke(Palette.withOpacity(Palette.TEXT, 0.35));
        map.setStrokeWidth(1.5);
        return centeredIcon(map);
    }

    public static Node gearIcon() {
        Group group = new Group();
        Circle outer = new Circle(8, Color.TRANSPARENT);
        outer.setStroke(Palette.withOpacity(Palette.TEXT, 0.35));
        outer.setStrokeWidth(1.4);
        Circle inner = new Circle(3, Palette.withOpacity(Palette.TEXT, 0.35));
        group.getChildren().addAll(outer, inner);
        return centeredIcon(group);
    }

    public static Node searchIcon() {
        Group group = new Group();
        Circle circle = new Circle(0, 0, 6.5);
        circle.setFill(Color.TRANSPARENT);
        circle.setStroke(Palette.withOpacity(Palette.TEXT, 0.32));
        circle.setStrokeWidth(1.3);
        Line line = new Line(5, 5, 10, 10);
        line.setStroke(Palette.withOpacity(Palette.TEXT, 0.32));
        line.setStrokeWidth(1.3);
        line.setStrokeLineCap(StrokeLineCap.ROUND);
        group.getChildren().addAll(circle, line);
        return group;
    }

    public static Node arcSun() {
        Group group = new Group();
        Arc halo = new Arc(0, 0, 72, 72, 0, 360);
        halo.setType(ArcType.ROUND);
        halo.setFill(Palette.withOpacity(Palette.YELLOW, 0.045));
        group.getChildren().add(halo);
        group.getChildren().add(sun(28.0));
        return centeredIcon(group);
    }

    private static Circle rawSmallSun(double radius, double opacity) {
        Circle circle = new Circle(radius, Palette.YELLOW);
        circle.setOpacity(opacity);
        return circle;
    }

    private static SVGPath rawMoon() {
        SVGPath moon = path("M12 0 Q1 0 1 10 Q1 20 12 20 Q6 16 6 10 Q6 4 12 0Z");
        moon.setFill(Palette.withOpacity(Palette.TEXT, 0.72));
        return moon;
    }

    private static Group rawCloud(boolean darker) {
        Group group = new Group();
        Color primary = darker ? Color.web("#506070") : Palette.CLOUD_DARK;
        Color secondary = darker ? Color.web("#607080") : Palette.CLOUD;
        Ellipse left = new Ellipse(8, 8, 8, 5.5);
        left.setFill(primary);
        left.setOpacity(0.85);
        Ellipse right = new Ellipse(15, 6, 6, 4.5);
        right.setFill(secondary);
        Rectangle base = new Rectangle(1, 8, 19, 5);
        base.setArcWidth(5);
        base.setArcHeight(5);
        base.setFill(primary);
        base.setOpacity(0.85);
        group.getChildren().addAll(left, right, base);
        return group;
    }

    private static Group rawRainCloud() {
        Group group = new Group(rawCloud(true));
        for (int i = 0; i < 3; i++) {
            Line drop = new Line(5 + i * 5, 17, 3 + i * 5, 23);
            drop.setStroke(Palette.CYAN_DARK);
            drop.setStrokeWidth(1.5);
            drop.setStrokeLineCap(StrokeLineCap.ROUND);
            group.getChildren().add(drop);
        }
        return group;
    }

    private static Group centeredIcon(Node icon) {
        Bounds bounds = icon.getLayoutBounds();
        icon.setLayoutX(-bounds.getMinX() - bounds.getWidth() / 2.0);
        icon.setLayoutY(-bounds.getMinY() - bounds.getHeight() / 2.0);
        Group wrapper = new Group(icon);
        return wrapper;
    }

    private static SVGPath path(String content) {
        SVGPath path = new SVGPath();
        path.setContent(content);
        return path;
    }
}
