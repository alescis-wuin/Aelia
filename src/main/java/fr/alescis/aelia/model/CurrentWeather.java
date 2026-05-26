package fr.alescis.aelia.model;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.Objects;

/**
 * Current weather and environmental indicators for the selected location.
 *
 * <p>The class keeps the original mockup-facing string accessors while storing
 * typed temporal values internally. This preserves compatibility with older
 * dashboard code and still gives future remote API providers a clean typed
 * target for dates, times and time zones.</p>
 */
public final class CurrentWeather {
    private static final Locale FRENCH = Locale.FRANCE;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm", FRENCH);
    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Europe/Paris");

    private final String city;
    private final String conditionLabel;
    private final LocalDate date;
    private final ZoneId zoneId;
    private final int temperatureCelsius;
    private final int maximumTemperatureCelsius;
    private final int minimumTemperatureCelsius;
    private final int apparentTemperatureCelsius;
    private final LocalTime sunriseTime;
    private final LocalTime sunsetTime;
    private final int humidityPercent;
    private final int windSpeedKmh;
    private final String windDirection;
    private final int windGustKmh;
    private final int pressureHpa;
    private final String pressureTrend;
    private final int uvIndex;
    private final String uvAdvice;
    private final AirQuality airQuality;
    private final LocalTime currentSolarTime;

    public CurrentWeather(
            String city,
            String conditionLabel,
            LocalDate date,
            ZoneId zoneId,
            int temperatureCelsius,
            int maximumTemperatureCelsius,
            int minimumTemperatureCelsius,
            int apparentTemperatureCelsius,
            LocalTime sunriseTime,
            LocalTime sunsetTime,
            int humidityPercent,
            int windSpeedKmh,
            String windDirection,
            int windGustKmh,
            int pressureHpa,
            String pressureTrend,
            int uvIndex,
            String uvAdvice,
            AirQuality airQuality,
            LocalTime currentSolarTime
    ) {
        this.city = requireText(city, "City is required.");
        this.conditionLabel = requireText(conditionLabel, "Condition label is required.");
        this.date = Objects.requireNonNull(date, "date");
        this.zoneId = Objects.requireNonNull(zoneId, "zoneId");
        this.sunriseTime = Objects.requireNonNull(sunriseTime, "sunriseTime");
        this.sunsetTime = Objects.requireNonNull(sunsetTime, "sunsetTime");
        this.currentSolarTime = Objects.requireNonNull(currentSolarTime, "currentSolarTime");
        this.temperatureCelsius = temperatureCelsius;
        this.maximumTemperatureCelsius = maximumTemperatureCelsius;
        this.minimumTemperatureCelsius = minimumTemperatureCelsius;
        this.apparentTemperatureCelsius = apparentTemperatureCelsius;
        this.humidityPercent = requireRange(humidityPercent, 0, 100, "Humidity must be between 0 and 100.");
        this.windSpeedKmh = requireNonNegative(windSpeedKmh, "Wind speed cannot be negative.");
        this.windDirection = windDirection == null || windDirection.isBlank() ? "N/A" : windDirection;
        this.windGustKmh = requireNonNegative(windGustKmh, "Wind gust cannot be negative.");
        this.pressureHpa = pressureHpa;
        this.pressureTrend = pressureTrend == null || pressureTrend.isBlank() ? "Stable" : pressureTrend;
        this.uvIndex = requireNonNegative(uvIndex, "UV index cannot be negative.");
        this.uvAdvice = uvAdvice == null || uvAdvice.isBlank() ? "Aucune recommandation spécifique." : uvAdvice;
        this.airQuality = Objects.requireNonNull(airQuality, "airQuality");
    }

    /**
     * Compatibility constructor matching the original mockup-oriented model.
     */
    public CurrentWeather(
            String city,
            String conditionLabel,
            String dateLabel,
            int temperatureCelsius,
            int maximumTemperatureCelsius,
            int minimumTemperatureCelsius,
            int apparentTemperatureCelsius,
            String sunrise,
            String sunset,
            int humidityPercent,
            int windSpeedKmh,
            String windDirection,
            int windGustKmh,
            int pressureHpa,
            String pressureTrend,
            int uvIndex,
            String uvAdvice,
            AirQuality airQuality,
            String daylightDuration,
            String currentSolarTime
    ) {
        this(
                city,
                conditionLabel,
                parseDateLabel(dateLabel),
                DEFAULT_ZONE,
                temperatureCelsius,
                maximumTemperatureCelsius,
                minimumTemperatureCelsius,
                apparentTemperatureCelsius,
                parseTime(sunrise, "sunrise"),
                parseTime(sunset, "sunset"),
                humidityPercent,
                windSpeedKmh,
                windDirection,
                windGustKmh,
                pressureHpa,
                pressureTrend,
                uvIndex,
                uvAdvice,
                airQuality,
                parseTime(currentSolarTime, "currentSolarTime")
        );
    }

