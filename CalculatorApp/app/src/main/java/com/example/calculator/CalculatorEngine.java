package com.example.calculator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;

/**
 * Pure-Java calculator brain. Holds the expression the user is typing,
 * evaluates it with correct operator precedence and exposes display strings.
 *
 * <p>Internal operators are ASCII: '+', '-', '*', '/'. Pretty glyphs
 * ('×', '÷', '−') are produced only for display via {@link #getDisplayExpression()}.</p>
 *
 * <p>The class is deliberately free of Android dependencies so it can be
 * unit-tested on the JVM.</p>
 */
public class CalculatorEngine {

    /** Maximum number of digits a single number entry may contain. */
    private static final int MAX_DIGITS = 15;
    /** Maximum characters of the whole expression, as a safety bound. */
    private static final int MAX_EXPRESSION_LENGTH = 100;
    /** Scale used when rounding results to strip binary float noise. */
    private static final int RESULT_SCALE = 12;

    /** The expression as typed, using ASCII operators, e.g. "12+3*4". */
    private final StringBuilder expression = new StringBuilder();
    /** True right after '=' was pressed and the result is showing. */
    private boolean justEvaluated = false;
    /** True when a math error (e.g. division by zero) is being displayed. */
    private boolean errorState = false;

    // ------------------------------------------------------------------
    // Input handling
    // ------------------------------------------------------------------

    public void inputDigit(int digit) {
        if (digit < 0 || digit > 9) {
            throw new IllegalArgumentException("digit must be 0..9");
        }
        if (errorState) {
            clear();
        }
        if (justEvaluated) {
            // Start a brand new calculation after a result.
            expression.setLength(0);
            justEvaluated = false;
        }
        if (expression.length() >= MAX_EXPRESSION_LENGTH) {
            return;
        }
        String current = currentNumber();
        if (countDigits(current) >= MAX_DIGITS) {
            return;
        }
        // Avoid "00" / "-00": replace a lone leading zero instead of appending.
        if (current.equals("0") || current.equals("-0")) {
            expression.setCharAt(expression.length() - 1, (char) ('0' + digit));
            return;
        }
        expression.append((char) ('0' + digit));
    }

    public void inputDot() {
        if (errorState) {
            clear();
        }
        if (justEvaluated) {
            expression.setLength(0);
            expression.append('0');
            justEvaluated = false;
        }
        String current = currentNumber();
        if (current.indexOf('.') >= 0) {
            return; // already has a dot
        }
        if (current.isEmpty()) {
            // Dot pressed at start or right after an operator: begin "0."
            if (expression.length() == 0 || isOperator(lastChar())) {
                expression.append('0');
            } else {
                return;
            }
        }
        expression.append('.');
    }

    public void inputOperator(char op) {
        if (!isOperator(op)) {
            throw new IllegalArgumentException("unsupported operator: " + op);
        }
        if (errorState) {
            return;
        }
        if (justEvaluated) {
            // Continue calculating from the previous result.
            justEvaluated = false;
        }
        if (expression.length() == 0) {
            // Only '-' may start an expression (negative number).
            if (op == '-') {
                expression.append(op);
            }
            return;
        }
        if (expression.length() >= MAX_EXPRESSION_LENGTH) {
            return;
        }
        char last = lastChar();
        if (last == '.') {
            expression.deleteCharAt(expression.length() - 1);
            last = lastChar();
        }
        if (isOperator(last)) {
            if (op == '-' && last != '-' && !endsWithUnaryMinus()) {
                // Allow "5*-" => negative factor.
                expression.append(op);
                return;
            }
            if (endsWithUnaryMinus() && expression.length() > 1) {
                // Tail like "5*-": drop the unary minus, replace the binary operator.
                expression.deleteCharAt(expression.length() - 1);
                expression.setCharAt(expression.length() - 1, op);
                return;
            }
            if (expression.length() == 1) {
                // Expression is only "-": another '-' keeps it, anything else clears it.
                if (op != '-') {
                    expression.setLength(0);
                }
                return;
            }
            // Simple replacement, e.g. "5+" then '*' => "5*".
            expression.setCharAt(expression.length() - 1, op);
            return;
        }
        expression.append(op);
    }


