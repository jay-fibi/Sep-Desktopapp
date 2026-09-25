package com.notecalc.app.engine;

import java.text.DecimalFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Intelligent Note & Calculation Parser and Evaluator.
 */
public class NoteCalcEngine {

    private static final DecimalFormat NUMBER_FORMAT = new DecimalFormat("#,##0.######");

    public static class EvaluationResult {
        private final List<LineResult> lineResults;
        private final double grandTotal;
        private final Map<String, Double> finalVariables;

        public EvaluationResult(List<LineResult> lineResults, double grandTotal, Map<String, Double> finalVariables) {
            this.lineResults = lineResults;
            this.grandTotal = grandTotal;
            this.finalVariables = finalVariables;
        }

        public List<LineResult> getLineResults() {
            return lineResults;
        }

        public double getGrandTotal() {
            return grandTotal;
        }

        public String getFormattedGrandTotal() {
            return formatNumber(grandTotal);
        }

        public Map<String, Double> getFinalVariables() {
            return finalVariables;
        }
    }

    public EvaluationResult evaluate(String documentText) {
        if (documentText == null || documentText.isEmpty()) {
            return new EvaluationResult(Collections.emptyList(), 0.0, Collections.emptyMap());
        }

        String[] rawLines = documentText.split("\r?\n", -1);
        List<LineResult> results = new ArrayList<>();
        Map<String, Double> variables = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        List<Double> previousValues = new ArrayList<>();
        List<Double> sectionValues = new ArrayList<>();
        double grandTotal = 0.0;

        for (int i = 0; i < rawLines.length; i++) {
            String rawLine = rawLines[i];
            String trimmed = rawLine.trim();

            if (trimmed.isEmpty()) {
                sectionValues.clear();
                results.add(new LineResult(i, rawLine, null, null, "", false, null, false));
                continue;
            }

            if (trimmed.startsWith("#") || trimmed.startsWith("//") || trimmed.startsWith("/*")) {
                sectionValues.clear();
                results.add(new LineResult(i, rawLine, null, null, "", false, null, true));
                continue;
            }

            String lower = trimmed.toLowerCase();
            if (isSpecialAggregateCommand(lower)) {
                LineResult aggResult = handleAggregate(i, rawLine, lower, previousValues, sectionValues);
                if (aggResult.getNumericValue() != null) {
                    previousValues.add(aggResult.getNumericValue());
                    sectionValues.add(aggResult.getNumericValue());
                    variables.put("total", aggResult.getNumericValue());
                    variables.put("last", aggResult.getNumericValue());
                }
                results.add(aggResult);
                continue;
            }

            String varName = null;
            String exprText = trimmed;

            Pattern assignPattern = Pattern.compile("^([a-zA-Z_][a-zA-Z0-9_]*)\\s*[:=]\\s*(.*)$");
            Matcher assignMatcher = assignPattern.matcher(trimmed);
            if (assignMatcher.matches()) {
                String candidateVar = assignMatcher.group(1).trim();
                String rightSide = assignMatcher.group(2).trim();
                if (!rightSide.isEmpty()) {
                    varName = candidateVar;
                    exprText = rightSide;
                }
            } else {
                exprText = extractMathExpression(trimmed);
            }

            if (exprText == null || exprText.isEmpty() || !containsMathTokens(exprText)) {
                results.add(new LineResult(i, rawLine, null, null, "", false, null, false));
                continue;
            }

            try {
                double val = evaluateExpression(exprText, variables, previousValues);
                if (Double.isNaN(val) || Double.isInfinite(val)) {
                    results.add(new LineResult(i, rawLine, varName, null, "Error: Div by zero", true, "Division by zero", false));
                } else {
                    String formatted = formatNumber(val);
                    if (varName != null) {
                        variables.put(varName, val);
                    }
                    variables.put("last", val);
                    previousValues.add(val);
                    sectionValues.add(val);
                    grandTotal += val;
                    results.add(new LineResult(i, rawLine, varName, val, formatted, false, null, false));
                }
            } catch (Exception e) {
                if (varName == null && !isLikelyStrictMath(exprText)) {
                    results.add(new LineResult(i, rawLine, null, null, "", false, null, false));
                } else {
                    results.add(new LineResult(i, rawLine, varName, null, "Error", true, e.getMessage(), false));
                }
            }
        }

        return new EvaluationResult(results, grandTotal, variables);
    }

    private static boolean isSpecialAggregateCommand(String lower) {
        String stripped = lower.replaceAll("[:=]", "").trim();
        return stripped.equals("total") || stripped.equals("sum") || stripped.equals("subtotal")
                || stripped.equals("avg") || stripped.equals("average") || stripped.equals("count");
    }

