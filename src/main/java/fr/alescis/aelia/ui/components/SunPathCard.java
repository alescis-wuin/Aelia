package fr.alescis.aelia.ui.components;

import fr.alescis.aelia.model.CurrentWeather;
import fr.alescis.aelia.ui.AccessibilitySupport;
import fr.alescis.aelia.ui.Palette;
import fr.alescis.aelia.ui.UiFormatters;
import fr.alescis.aelia.ui.UiText;
import javafx.scene.AccessibleRole;
import javafx.scene.control.Label;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeLineCap;

/**
 * Sun path card showing sunrise, current sun position and sunset.
 */
public final class SunPathCard extends CardPane {
    public SunPathCard(CurrentWeather weather) {
        super(522, 244);
        Label title = UiText.section("Soleil");
        title.setLayoutX(20);
        title.setLayoutY(17);

        double centerX = 261.0;
        double centerY = 198.0;
        double radius = 128.0;
        double progress = weather.daylightProgress();

        Arc path = new Arc(centerX, centerY, radius, radius, 180, -180);
        path.setFill(null);
        path.setStroke(Palette.withOpacity(Palette.BORDER, 0.7));
        path.setStrokeWidth(3.0);
        path.setType(ArcType.OPEN);
        path.getStrokeDashArray().addAll(7.0, 8.0);

        Arc elapsed = new Arc(centerX, centerY, radius, radius, 180, -180.0 * progress);
        elapsed.setFill(null);
        elapsed.setStroke(Palette.YELLOW);
        elapsed.setStrokeWidth(3.0);
        elapsed.setType(ArcType.OPEN);
        elapsed.setStrokeLineCap(StrokeLineCap.BUTT);

        GaugePoint sunPosition = GaugeMath.semicirclePoint(centerX, centerY, radius, radius, progress);
        Circle sunHalo = new Circle(sunPosition.x(), sunPosition.y(), 14, Palette.withOpacity(Palette.YELLOW, 0.18));
        Circle sun = new Circle(sunPosition.x(), sunPosition.y(), 9, Palette.withOpacity(Palette.YELLOW, 0.90));

        Line horizon = new Line(14, 198, 508, 198);
        horizon.setStroke(Palette.BORDER_SOFT);
        horizon.setStrokeWidth(1.0);

        Rectangle daylightBadge = new Rectangle(160, 24);
        daylightBadge.setLayoutX(181);
        daylightBadge.setLayoutY(102);
        daylightBadge.setArcWidth(24);
        daylightBadge.setArcHeight(24);
        daylightBadge.getStyleClass().add("daylight-badge");
        Label duration = UiText.data("☀ " + weather.daylightDuration(), "daylight-text");
        duration.setLayoutX(181);
        duration.setLayoutY(106);
        duration.setPrefWidth(160);

        Label sunrise = UiText.data(UiFormatters.time(weather.sunriseTime()), "sunrise-time");
        sunrise.setLayoutX(14);
        sunrise.setLayoutY(206);
        Label sunriseLabel = UiText.label("Lever du soleil", "sun-label");
        sunriseLabel.setLayoutX(14);
        sunriseLabel.setLayoutY(228);

        Label now = UiText.data(weather.currentSolarTime(), "sun-now");
        now.setLayoutX(227);
        now.setLayoutY(208);
        now.setPrefWidth(68);
        Label nowLabel = UiText.label("Maintenant", "sun-label-center");
        nowLabel.setLayoutX(224);
        nowLabel.setLayoutY(228);
        nowLabel.setPrefWidth(74);

        Label sunset = UiText.data(UiFormatters.time(weather.sunsetTime()), "sunset-time");
        sunset.setLayoutX(438);
        sunset.setLayoutY(206);
        sunset.setPrefWidth(70);
        Label sunsetLabel = UiText.label("Coucher du soleil", "sun-label-right");
        sunsetLabel.setLayoutX(394);
        sunsetLabel.setLayoutY(228);
        sunsetLabel.setPrefWidth(114);

        getChildren().addAll(title, path, elapsed, sunHalo, sun, horizon, daylightBadge, duration,
                sunrise, sunriseLabel, now, nowLabel, sunset, sunsetLabel);
        String tooltipText = "Soleil · lever " + UiFormatters.time(weather.sunriseTime())
                + " · maintenant " + weather.currentSolarTime()
                + " · coucher " + UiFormatters.time(weather.sunsetTime())
                + " · " + weather.daylightDuration();
        TooltipSupport.install(this, tooltipText);
        AccessibilitySupport.describe(this, AccessibleRole.TEXT,
                "Soleil levé à " + UiFormatters.time(weather.sunriseTime()) + ", coucher à "
                        + UiFormatters.time(weather.sunsetTime()) + ", durée du jour "
                        + weather.daylightDuration(),
                "Carte de course du soleil.");
    }
}