    /** Applies percent to the number currently being entered (50 -> 0.5). */
    public void inputPercent() {
        if (errorState) {
            return;
        }
        justEvaluated = false;
        String current = currentNumber();
        if (current.isEmpty() || current.equals("-")) {
            return;
        }
        double value = parseNumber(current) / 100.0;
        replaceCurrentNumber(formatNumber(value));
    }

    /** Flips the sign of the number currently being entered. */
    public void toggleSign() {
        if (errorState) {
            return;
        }
        justEvaluated = false;
        String current = currentNumber();
        if (current.isEmpty()) {
            // No number yet: start a negative one.
            if (expression.length() == 0 || isOperator(lastChar())) {
                expression.append('-');
            }
            return;
        }
        if (current.equals("-")) {
            expression.deleteCharAt(expression.length() - 1);
            return;
        }
        int start = expression.length() - current.length();
        if (current.startsWith("-")) {
            expression.deleteCharAt(start);
        } else {
            expression.insert(start, '-');
        }
    }

    public void backspace() {
        if (errorState) {
            clear();
            return;
        }
        if (justEvaluated) {
            justEvaluated = false;
        }
        if (expression.length() > 0) {
            expression.deleteCharAt(expression.length() - 1);
        }
    }

    public void clear() {
        expression.setLength(0);
        justEvaluated = false;
        errorState = false;
    }

    // ------------------------------------------------------------------
    // Evaluation
    // ------------------------------------------------------------------

    /** Evaluates the current expression. On success the result becomes the new input. */
    public void evaluate() {
        if (errorState || justEvaluated) {
            return;
        }
        String sanitized = sanitize(expression.toString());
        if (sanitized.isEmpty() || sanitized.equals("-")) {
            return;
        }
        Double result = tryEvaluate(sanitized);
        if (result == null) {
            errorState = true;
            return;
        }
        expression.setLength(0);
        expression.append(formatNumber(result));
        justEvaluated = true;
    }

    /**
     * Live preview of the result shown while typing.
     *
     * @return formatted result, or {@code null} when the expression is not
     *         yet evaluable or is invalid.
     */
    public String getPreview() {
        if (errorState || justEvaluated) {
            return null;
        }
        String sanitized = sanitize(expression.toString());
        if (sanitized.isEmpty() || sanitized.equals("-") || isPlainNumber(sanitized)) {
            return null;
        }
        Double result = tryEvaluate(sanitized);
        if (result == null) {
            return null;
        }
        return formatNumber(result);
    }

    /** The expression to show on screen, with pretty operator glyphs. */
    public String getDisplayExpression() {
        StringBuilder pretty = new StringBuilder(expression.length());
        for (int i = 0; i < expression.length(); i++) {
            pretty.append(prettyChar(expression.charAt(i)));
        }
        return pretty.toString();
    }

    /** True when the last evaluation failed (division by zero, overflow...). */
    public boolean isError() {
        return errorState;
    }

    /** True when a finished result is currently displayed. */
    public boolean isShowingResult() {
        return justEvaluated;
    }

    // ------------------------------------------------------------------
    // Expression helpers
    // ------------------------------------------------------------------

    private char lastChar() {
        return expression.charAt(expression.length() - 1);
    }

    private static boolean isOperator(char c) {
        return c == '+' || c == '-' || c == '*' || c == '/';
    }

    private static char prettyChar(char c) {
        switch (c) {
            case '*':
                return '×';
            case '/':
                return '÷';
            case '-':
                return '−';
            default:
                return c;
        }
    }

