package com.stocktrend.app.data;

import com.stocktrend.app.util.Dates;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * One-off demo data set.
 *
 * <p>It exists so the trend chart is not empty on first launch; every entry is
 * flagged with {@link StockPrice#SOURCE_SAMPLE} and shows a "Sample" badge in
 * the list, and it can be deleted like any other entry.</p>
 */
public final class SampleData {

    private static final String[] SYMBOLS = {"RELIANCE", "TCS", "INFY"};
    private static final String[] COMPANIES = {
            "Reliance Industries Limited",
            "Tata Consultancy Services Limited",
            "Infosys Limited"
    };
    private static final double[][] CLOSES = {
            {2890.40, 2915.75, 2876.10, 2932.55, 2960.20, 2944.85, 2998.30, 3032.65, 3010.15, 3055.90},
            {3890.00, 3925.45, 3960.80, 3912.30, 3948.95, 3985.20, 4010.60, 3992.35, 4035.70, 4062.25},
            {1595.50, 1612.20, 1601.85, 1628.40, 1645.10, 1633.75, 1660.30, 1678.55, 1665.20, 1691.80}
    };

    /** Number of days between two consecutive demo readings. */
    private static final int STEP_DAYS = 3;

    private SampleData() {
    }

    /** Builds the demo entries, newest first, exactly like {@link StockRepository#getAll()}. */
    public static List<StockPrice> build() {
        List<StockPrice> entries = new ArrayList<>();
        long today = Dates.today();
        long now = System.currentTimeMillis();

        for (int s = 0; s < SYMBOLS.length; s++) {
            double[] series = CLOSES[s];
            for (int i = series.length - 1; i >= 0; i--) {
                int daysAgo = (series.length - 1 - i) * STEP_DAYS;
                long date = Dates.addDays(today, -daysAgo);
                entries.add(new StockPrice(
                        UUID.randomUUID().toString(),
                        SYMBOLS[s],
                        COMPANIES[s],
                        series[i],
                        date,
                        StockPrice.SOURCE_SAMPLE,
                        now
                ));
            }
        }
        return entries;
    }
}
