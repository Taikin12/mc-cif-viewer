package dev.taikin.mcplugins.cifviewer.mp;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class JsonParser {

    private final String text;
    private int position;

    private JsonParser(String text) {
        this.text = text;
    }

    static Object parse(String text) {
        JsonParser parser = new JsonParser(text);
        Object value = parser.value();
        parser.skipWhitespace();
        if (parser.position != text.length()) {
            throw parser.error("Unexpected trailing content");
        }
        return value;
    }

    private Object value() {
        skipWhitespace();
        if (position >= text.length()) {
            throw error("Unexpected end of JSON");
        }
        char current = text.charAt(position);
        return switch (current) {
            case '{' -> object();
            case '[' -> array();
            case '"' -> string();
            case 't' -> literal("true", Boolean.TRUE);
            case 'f' -> literal("false", Boolean.FALSE);
            case 'n' -> literal("null", null);
            default -> {
                if (current == '-' || Character.isDigit(current)) {
                    yield number();
                }
                throw error("Unexpected character '" + current + "'");
            }
        };
    }

    private Map<String, Object> object() {
        expect('{');
        Map<String, Object> result = new LinkedHashMap<>();
        skipWhitespace();
        if (consume('}')) {
            return result;
        }
        while (true) {
            skipWhitespace();
            String key = string();
            skipWhitespace();
            expect(':');
            result.put(key, value());
            skipWhitespace();
            if (consume('}')) {
                return result;
            }
            expect(',');
        }
    }

    private List<Object> array() {
        expect('[');
        List<Object> result = new ArrayList<>();
        skipWhitespace();
        if (consume(']')) {
            return result;
        }
        while (true) {
            result.add(value());
            skipWhitespace();
            if (consume(']')) {
                return result;
            }
            expect(',');
        }
    }

    private String string() {
        expect('"');
        StringBuilder result = new StringBuilder();
        while (position < text.length()) {
            char current = text.charAt(position++);
            if (current == '"') {
                return result.toString();
            }
            if (current == '\\') {
                if (position >= text.length()) {
                    throw error("Unterminated escape sequence");
                }
                char escaped = text.charAt(position++);
                switch (escaped) {
                    case '"' -> result.append('"');
                    case '\\' -> result.append('\\');
                    case '/' -> result.append('/');
                    case 'b' -> result.append('\b');
                    case 'f' -> result.append('\f');
                    case 'n' -> result.append('\n');
                    case 'r' -> result.append('\r');
                    case 't' -> result.append('\t');
                    case 'u' -> result.append(unicode());
                    default -> throw error("Invalid escape sequence \\" + escaped);
                }
            } else {
                result.append(current);
            }
        }
        throw error("Unterminated string");
    }

    private char unicode() {
        if (position + 4 > text.length()) {
            throw error("Invalid unicode escape");
        }
        String hex = text.substring(position, position + 4);
        position += 4;
        try {
            return (char) Integer.parseInt(hex, 16);
        } catch (NumberFormatException e) {
            throw error("Invalid unicode escape");
        }
    }

    private Double number() {
        int start = position;
        if (consume('-')) {
            if (position >= text.length()) {
                throw error("Invalid number");
            }
        }
        while (position < text.length() && Character.isDigit(text.charAt(position))) {
            position++;
        }
        if (position < text.length() && text.charAt(position) == '.') {
            position++;
            while (position < text.length() && Character.isDigit(text.charAt(position))) {
                position++;
            }
        }
        if (position < text.length() && (text.charAt(position) == 'e' || text.charAt(position) == 'E')) {
            position++;
            if (position < text.length() && (text.charAt(position) == '+' || text.charAt(position) == '-')) {
                position++;
            }
            while (position < text.length() && Character.isDigit(text.charAt(position))) {
                position++;
            }
        }
        try {
            return Double.parseDouble(text.substring(start, position));
        } catch (NumberFormatException e) {
            throw error("Invalid number");
        }
    }

    private Object literal(String literal, Object value) {
        if (!text.startsWith(literal, position)) {
            throw error("Invalid literal");
        }
        position += literal.length();
        return value;
    }

    private void expect(char expected) {
        skipWhitespace();
        if (position >= text.length() || text.charAt(position) != expected) {
            throw error("Expected '" + expected + "'");
        }
        position++;
    }

    private boolean consume(char expected) {
        if (position < text.length() && text.charAt(position) == expected) {
            position++;
            return true;
        }
        return false;
    }

    private void skipWhitespace() {
        while (position < text.length() && Character.isWhitespace(text.charAt(position))) {
            position++;
        }
    }

    private IllegalArgumentException error(String message) {
        return new IllegalArgumentException(message + " at position " + position);
    }
}
