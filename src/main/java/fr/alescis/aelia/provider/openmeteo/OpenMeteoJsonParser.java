package fr.alescis.aelia.provider.openmeteo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal RFC 8259 JSON parser used to avoid adding a dependency for small API payloads.
 */
public final class OpenMeteoJsonParser {
    private final String input;
    private int index;

    private OpenMeteoJsonParser(String input) {
        this.input = input == null ? "" : input;
    }

    public static OpenMeteoJsonValue parse(String input) {
        OpenMeteoJsonParser parser = new OpenMeteoJsonParser(input);
        OpenMeteoJsonValue value = parser.parseValue();
        parser.skipWhitespace();
        if (!parser.isEnd()) {
            throw parser.error("Unexpected trailing content");
        }
        return value;
    }

    private OpenMeteoJsonValue parseValue() {
        skipWhitespace();
        if (isEnd()) {
            throw error("Unexpected end of JSON input");
        }
        char current = input.charAt(index);
        return switch (current) {
            case '{' -> parseObject();
            case '[' -> parseArray();
            case '"' -> new OpenMeteoJsonValue(parseString());
            case 't' -> parseLiteral("true", Boolean.TRUE);
            case 'f' -> parseLiteral("false", Boolean.FALSE);
            case 'n' -> parseLiteral("null", null);
            default -> {
                if (current == '-' || Character.isDigit(current)) {
                    yield new OpenMeteoJsonValue(parseNumber());
                }
                throw error("Unexpected JSON token '" + current + "'");
            }
        };
    }

    private OpenMeteoJsonValue parseObject() {
        expect('{');
        Map<String, OpenMeteoJsonValue> object = new LinkedHashMap<>();
        skipWhitespace();
        if (peek('}')) {
            expect('}');
            return new OpenMeteoJsonValue(object);
        }
        while (true) {
            skipWhitespace();
            if (!peek('"')) {
                throw error("Expected object key");
            }
            String key = parseString();
            skipWhitespace();
            expect(':');
            object.put(key, parseValue());
            skipWhitespace();
            if (peek('}')) {
                expect('}');
                return new OpenMeteoJsonValue(object);
            }
            expect(',');
        }
    }

    private OpenMeteoJsonValue parseArray() {
        expect('[');
        List<OpenMeteoJsonValue> array = new ArrayList<>();
        skipWhitespace();
        if (peek(']')) {
            expect(']');
            return new OpenMeteoJsonValue(array);
        }
        while (true) {
            array.add(parseValue());
            skipWhitespace();
            if (peek(']')) {
                expect(']');
                return new OpenMeteoJsonValue(array);
            }
            expect(',');
        }
    }

    private String parseString() {
        expect('"');
        StringBuilder builder = new StringBuilder();
        while (!isEnd()) {
            char current = input.charAt(index++);
            if (current == '"') {
                return builder.toString();
            }
            if (current == '\\') {
                builder.append(parseEscape());
            } else {
                builder.append(current);
            }
        }
        throw error("Unterminated JSON string");
    }

    private char parseEscape() {
        if (isEnd()) {
            throw error("Unterminated JSON escape");
        }
        char escaped = input.charAt(index++);
        return switch (escaped) {
            case '"' -> '"';
            case '\\' -> '\\';
            case '/' -> '/';
            case 'b' -> '\b';
            case 'f' -> '\f';
            case 'n' -> '\n';
            case 'r' -> '\r';
            case 't' -> '\t';
            case 'u' -> parseUnicodeEscape();
            default -> throw error("Unsupported JSON escape: " + escaped);
        };
    }

    private char parseUnicodeEscape() {
        if (index + 4 > input.length()) {
            throw error("Incomplete unicode escape");
        }
        String hex = input.substring(index, index + 4);
        index += 4;
        try {
            return (char) Integer.parseInt(hex, 16);
        } catch (NumberFormatException exception) {
            throw new OpenMeteoException("Invalid unicode escape: " + hex, exception);
        }
    }

    private Number parseNumber() {
        int start = index;
        if (peek('-')) {
            index++;
        }
        consumeDigits();
        if (peek('.')) {
            index++;
            consumeDigits();
        }
        if (peek('e') || peek('E')) {
            index++;
            if (peek('+') || peek('-')) {
                index++;
            }
            consumeDigits();
        }
        String number = input.substring(start, index);
        try {
            return Double.valueOf(number);
        } catch (NumberFormatException exception) {
            throw new OpenMeteoException("Invalid JSON number: " + number, exception);
        }
    }

    private void consumeDigits() {
        int start = index;
        while (!isEnd() && Character.isDigit(input.charAt(index))) {
            index++;
        }
        if (start == index) {
            throw error("Expected digit");
        }
    }

    private OpenMeteoJsonValue parseLiteral(String literal, Object value) {
        if (!input.startsWith(literal, index)) {
            throw error("Expected literal " + literal);
        }
        index += literal.length();
        return new OpenMeteoJsonValue(value);
    }

    private void expect(char expected) {
        skipWhitespace();
        if (isEnd() || input.charAt(index) != expected) {
            throw error("Expected '" + expected + "'");
        }
        index++;
    }

    private boolean peek(char expected) {
        return !isEnd() && input.charAt(index) == expected;
    }

    private void skipWhitespace() {
        while (!isEnd() && Character.isWhitespace(input.charAt(index))) {
            index++;
        }
    }

    private boolean isEnd() {
        return index >= input.length();
    }

    private OpenMeteoException error(String message) {
        return new OpenMeteoException(message + " at character " + index + ".");
    }
}