    public String city() {
        return city;
    }

    public String conditionLabel() {
        return conditionLabel;
    }

    public LocalDate date() {
        return date;
    }

    public ZoneId zoneId() {
        return zoneId;
    }

    public String dateLabel() {
        String day = date.getDayOfWeek().getDisplayName(TextStyle.SHORT, FRENCH);
        String month = date.getMonth().getDisplayName(TextStyle.SHORT, FRENCH);
        return capitalize(day) + " " + date.getDayOfMonth() + " " + month + " " + date.getYear();
    }

    public int temperatureCelsius() {
        return temperatureCelsius;
    }

    public int maximumTemperatureCelsius() {
        return maximumTemperatureCelsius;
    }

    public int minimumTemperatureCelsius() {
        return minimumTemperatureCelsius;
    }

    public int apparentTemperatureCelsius() {
        return apparentTemperatureCelsius;
    }

    public LocalTime sunriseTime() {
        return sunriseTime;
    }

    public String sunrise() {
        return TIME_FORMATTER.format(sunriseTime);
    }

    public LocalTime sunsetTime() {
        return sunsetTime;
    }

    public String sunset() {
        return TIME_FORMATTER.format(sunsetTime);
    }

    public int humidityPercent() {
        return humidityPercent;
    }

    public int windSpeedKmh() {
        return windSpeedKmh;
    }

    public String windDirection() {
        return windDirection;
    }

    public int windGustKmh() {
        return windGustKmh;
    }

    public int pressureHpa() {
        return pressureHpa;
    }

    public String pressureTrend() {
        return pressureTrend;
    }

    public int uvIndex() {
        return uvIndex;
    }

    public String uvAdvice() {
        return uvAdvice;
    }

    public AirQuality airQuality() {
        return airQuality;
    }

    public LocalTime currentSolarLocalTime() {
        return currentSolarTime;
    }

    public String currentSolarTime() {
        return "~" + TIME_FORMATTER.format(currentSolarTime).replace(":", "h");
    }

    public ZonedDateTime sunriseDateTime() {
        return ZonedDateTime.of(date, sunriseTime, zoneId);
    }

    public ZonedDateTime sunsetDateTime() {
        ZonedDateTime sunrise = sunriseDateTime();
        ZonedDateTime sunset = ZonedDateTime.of(date, sunsetTime, zoneId);
        return sunset.isBefore(sunrise) ? sunset.plusDays(1) : sunset;
    }

    public ZonedDateTime currentSolarDateTime() {
        ZonedDateTime sunrise = sunriseDateTime();
        ZonedDateTime current = ZonedDateTime.of(date, currentSolarTime, zoneId);
        return current.isBefore(sunrise) && sunsetTime.isBefore(sunriseTime) ? current.plusDays(1) : current;
    }

    public Duration daylightDurationValue() {
        return Duration.between(sunriseDateTime(), sunsetDateTime());
    }

    public String daylightDuration() {
        long minutes = Math.max(0, daylightDurationValue().toMinutes());
        long hours = minutes / 60;
        long remainingMinutes = minutes % 60;
        return hours + "h " + String.format(FRENCH, "%02d", remainingMinutes) + " min de jour";
    }

    public double daylightProgress() {
        Duration daylight = daylightDurationValue();
        if (daylight.isZero() || daylight.isNegative()) {
            return 0.0;
        }
        Duration elapsed = Duration.between(sunriseDateTime(), currentSolarDateTime());
        double progress = (double) elapsed.toSeconds() / (double) daylight.toSeconds();
        return Math.max(0.0, Math.min(1.0, progress));
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private static int requireRange(int value, int minimum, int maximum, String message) {
        if (value < minimum || value > maximum) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private static int requireNonNegative(int value, String message) {
        if (value < 0) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private static LocalDate parseDateLabel(String label) {
        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException("Date label is required.");
        }
        String normalized = label.trim().replace("Dim.", "dim.").replace("Lun.", "lun.")
                .replace("Mar.", "mar.").replace("Mer.", "mer.").replace("Jeu.", "jeu.")
                .replace("Ven.", "ven.").replace("Sam.", "sam.");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE d MMM uuuu", FRENCH);
        try {
            return LocalDate.parse(normalized, formatter);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("Unsupported date label: " + label, exception);
        }
    }

    private static LocalTime parseTime(String label, String fieldName) {
        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        String normalized = label.trim().replace("~", "").replace("h", ":");
        if (normalized.endsWith(":")) {
            normalized += "00";
        }
        return LocalTime.parse(normalized, TIME_FORMATTER);
    }

    private static String capitalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.substring(0, 1).toUpperCase(FRENCH) + value.substring(1);
    }
}
