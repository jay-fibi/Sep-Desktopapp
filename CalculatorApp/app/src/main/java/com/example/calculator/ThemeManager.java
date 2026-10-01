package com.example.calculator;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.StringRes;
import androidx.annotation.StyleRes;

/**
 * Manages the app's selectable color themes: persistence in
 * {@link SharedPreferences} and application before the view hierarchy
 * is inflated.
 */
public final class ThemeManager {

    private static final String PREFS_NAME = "calculator_settings";
    private static final String KEY_THEME = "app_theme";

    /** Every user-selectable theme, in the order shown in the picker. */
    public enum AppTheme {
        LIGHT(R.style.Theme_Calculator_Light, R.string.theme_light),
        DARK(R.style.Theme_Calculator_Dark, R.string.theme_dark),
        OCEAN(R.style.Theme_Calculator_Ocean, R.string.theme_ocean),
        FOREST(R.style.Theme_Calculator_Forest, R.string.theme_forest),
        SUNSET(R.style.Theme_Calculator_Sunset, R.string.theme_sunset);

        @StyleRes
        final int styleRes;
        @StringRes
        final int labelRes;

        AppTheme(@StyleRes int styleRes, @StringRes int labelRes) {
            this.styleRes = styleRes;
            this.labelRes = labelRes;
        }
    }

    private ThemeManager() {
        // no instances
    }

    /**
     * Applies the saved theme to the activity. Must be called before
     * {@code setContentView()}, ideally as the first line of {@code onCreate()}.
     */
    public static void applyTheme(Activity activity) {
        activity.setTheme(getSavedTheme(activity).styleRes);
    }

    /** Returns the persisted theme, defaulting to {@link AppTheme#LIGHT}. */
    public static AppTheme getSavedTheme(Context context) {
        String name = prefs(context).getString(KEY_THEME, AppTheme.LIGHT.name());
        try {
            return AppTheme.valueOf(name);
        } catch (IllegalArgumentException e) {
            return AppTheme.LIGHT;
        }
    }

    /** Persists the chosen theme. */
    public static void saveTheme(Context context, AppTheme theme) {
        prefs(context).edit().putString(KEY_THEME, theme.name()).apply();
    }

    /** All theme display names, for the picker dialog. */
    public static String[] getThemeNames(Context context) {
        AppTheme[] themes = AppTheme.values();
        String[] names = new String[themes.length];
        for (int i = 0; i < themes.length; i++) {
            names[i] = context.getString(themes[i].labelRes);
        }
        return names;
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
