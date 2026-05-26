package fr.alescis.aelia.service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Minimal parser for the small subset of Nominatim JSON used by the map picker.
 */
final class NominatimJson {
    private static final Pattern DISPLAY_NAME = Pattern.compile("\\\"display_name\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\"\\\\])*)\\\"");

    private NominatimJson() {
    }

    static ReverseGeocodeResult parse(String json, double latitude, double longitude) {
        String displayName = first(DISPLAY_NAME, json);
        String city = firstAddress(json, "city", "town", "village", "municipality", "hamlet", "locality", "county", "state");
        if (city == null || city.isBlank()) {
            city = firstDisplayPart(displayName);
        }
        String country = firstAddress(json, "country");
        return new ReverseGeocodeResult(displayName, city, country, latitude, longitude);
    }

    private static String firstDisplayPart(String displayName) {
        if (displayName == null || displayName.isBlank()) {
            return null;
        }
        int separator = displayName.indexOf(',');
        return separator > 0 ? displayName.substring(0, separator).trim() : displayName.trim();
    }

    private static String firstAddress(String json, String... keys) {
        for (String key : keys) {
            Pattern pattern = Pattern.compile("\\\"" + Pattern.quote(key) + "\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\"\\\\])*)\\\"");
            String value = first(pattern, json);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private static String first(Pattern pattern, String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        Matcher matcher = pattern.matcher(json);
        if (!matcher.find()) {
            return null;
        }
        return unescapeJsonString(matcher.group(1));
    }

    private static String unescapeJsonString(String raw) {
        StringBuilder builder = new StringBuilder(raw.length());
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c != '\\' || i + 1 >= raw.length()) {
                builder.append(c);
                continue;
            }
            char escaped = raw.charAt(++i);
            switch (escaped) {
                case '"' -> builder.append('"');
                case '\\' -> builder.append('\\');
                case '/' -> builder.append('/');
                case 'b' -> builder.append('\b');
                case 'f' -> builder.append('\f');
                case 'n' -> builder.append('\n');
                case 'r' -> builder.append('\r');
                case 't' -> builder.append('\t');
                case 'u' -> {
                    if (i + 4 < raw.length()) {
                        String hex = raw.substring(i + 1, i + 5);
                        try {
                            builder.append((char) Integer.parseInt(hex, 16));
                            i += 4;
                        } catch (NumberFormatException exception) {
                            builder.append("\\u").append(hex);
                            i += 4;
                        }
                    } else {
                        builder.append("\\u");
                    }
                }
                default -> builder.append(escaped);
            }
        }
        return builder.toString();
    }
}
