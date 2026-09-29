package com.stocktrend.app.engine;

import com.stocktrend.app.data.StockPrice;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TrendEngineTest {

    private TrendEngine engine;

    @Before
    public void setUp() {
        engine = new TrendEngine();
    }

    /** Local midnight for a fixed calendar day. */
    private static long day(int year, int month, int dayOfMonth) {
        Calendar cal = Calendar.getInstance();
        cal.set(year, month - 1, dayOfMonth, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    private static StockPrice entry(String symbol, double price, long date) {
        return new StockPrice("id-" + symbol + date, symbol, symbol + " Limited", price, date,
                StockPrice.SOURCE_MANUAL, date);
    }

    @Test
    public void buildSeriesOrdersOldestFirstAndFiltersBySymbol() {
        List<StockPrice> entries = new ArrayList<>();
        entries.add(entry("INFY", 1600, day(2026, 9, 3)));
        entries.add(entry("TCS", 3900, day(2026, 9, 1)));
        entries.add(entry("INFY", 1500, day(2026, 9, 1)));
        entries.add(entry("INFY", 1550, day(2026, 9, 2)));

        TrendEngine.Series series = engine.buildSeries(entries, "INFY");

        assertEquals(3, series.size());
        assertEquals("INFY", series.getSymbol());
        assertEquals(1500d, series.getPoints().get(0).getPrice(), 0.001d);
        assertEquals(1600d, series.getLastPrice(), 0.001d);
        assertTrue(series.getLastDateMillis() > series.getPoints().get(0).getDateMillis());
    }

    @Test
    public void buildSeriesWithUnknownSymbolIsEmpty() {
        List<StockPrice> entries = new ArrayList<>();
        entries.add(entry("INFY", 1600, day(2026, 9, 3)));

        TrendEngine.Series series = engine.buildSeries(entries, "WIPRO");

        assertTrue(series.isEmpty());
        assertEquals(0d, series.getLastPrice(), 0.001d);
        assertEquals(0d, series.getPercentChange(), 0.001d);
    }

    @Test
    public void collapseKeepsOnePointPerDay() {
        List<TrendEngine.Point> points = new ArrayList<>();
        long date = day(2026, 9, 10);
        points.add(new TrendEngine.Point(date, 100d, StockPrice.SOURCE_MANUAL));
        points.add(new TrendEngine.Point(date, 120d, StockPrice.SOURCE_NSE));

        List<TrendEngine.Point> collapsed = engine.sortAndCollapse(points);

        assertEquals(1, collapsed.size());
        assertEquals(120d, collapsed.get(0).getPrice(), 0.001d);
        assertEquals(StockPrice.SOURCE_NSE, collapsed.get(0).getSource());
    }

    @Test
    public void statisticsAreDerivedFromTheSeries() {
        List<StockPrice> entries = new ArrayList<>();
        entries.add(entry("RELIANCE", 100d, day(2026, 9, 1)));
        entries.add(entry("RELIANCE", 120d, day(2026, 9, 2)));
        entries.add(entry("RELIANCE", 110d, day(2026, 9, 3)));

        TrendEngine.Series series = engine.buildSeries(entries, "RELIANCE");

        assertEquals(100d, series.getMinPrice(), 0.001d);
        assertEquals(120d, series.getMaxPrice(), 0.001d);
        assertEquals(110d, series.getAveragePrice(), 0.001d);
        assertEquals(10d, series.getAbsoluteChange(), 0.001d);
        assertEquals(10d, series.getPercentChange(), 0.001d);
        assertEquals(TrendEngine.UP, series.getDirection());
        assertTrue(series.getPadding() > 0d);
    }

    @Test
    public void fallingSeriesReportsDown() {
        List<StockPrice> entries = new ArrayList<>();
        entries.add(entry("SBIN", 500d, day(2026, 9, 1)));
        entries.add(entry("SBIN", 450d, day(2026, 9, 2)));

        TrendEngine.Series series = engine.buildSeries(entries, "SBIN");

        assertEquals(TrendEngine.DOWN, series.getDirection());
        assertEquals(-10d, series.getPercentChange(), 0.001d);
    }

    @Test
    public void flatSeriesWithOnePointIsNeutral() {
        List<StockPrice> entries = new ArrayList<>();
        entries.add(entry("ITC", 400d, day(2026, 9, 1)));

        TrendEngine.Series series = engine.buildSeries(entries, "ITC");

        assertEquals(TrendEngine.FLAT, series.getDirection());
        assertEquals(8d, series.getPadding(), 0.001d);
    }

    @Test
    public void distinctSymbolsAreDeduplicatedAndSorted() {
        List<StockPrice> entries = new ArrayList<>();
        entries.add(entry("TCS", 3900, day(2026, 9, 1)));
        entries.add(entry("reliance", 2900, day(2026, 9, 1)));
        entries.add(entry("RELIANCE", 2950, day(2026, 9, 2)));
        entries.add(entry("", 0, day(2026, 9, 2)));

        List<String> symbols = engine.distinctSymbols(entries);

        assertEquals(2, symbols.size());
        assertEquals("RELIANCE", symbols.get(0));
        assertEquals("TCS", symbols.get(1));
    }

    @Test
    public void symbolValidationAcceptsNseStyleSymbols() {
        assertEquals("M&M", TrendEngine.normalizeSymbol(" m&m "));
        assertEquals("BAJAJ-AUTO", TrendEngine.normalizeSymbol("bajaj-auto"));

        assertTrue(TrendEngine.isValidSymbol("reliance"));
        assertTrue(TrendEngine.isValidSymbol("M&M"));
        assertTrue(TrendEngine.isValidSymbol("BAJAJ-AUTO"));
        assertTrue(TrendEngine.isValidSymbol("3MINDIA"));

        assertFalse(TrendEngine.isValidSymbol(""));
        assertFalse(TrendEngine.isValidSymbol("RELIANCE.NS"));
        assertFalse(TrendEngine.isValidSymbol("THIS-SYMBOL-IS-WAY-TOO-LONG"));
    }

    @Test
    public void parsePriceToleratesTypedDecoration() {
        assertEquals(1234.5d, TrendEngine.parsePrice("\u20b91,234.50"), 0.001d);
        assertEquals(3055.9d, TrendEngine.parsePrice(" 3055.90 "), 0.001d);

        assertTrue(Double.isNaN(TrendEngine.parsePrice("")));
        assertTrue(Double.isNaN(TrendEngine.parsePrice("abc")));
        assertTrue(Double.isNaN(TrendEngine.parsePrice("-15")));
        assertTrue(Double.isNaN(TrendEngine.parsePrice("0")));
    }

    @Test
    public void priceFormattingUsesIndianDigitGrouping() {
        assertEquals("3,055.90", TrendEngine.formatPrice(3055.9d));
        assertEquals("1,234.50", TrendEngine.formatPrice(1234.5d));
        assertEquals("12,34,567.00", TrendEngine.formatPrice(1234567d));
        assertEquals("1,23,45,678", TrendEngine.formatPriceShort(12345678d));
        assertEquals("3,056", TrendEngine.formatPriceShort(3056.4d));
        assertEquals("999", TrendEngine.formatPriceShort(999.2d));
        assertEquals("499.00", TrendEngine.formatPrice(499d));
    }

    @Test
    public void changeFormattingCarriesTheSign() {
        assertEquals("+5.00", TrendEngine.formatChange(5d));
        assertEquals("-5.00", TrendEngine.formatChange(-5d));
        assertEquals("+1.52%", TrendEngine.formatPercent(1.5249d));
        assertEquals("-4.32%", TrendEngine.formatPercent(-4.32d));
        assertEquals("0.00%", TrendEngine.formatPercent(0d));
    }
}
