package com.stocktrend.app.engine;

import com.stocktrend.app.data.StockPrice;
import com.stocktrend.app.util.Dates;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Turns a flat list of {@link StockPrice} rows into the ordered series that the
 * trend chart and the summary statistics are drawn from.
 *
 * <p>Contains no Android dependencies on purpose: the mapping logic is covered
 * by {@code TrendEngineTest} running on a plain JVM.</p>
 */
public class TrendEngine {

    /** Direction values used to colour the change indicator. */
    public static final int FLAT = 0;
    public static final int UP = 1;
    public static final int DOWN = -1;

    /** A single charted observation. */
    public static class Point {
        private final long dateMillis;
        private final double price;
        private final String source;

        public Point(long dateMillis, double price, String source) {
            this.dateMillis = dateMillis;
            this.price = price;
            this.source = source;
        }

        public long getDateMillis() {
            return dateMillis;
        }

        public double getPrice() {
            return price;
        }

        public String getSource() {
            return source;
        }
    }

    /** Ordered observations for one symbol plus the derived statistics. */
    public static class Series {
        private final String symbol;
        private final String companyName;
        private final List<Point> points;

        Series(String symbol, String companyName, List<Point> points) {
            this.symbol = symbol;
            this.companyName = companyName;
            this.points = Collections.unmodifiableList(points);
        }

        public String getSymbol() {
            return symbol;
        }

        public String getCompanyName() {
            return companyName;
        }

        public List<Point> getPoints() {
            return points;
        }

        public int size() {
            return points.size();
        }

        public boolean isEmpty() {
            return points.isEmpty();
        }

        public double getFirstPrice() {
            return points.isEmpty() ? 0d : points.get(0).getPrice();
        }

        public double getLastPrice() {
            return points.isEmpty() ? 0d : points.get(points.size() - 1).getPrice();
        }

        public long getLastDateMillis() {
            return points.isEmpty() ? 0L : points.get(points.size() - 1).getDateMillis();
        }

        public double getMinPrice() {
            if (points.isEmpty()) {
                return 0d;
            }
            double min = Double.MAX_VALUE;
            for (Point p : points) {
                min = Math.min(min, p.getPrice());
            }
            return min;
        }

        public double getMaxPrice() {
            if (points.isEmpty()) {
                return 0d;
            }
            double max = -Double.MAX_VALUE;
            for (Point p : points) {
                max = Math.max(max, p.getPrice());
            }
            return max;
        }

        public double getAveragePrice() {
            if (points.isEmpty()) {
                return 0d;
            }
            double sum = 0d;
            for (Point p : points) {
                sum += p.getPrice();
            }
            return sum / points.size();
        }

        /** Absolute move between the oldest and the newest observation. */
        public double getAbsoluteChange() {
            if (points.size() < 2) {
                return 0d;
            }
            return getLastPrice() - getFirstPrice();
        }

        /** Percentage move between the oldest and the newest observation. */
        public double getPercentChange() {
            double first = getFirstPrice();
            if (points.size() < 2 || first == 0d) {
                return 0d;
            }
            return (getLastPrice() - first) / first * 100d;
        }

        public int getDirection() {
            double change = getAbsoluteChange();
            double epsilon = Math.abs(getFirstPrice()) * 0.0001d;
            if (Math.abs(change) <= epsilon) {
                return FLAT;
            }
            return change > 0d ? UP : DOWN;
        }

        /** Headroom for the Y axis so the line never touches the chart edges. */
        public double getPadding() {
            double span = getMaxPrice() - getMinPrice();
            if (span <= 0d) {
                return Math.max(Math.abs(getMaxPrice()) * 0.02d, 1d);
            }
            return span * 0.12d;
        }
    }

    /** Ordered series (oldest to newest) for {@code symbol}; never {@code null}. */
    public Series buildSeries(List<StockPrice> entries, String symbol) {
        List<Point> points = new ArrayList<>();
        String companyName = "";

        if (entries != null && symbol != null) {
            for (StockPrice entry : entries) {
                if (!symbol.equalsIgnoreCase(entry.getSymbol())) {
                    continue;
                }
                points.add(new Point(entry.getDateMillis(), entry.getPrice(), entry.getSource()));
                if (companyName.isEmpty() && entry.hasCompanyName()) {
                    companyName = entry.getCompanyName();
                }
            }
        }

        return new Series(symbol == null ? "" : symbol, companyName, sortAndCollapse(points));
    }