    /** Returns the number (with optional unary '-') currently being typed. */
    private String currentNumber() {
        int end = expression.length();
        int start = end;
        while (start > 0) {
            char c = expression.charAt(start - 1);
            if (Character.isDigit(c) || c == '.') {
                start--;
            } else {
                // Keep '-' only when it is a unary sign (at start or after an operator).
                if (c == '-' && (start - 1 == 0 || isOperator(expression.charAt(start - 2)))) {
                    start--;
                }
                break;
            }
        }
        return expression.substring(start, end);
    }

    private void replaceCurrentNumber(String replacement) {
        String current = currentNumber();
        expression.delete(expression.length() - current.length(), expression.length());
        expression.append(replacement);
    }

    private static int countDigits(String s) {
        int count = 0;
        for (int i = 0; i < s.length(); i++) {
            if (Character.isDigit(s.charAt(i))) {
                count++;
            }
        }
        return count;
    }

    private boolean endsWithUnaryMinus() {
        int len = expression.length();
        if (len == 0 || expression.charAt(len - 1) != '-') {
            return false;
        }
        return len == 1 || isOperator(expression.charAt(len - 2));
    }

    /** Drops trailing operators/dots so evaluation never sees a dangling token. */
    private static String sanitize(String expr) {
        int end = expr.length();
        while (end > 0) {
            char c = expr.charAt(end - 1);
            if (isOperator(c) || c == '.') {
                end--;
            } else {
                break;
            }
        }
        return expr.substring(0, end);
    }

    private static boolean isPlainNumber(String s) {
        int start = s.startsWith("-") ? 1 : 0;
        if (start == s.length()) {
            return false;
        }
        for (int i = start; i < s.length(); i++) {
            char c = s.charAt(i);
            if (!Character.isDigit(c) && c != '.') {
                return false;
            }
        }
        return true;
    }

    // ------------------------------------------------------------------
    // Shunting-yard evaluation
    // ------------------------------------------------------------------

    /** @return the value of the expression, or {@code null} on any math error. */
    static Double tryEvaluate(String expr) {
        List<String> tokens = tokenize(expr);
        if (tokens == null || tokens.isEmpty()) {
            return null;
        }
        List<String> rpn = toRpn(tokens);
        return evalRpn(rpn);
    }

    private static List<String> tokenize(String expr) {
        List<String> tokens = new ArrayList<>();
        int i = 0;
        boolean expectNumber = true;
        while (i < expr.length()) {
            char c = expr.charAt(i);
            if (expectNumber) {
                if (c == '-') {
                    // Unary minus: fold the sign into the number token.
                    StringBuilder num = new StringBuilder("-");
                    int j = i + 1;
                    boolean seenDigit = false;
                    while (j < expr.length()
                            && (Character.isDigit(expr.charAt(j)) || expr.charAt(j) == '.')) {
                        if (Character.isDigit(expr.charAt(j))) {
                            seenDigit = true;
                        }
                        num.append(expr.charAt(j));
                        j++;
                    }
                    if (!seenDigit) {
                        return null;
                    }
                    tokens.add(num.toString());
                    i = j;
                } else if (Character.isDigit(c) || c == '.') {
                    StringBuilder num = new StringBuilder();
                    int j = i;
                    while (j < expr.length()
                            && (Character.isDigit(expr.charAt(j)) || expr.charAt(j) == '.')) {
                        num.append(expr.charAt(j));
                        j++;
                    }
                    tokens.add(num.toString());
                    i = j;
                } else {
                    return null;
                }
                expectNumber = false;
            } else {
                if (!isOperator(c)) {
                    return null;
                }
                tokens.add(String.valueOf(c));
                i++;
                expectNumber = true;
            }
        }
        return tokens;
    }

    private static int precedence(String op) {
        switch (op.charAt(0)) {
            case '+':
            case '-':
                return 1;
            case '*':
            case '/':
                return 2;
            default:
                return 0;
        }
    }

    private static boolean isOpToken(String token) {
        return token.length() == 1 && isOperator(token.charAt(0));
    }

