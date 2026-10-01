package com.example.calculator;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Single-screen calculator. Applies the saved theme before inflating the
 * layout, forwards key presses to {@link CalculatorEngine} and mirrors the
 * engine state onto the two-line display.
 */
public class MainActivity extends AppCompatActivity {

    private static final String STATE_EXPRESSION = "state_expression";
    private static final String STATE_EVALUATED = "state_evaluated";
    private static final String STATE_ERROR = "state_error";
    private static final String STATE_HISTORY = "state_history";

    private final CalculatorEngine engine = new CalculatorEngine();

    private TextView expressionText;
    private TextView previewText;
    /** Small-line history shown after '=', e.g. "12+7 =". */
    private String lastHistory = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Theme must be applied before the layout is inflated.
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        expressionText = findViewById(R.id.text_expression);
        previewText = findViewById(R.id.text_preview);

        if (savedInstanceState != null) {
            engine.restoreState(
                    savedInstanceState.getString(STATE_EXPRESSION),
                    savedInstanceState.getBoolean(STATE_EVALUATED),
                    savedInstanceState.getBoolean(STATE_ERROR));
            lastHistory = savedInstanceState.getString(STATE_HISTORY, "");
        }

        bindButtons();
        findViewById(R.id.button_theme).setOnClickListener(v -> showThemePicker());
        refreshDisplay();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_EXPRESSION, engine.getRawExpression());
        outState.putBoolean(STATE_EVALUATED, engine.isShowingResult());
        outState.putBoolean(STATE_ERROR, engine.isError());
        outState.putString(STATE_HISTORY, lastHistory);
    }

    // ------------------------------------------------------------------
    // Buttons
    // ------------------------------------------------------------------

    private void bindButtons() {
        bindDigit(R.id.button_0, 0);
        bindDigit(R.id.button_1, 1);
        bindDigit(R.id.button_2, 2);
        bindDigit(R.id.button_3, 3);
        bindDigit(R.id.button_4, 4);
        bindDigit(R.id.button_5, 5);
        bindDigit(R.id.button_6, 6);
        bindDigit(R.id.button_7, 7);
        bindDigit(R.id.button_8, 8);
        bindDigit(R.id.button_9, 9);

        bindOperator(R.id.button_add, '+');
        bindOperator(R.id.button_subtract, '-');
        bindOperator(R.id.button_multiply, '*');
        bindOperator(R.id.button_divide, '/');

        bindAction(R.id.button_dot, () -> engine.inputDot());
        bindAction(R.id.button_percent, () -> engine.inputPercent());
        bindAction(R.id.button_sign, () -> engine.toggleSign());
        bindAction(R.id.button_backspace, () -> engine.backspace());
        bindAction(R.id.button_clear, () -> {
            engine.clear();
            lastHistory = "";
        });
        bindAction(R.id.button_equals, this::onEqualsPressed);
    }

    private void bindDigit(int viewId, int digit) {
        findViewById(viewId).setOnClickListener(v -> {
            engine.inputDigit(digit);
            refreshDisplay();
        });
    }

    private void bindOperator(int viewId, char operator) {
        findViewById(viewId).setOnClickListener(v -> {
            engine.inputOperator(operator);
            refreshDisplay();
        });
    }

    private void bindAction(int viewId, Runnable action) {
        findViewById(viewId).setOnClickListener(v -> {
            action.run();
            refreshDisplay();
        });
    }

    private void onEqualsPressed() {
        String before = engine.getDisplayExpression();
        engine.evaluate();
        if (engine.isShowingResult()) {
            lastHistory = before + " =";
        } else if (!engine.isError()) {
            lastHistory = "";
        }
    }

    // ------------------------------------------------------------------
    // Display
    // ------------------------------------------------------------------

    private void refreshDisplay() {
        String expression = engine.getDisplayExpression();
        expressionText.setText(expression.isEmpty() ? "0" : expression);

        if (engine.isError()) {
            previewText.setText(R.string.error_generic);
        } else if (engine.isShowingResult()) {
            previewText.setText(lastHistory);
        } else {
            String preview = engine.getPreview();
            previewText.setText(preview != null ? "= " + preview : "");
        }
    }

    // ------------------------------------------------------------------
    // Theme picker
    // ------------------------------------------------------------------

    private void showThemePicker() {
        ThemeManager.AppTheme current = ThemeManager.getSavedTheme(this);
        new AlertDialog.Builder(this)
                .setTitle(R.string.choose_theme)
                .setSingleChoiceItems(
                        ThemeManager.getThemeNames(this),
                        current.ordinal(),
                        (dialog, which) -> {
                            ThemeManager.AppTheme selected = ThemeManager.AppTheme.values()[which];
                            dialog.dismiss();
                            if (selected != current) {
                                ThemeManager.saveTheme(this, selected);
                                // Recreate so onCreate applies the new theme.
                                recreate();
                            }
                        })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
}
