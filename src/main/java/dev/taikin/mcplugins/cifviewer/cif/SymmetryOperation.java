package dev.taikin.mcplugins.cifviewer.cif;

import java.util.Locale;

final class SymmetryOperation {

    private final String source;
    private final String[] expressions;

    private SymmetryOperation(String source, String[] expressions) {
        this.source = source;
        this.expressions = expressions;
    }

    static SymmetryOperation parse(String raw) {
        String text = raw == null ? "" : raw.trim();
        if (text.isEmpty()) {
            throw new IllegalArgumentException("Empty symmetry operation.");
        }
        String[] parts = text.split(",", -1);
        if (parts.length != 3) {
            throw new IllegalArgumentException("Symmetry operation must have three parts: " + text);
        }
        String[] expressions = new String[3];
        for (int i = 0; i < 3; i++) {
            String expression = parts[i].replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
            if (expression.isEmpty()) {
                throw new IllegalArgumentException("Invalid symmetry operation: " + text);
            }
            expressions[i] = expression;
        }
        SymmetryOperation operation = new SymmetryOperation(text, expressions);
        operation.apply(0.1, 0.2, 0.3);
        return operation;
    }

    double[] apply(double x, double y, double z) {
        double[] result = new double[3];
        for (int i = 0; i < 3; i++) {
            result[i] = wrap(new Evaluator(expressions[i], x, y, z).evaluate());
        }
        return result;
    }

    String source() {
        return source;
    }

    private static double wrap(double value) {
        double wrapped = value - Math.floor(value);
        if (wrapped < 1.0e-9 || wrapped > 1.0 - 1.0e-9) {
            return 0.0;
        }
        return wrapped;
    }

    private static final class Evaluator {

        private final String text;
        private final double x;
        private final double y;
        private final double z;
        private int position;

        private Evaluator(String text, double x, double y, double z) {
            this.text = text;
            this.x = x;
            this.y = y;
            this.z = z;
        }

        private double evaluate() {
            double value = expression();
            skipWhitespace();
            if (position != text.length()) {
                throw new IllegalArgumentException("Invalid expression: " + text);
            }
            return value;
        }

        private double expression() {
            double value = term();
            while (true) {
                skipWhitespace();
                if (consume('+')) {
                    value += term();
                } else if (consume('-')) {
                    value -= term();
                } else {
                    return value;
                }
            }
        }

        private double term() {
            double value = factor();
            while (true) {
                skipWhitespace();
                if (consume('*')) {
                    value *= factor();
                } else if (consume('/')) {
                    double divisor = factor();
                    if (Math.abs(divisor) < 1.0e-12) {
                        throw new IllegalArgumentException("Division by zero in expression: " + text);
                    }
                    value /= divisor;
                } else if (startsFactor()) {
                    value *= factor();
                } else {
                    return value;
                }
            }
        }

        private double factor() {
            skipWhitespace();
            if (consume('+')) {
                return factor();
            }
            if (consume('-')) {
                return -factor();
            }
            if (consume('(')) {
                double value = expression();
                skipWhitespace();
                if (!consume(')')) {
                    throw new IllegalArgumentException("Missing parenthesis in expression: " + text);
                }
                return value;
            }
            if (position < text.length() && (Character.isDigit(text.charAt(position)) || text.charAt(position) == '.')) {
                return number();
            }
            if (position < text.length()) {
                char variable = text.charAt(position);
                if (variable == 'x' || variable == 'y' || variable == 'z') {
                    position++;
                    return switch (variable) {
                        case 'x' -> x;
                        case 'y' -> y;
                        default -> z;
                    };
                }
            }
            throw new IllegalArgumentException("Invalid expression: " + text);
        }

        private boolean startsFactor() {
            skipWhitespace();
            if (position >= text.length()) {
                return false;
            }
            char c = text.charAt(position);
            return Character.isDigit(c) || c == '.' || c == 'x' || c == 'y' || c == 'z' || c == '(';
        }

        private double number() {
            int start = position;
            while (position < text.length() && Character.isDigit(text.charAt(position))) {
                position++;
            }
            if (position < text.length() && text.charAt(position) == '.') {
                position++;
                while (position < text.length() && Character.isDigit(text.charAt(position))) {
                    position++;
                }
            }
            if (position < text.length() && (text.charAt(position) == 'e')) {
                position++;
                if (position < text.length() && (text.charAt(position) == '+' || text.charAt(position) == '-')) {
                    position++;
                }
                while (position < text.length() && Character.isDigit(text.charAt(position))) {
                    position++;
                }
            }
            if (start == position) {
                throw new IllegalArgumentException("Invalid number in expression: " + text);
            }
            return Double.parseDouble(text.substring(start, position));
        }

        private boolean consume(char expected) {
            skipWhitespace();
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
    }
}