    /**
     * Sorts oldest first and keeps a single point per calendar day. When two
     * readings share a day the last one in the input wins, mirroring the way
     * {@code StockRepository.save} replaces an existing slot.
     */
    public List<Point> sortAndCollapse(List<Point> input) {
        List<Point> sorted = new ArrayList<>();
        if (input != null) {
            sorted.addAll(input);
        }
        Collections.sort(sorted, BY_DATE);

        Map<String, Point> byDay = new LinkedHashMap<>();
        for (Point p : sorted) {
            byDay.put(Dates.key(p.getDateMillis()), p);
        }

        List<Point> collapsed = new ArrayList<>(byDay.values());
        Collections.sort(collapsed, BY_DATE);
        return collapsed;
    }

    /** Distinct symbols present in the data, A to Z. */
    public List<String> distinctSymbols(List<StockPrice> entries) {
        Map<String, String> byUpper = new LinkedHashMap<>();
        if (entries != null) {
            for (StockPrice entry : entries) {
                String symbol = entry.getSymbol();
                if (symbol == null || symbol.trim().isEmpty()) {
                    continue;
                }
                String normalized = normalizeSymbol(symbol);
                if (!byUpper.containsKey(normalized)) {
                    byUpper.put(normalized, normalized);
                }
            }
        }
        List<String> symbols = new ArrayList<>(byUpper.values());
        Collections.sort(symbols, String.CASE_INSENSITIVE_ORDER);
        return symbols;
    }

    /** Uppercase, trimmed symbol with internal whitespace removed. */
    public static String normalizeSymbol(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.toUpperCase(Locale.US).replace(" ", "").trim();
    }

    /** NSE symbols are limited to A-Z, 0-9, {@code &} and {@code -}. */
    public static boolean isValidSymbol(String raw) {
        String symbol = normalizeSymbol(raw);
        if (symbol.isEmpty() || symbol.length() > 20) {
            return false;
        }
        for (int i = 0; i < symbol.length(); i++) {
            char c = symbol.charAt(i);
            boolean ok = (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '&' || c == '-';
            if (!ok) {
                return false;
            }
        }
        return true;
    }

    private static final Comparator<Point> BY_DATE = new Comparator<Point>() {
        @Override
        public int compare(Point a, Point b) {
            return Long.compare(a.getDateMillis(), b.getDateMillis());
        }
    };

    /** Parses a user typed price; tolerates {@code ₹}, spaces and thousands separators. */
    public static double parsePrice(String raw) {
        if (raw == null) {
            return Double.NaN;
        }
        String cleaned = raw.replace(",", "")
                .replace("\u20b9", "")
                .replace(" ", "")
                .trim();
        if (cleaned.isEmpty()) {
            return Double.NaN;
        }
        try {
            double value = Double.parseDouble(cleaned);
            if (Double.isNaN(value) || Double.isInfinite(value) || value <= 0d) {
                return Double.NaN;
            }
            return value;
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }

    /** Money formatting with Indian digit grouping and two decimals. */
    public static String formatPrice(double value) {
        long scaled = Math.round(Math.abs(value) * 100d);
        long whole = scaled / 100L;
        int fraction = (int) (scaled % 100L);
        return (value < 0d ? "-" : "")
                + groupIndian(whole)
                + "." + (fraction < 10 ? "0" : "") + fraction;
    }

    /** Money formatting with Indian digit grouping and no decimals (chart axis). */
    public static String formatPriceShort(double value) {
        return (value < 0d ? "-" : "") + groupIndian(Math.round(Math.abs(value)));
    }

    /** Signed percentage such as {@code +4.32%}. */
    public static String formatPercent(double percent) {
        return String.format(Locale.US, "%s%.2f%%", percent > 0d ? "+" : "", percent);
    }

    /** Signed money value such as {@code +125.50}. */
    public static String formatChange(double change) {
        String sign = change > 0d ? "+" : change < 0d ? "-" : "";
        return sign + formatPrice(Math.abs(change));
    }

    /** {@code 1234567 -> 12,34,567} (Indian lakh/crore grouping). */
    static String groupIndian(long value) {
        String digits = Long.toString(Math.abs(value));
        int length = digits.length();
        if (length <= 3) {
            return digits;
        }
        StringBuilder reversed = new StringBuilder(length + 4);
        int count = 0;
        for (int i = length - 1; i >= 0; i--) {
            if (count == 3 || (count > 3 && (count - 3) % 2 == 0)) {
                reversed.append(',');
            }
            reversed.append(digits.charAt(i));
            count++;
        }
        return reversed.reverse().toString();
    }
}