    private LineResult handleAggregate(int lineNum, String rawLine, String lower,
                                       List<Double> previousValues, List<Double> sectionValues) {
        String stripped = lower.replaceAll("[:=]", "").trim();
        List<Double> targetList = (!sectionValues.isEmpty()) ? sectionValues : previousValues;

        if (targetList.isEmpty()) {
            return new LineResult(lineNum, rawLine, stripped, 0.0, "0", false, null, false);
        }

        double result = 0.0;
        if (stripped.equals("total") || stripped.equals("sum") || stripped.equals("subtotal")) {
            for (double d : targetList) {
                result += d;
            }
        } else if (stripped.equals("avg") || stripped.equals("average")) {
            for (double d : targetList) {
                result += d;
            }
            result = result / targetList.size();
        } else if (stripped.equals("count")) {
            result = targetList.size();
        }

        return new LineResult(lineNum, rawLine, stripped, result, formatNumber(result), false, null, false);
    }

    private static boolean containsMathTokens(String text) {
        return Pattern.compile("\\d|\\+|\\-|\\*|/|%|\\^").matcher(text).find();
    }

    private static boolean isLikelyStrictMath(String expr) {
        return Pattern.compile("[\\+\\-\\*/\\^%=]").matcher(expr).find()
                || Pattern.compile("^(sqrt|abs|sin|cos|tan|log|round|min|max)\\b").matcher(expr).find();
    }

    public static String extractMathExpression(String text) {
        String clean = text.replaceAll("[$€£¥₹]", " ").trim();

        if (clean.startsWith("(") || (clean.length() > 0 && Character.isDigit(clean.charAt(0)))) {
            return clean;
        }

        int colonIdx = clean.indexOf(":");
        if (colonIdx >= 0 && colonIdx < clean.length() - 1) {
            String after = clean.substring(colonIdx + 1).trim();
            if (containsMathTokens(after)) {
                return after;
            }
        }

        int dashIdx = clean.lastIndexOf(" - ");
        if (dashIdx > 0 && dashIdx < clean.length() - 3) {
            String before = clean.substring(0, dashIdx).trim();
            String after = clean.substring(dashIdx + 3).trim();
            if (!containsMathTokens(before) && containsMathTokens(after)) {
                return after;
            }
        }

        Pattern startMathPattern = Pattern.compile("(?:^|\\s)(\\(|[-+]?\\d+(?:\\.\\d+)?|sqrt|abs|sin|cos|tan|log|round|min|max|pi|e|last|total|sum)\\b", Pattern.CASE_INSENSITIVE);
        Matcher m = startMathPattern.matcher(clean);
        if (m.find()) {
            int start = m.start(1);
            String candidate = clean.substring(start).trim();
            candidate = candidate.replaceAll("(?i)\\b(usd|eur|gbp|inr|km|kg|lbs|g|m|cm|hrs|hr|hours|days|weeks|months|years|pcs|items|units)\\b", "");
            return candidate.trim();
        }

        return clean;
    }

    public static double evaluateExpression(String expr, Map<String, Double> variables, List<Double> previousValues) {
        String prepared = preprocessExpression(expr, variables, previousValues);
        return new ExpressionParser(prepared, variables).parse();
    }

    private static String preprocessExpression(String expr, Map<String, Double> variables, List<Double> previousValues) {
        String s = expr.trim();
        s = s.replaceAll("[$€£¥₹]", "");
        s = s.replaceAll("(?<=\\d)\\s*[xX]\\s*(?=\\d)", " * ");
        s = s.replaceAll("(?i)%\\s+(?:of|on)\\s+", "% * ");
        s = s.replace("**", "^");

        Pattern addSubPercent = Pattern.compile("([a-zA-Z0-9_.]+|\\([^)]+\\))\\s*([+-])\\s*([0-9.]+)\\s*%");
        Matcher m = addSubPercent.matcher(s);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String base = m.group(1);
            String op = m.group(2);
            String pct = m.group(3);
            String replacement = base + " " + op + " (" + base + " * (" + pct + " / 100))";
            m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        m.appendTail(sb);
        s = sb.toString();

        s = s.replaceAll("([0-9.]+)\\s*%", "($1 / 100.0)");
        return s;
    }

    public static String formatNumber(double value) {
        if (Double.isNaN(value)) return "NaN";
        if (Double.isInfinite(value)) return value > 0 ? "Infinity" : "-Infinity";

        if (Math.abs(value - Math.round(value)) < 1e-9) {
            long longVal = Math.round(value);
            return String.format(Locale.US, "%,d", longVal);
        }

        return NUMBER_FORMAT.format(value);
    }

    private static class ExpressionParser {
        private final String str;
        private final Map<String, Double> variables;
        private int pos = -1;
        private int ch;

        public ExpressionParser(String str, Map<String, Double> variables) {
            this.str = str;
            this.variables = variables != null ? variables : Collections.emptyMap();
        }