    private static List<String> toRpn(List<String> tokens) {
        List<String> output = new ArrayList<>();
        Deque<String> ops = new ArrayDeque<>();
        for (String token : tokens) {
            if (isOpToken(token)) {
                while (!ops.isEmpty() && precedence(ops.peek()) >= precedence(token)) {
                    output.add(ops.pop());
                }
                ops.push(token);
            } else {
                output.add(token);
            }
        }
        while (!ops.isEmpty()) {
            output.add(ops.pop());
        }
        return output;
    }

    private static Double evalRpn(List<String> rpn) {
        Deque<Double> stack = new ArrayDeque<>();
        for (String token : rpn) {
            if (isOpToken(token)) {
                if (stack.size() < 2) {
                    return null;
                }
                double b = stack.pop();
                double a = stack.pop();
                double value;
                switch (token.charAt(0)) {
                    case '+':
                        value = a + b;
                        break;
                    case '-':
                        value = a - b;
                        break;
                    case '*':
                        value = a * b;
                        break;
                    case '/':
                        if (b == 0.0) {
                            return null;
                        }
                        value = a / b;
                        break;
                    default:
                        return null;
                }
                if (Double.isInfinite(value) || Double.isNaN(value)) {
                    return null;
                }
                stack.push(value);
            } else {
                try {
                    stack.push(Double.parseDouble(token));
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        }
        return stack.size() == 1 ? stack.pop() : null;
    }

    // ------------------------------------------------------------------
    // Number formatting
    // ------------------------------------------------------------------

    static double parseNumber(String s) {
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }

    /**
     * Formats a result: trims binary-float noise, drops a trailing ".0",
     * and falls back to scientific notation for very large/small magnitudes.
     */
    static String formatNumber(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return "Error";
        }
        if (value == 0.0) {
            return "0";
        }
        double abs = Math.abs(value);
        if (abs >= 1e15 || abs < 1e-9) {
            return scientific(value);
        }
        BigDecimal bd = BigDecimal.valueOf(value)
                .setScale(RESULT_SCALE, RoundingMode.HALF_UP)
                .stripTrailingZeros();
        String plain = bd.toPlainString();
        // Extremely long integer parts still get scientific notation.
        if (plain.indexOf('.') < 0 && plain.replace("-", "").length() > MAX_DIGITS) {
            return scientific(value);
        }
        return plain;
    }

    private static String scientific(double value) {
        // e.g. 1.2345678E12 with up to 8 significant decimals, trailing zeros trimmed.
        String s = String.format(Locale.US, "%.8E", value);
        int eIndex = s.indexOf('E');
        String mantissa = s.substring(0, eIndex);
        String exponent = s.substring(eIndex + 1);
        while (mantissa.contains(".") && mantissa.endsWith("0")) {
            mantissa = mantissa.substring(0, mantissa.length() - 1);
        }
        if (mantissa.endsWith(".")) {
            mantissa = mantissa.substring(0, mantissa.length() - 1);
        }
        return mantissa + "E" + exponent.replace("+", "");
    }

    // ------------------------------------------------------------------
    // State persistence (e.g. across configuration changes)
    // ------------------------------------------------------------------

    /** Raw internal expression (ASCII operators), for saving instance state. */
    public String getRawExpression() {
        return expression.toString();
    }

    /**
     * Restores a previously saved state.
     *
     * @param raw       value from {@link #getRawExpression()}
     * @param evaluated value from {@link #isShowingResult()}
     * @param error     value from {@link #isError()}
     */
    public void restoreState(String raw, boolean evaluated, boolean error) {
        expression.setLength(0);
        if (raw != null) {
            // Defensive: only restore syntactically plausible characters.
            for (int i = 0; i < raw.length(); i++) {
                char c = raw.charAt(i);
                if (Character.isDigit(c) || c == '.' || isOperator(c)) {
                    expression.append(c);
                }
            }
        }
        this.justEvaluated = evaluated;
        this.errorState = error;
    }
}
