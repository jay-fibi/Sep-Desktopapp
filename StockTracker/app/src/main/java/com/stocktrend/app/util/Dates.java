package com.stocktrend.app.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Small date helpers shared by the UI and the trend engine.
 *
 * <p>Every observation is anchored to local midnight so that two entries for
 * the same calendar day collapse into a single point on the chart. No Android
 * imports here, which keeps the class unit-testable on a plain JVM.</p>
 */
public final class Dates {

    private static final String KEY_PATTERN = "yyyy-MM-dd";
    private static final String PRETTY_PATTERN = "d MMM yyyy";
    private static final String COMPACT_PATTERN = "d MMM";

    private Dates() {
    }

    /** Local midnight of the current day. */
    public static long today() {
        return normalize(System.currentTimeMillis());
    }

    /** Truncates an epoch timestamp down to local midnight. */
    public static long normalize(long millis) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(millis);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    /** Stable, sortable identity of a day: {@code 2026-09-28}. */
    public static String key(long millis) {
        return format(millis, KEY_PATTERN);
    }

    /** Human readable form used in lists, e.g. {@code 28 Sep 2026}. */
    public static String pretty(long millis) {
        return format(millis, PRETTY_PATTERN);
    }

    /** Short form used for chart axis labels, e.g. {@code 28 Sep}. */
    public static String compact(long millis) {
        return format(millis, COMPACT_PATTERN);
    }

    /** Shifts a midnight timestamp by {@code days}, keeping it at midnight. */
    public static long addDays(long millis, int days) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(millis);
        cal.add(Calendar.DAY_OF_YEAR, days);
        return normalize(cal.getTimeInMillis());
    }

    private static String format(long millis, String pattern) {
        SimpleDateFormat sdf = new SimpleDateFormat(pattern, Locale.getDefault());
        return sdf.format(new Date(millis));
    }
}