        private void nextChar() {
            ch = (++pos < str.length()) ? str.charAt(pos) : -1;
        }

        private boolean eat(int charToEat) {
            while (ch == ' ' || ch == '\t') nextChar();
            if (ch == charToEat) {
                nextChar();
                return true;
            }
            return false;
        }

        public double parse() {
            nextChar();
            double x = parseExpression();
            while (ch == ' ' || ch == '\t') nextChar();
            if (pos < str.length()) {
                throw new RuntimeException("Unexpected character: " + (char) ch);
            }
            return x;
        }

        private double parseExpression() {
            double x = parseTerm();
            for (;;) {
                if (eat('+')) x += parseTerm();
                else if (eat('-')) x -= parseTerm();
                else return x;
            }
        }

        private double parseTerm() {
            double x = parseFactor();
            for (;;) {
                if (eat('*')) {
                    x *= parseFactor();
                } else if (eat('/')) {
                    double divisor = parseFactor();
                    if (divisor == 0) throw new ArithmeticException("Division by zero");
                    x /= divisor;
                } else if (eat('%')) {
                    double mod = parseFactor();
                    if (mod == 0) throw new ArithmeticException("Division by zero");
                    x %= mod;
                } else {
                    return x;
                }
            }
        }

        private double parseFactor() {
            if (eat('+')) return +parseFactor();
            if (eat('-')) return -parseFactor();

            double x;
            int startPos = this.pos;

            if (eat('(')) {
                x = parseExpression();
                if (!eat(')')) throw new RuntimeException("Missing closing parenthesis ')'");
            } else if ((ch >= '0' && ch <= '9') || ch == '.') {
                while ((ch >= '0' && ch <= '9') || ch == '.') nextChar();
                x = Double.parseDouble(str.substring(startPos, this.pos));
            } else if ((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z') || ch == '_') {
                while ((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z') || (ch >= '0' && ch <= '9') || ch == '_') nextChar();
                String name = str.substring(startPos, this.pos);

                while (ch == ' ' || ch == '\t') nextChar();

                if (ch == '(') {
                    eat('(');
                    List<Double> args = new ArrayList<>();
                    if (!eat(')')) {
                        args.add(parseExpression());
                        while (eat(',')) {
                            args.add(parseExpression());
                        }
                        if (!eat(')')) throw new RuntimeException("Missing closing ')' in function " + name);
                    }
                    x = evalFunction(name.toLowerCase(), args);
                } else {
                    x = evalVariable(name);
                }
            } else {
                throw new RuntimeException("Unexpected symbol: " + (ch == -1 ? "end of expression" : (char) ch));
            }

            if (eat('^')) x = Math.pow(x, parseFactor());

            return x;
        }

        private double evalVariable(String name) {
            String lower = name.toLowerCase();
            if (lower.equals("pi")) return Math.PI;
            if (lower.equals("e")) return Math.E;

            if (variables.containsKey(name)) {
                return variables.get(name);
            }
            if (variables.containsKey(lower)) {
                return variables.get(lower);
            }
            throw new RuntimeException("Unknown variable: " + name);
        }

        private double evalFunction(String func, List<Double> args) {
            if (args.isEmpty()) throw new RuntimeException("Function " + func + " requires arguments");
            double a0 = args.get(0);

            switch (func) {
                case "sqrt":
                    if (a0 < 0) throw new RuntimeException("sqrt of negative number");
                    return Math.sqrt(a0);
                case "abs":
                    return Math.abs(a0);
                case "sin":
                    return Math.sin(Math.toRadians(a0));
                case "cos":
                    return Math.cos(Math.toRadians(a0));
                case "tan":
                    return Math.tan(Math.toRadians(a0));
                case "log":
                case "ln":
                    if (a0 <= 0) throw new RuntimeException("log of non-positive number");
                    return Math.log(a0);
                case "log10":
                    if (a0 <= 0) throw new RuntimeException("log10 of non-positive number");
                    return Math.log10(a0);
                case "round":
                    if (args.size() > 1) {
                        int decimals = (int) Math.round(args.get(1));
                        double factor = Math.pow(10, decimals);
                        return Math.round(a0 * factor) / factor;
                    }
                    return Math.round(a0);
                case "ceil":
                    return Math.ceil(a0);
                case "floor":
                    return Math.floor(a0);
                case "min":
                    if (args.size() < 2) throw new RuntimeException("min requires 2 arguments");
                    return Math.min(a0, args.get(1));
                case "max":
                    if (args.size() < 2) throw new RuntimeException("max requires 2 arguments");
                    return Math.max(a0, args.get(1));
                default:
                    throw new RuntimeException("Unknown function: " + func);
            }
        }
    }
}
