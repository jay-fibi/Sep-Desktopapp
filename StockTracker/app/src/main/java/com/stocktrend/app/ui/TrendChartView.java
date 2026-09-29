package com.stocktrend.app.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.stocktrend.app.R;
import com.stocktrend.app.engine.TrendEngine;
import com.stocktrend.app.util.Dates;

import java.util.List;

/**
 * Lightweight price/trend chart drawn straight onto a {@link Canvas}.
 *
 * <p>No charting library is pulled in: the view plots the series it is handed,
 * scales the Y axis to the observed range, shades the area under the line and
 * marks the most recent observation. It renders an empty state when a symbol
 * has no entries yet.</p>
 */
public class TrendChartView extends View {

    /** Space on the left for the price axis labels. */
    private static final float AXIS_GUTTER_DP = 54f;
    /** Space at the bottom for the date axis labels. */
    private static final float DATE_GUTTER_DP = 22f;
    private static final float TOP_INSET_DP = 14f;
    private static final float RIGHT_INSET_DP = 14f;
    private static final int GRID_LINES = 4;

    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint axisPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint emptyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Path linePath = new Path();
    private final Path fillPath = new Path();

    private final float density;

    private TrendEngine.Series series;
    private String emptyText = "";
    private int lineColor;
    private int fillTopColor;
    private int fillBottomColor;
    private final int pointColor;
    private float gradientTop = Float.NaN;
    private float gradientBottom = Float.NaN;

    public TrendChartView(Context context) {
        this(context, null);
    }

    public TrendChartView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TrendChartView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        density = getResources().getDisplayMetrics().density;

        lineColor = ContextCompat.getColor(context, R.color.chart_line);
        fillTopColor = ContextCompat.getColor(context, R.color.chart_fill_top);
        fillBottomColor = ContextCompat.getColor(context, R.color.chart_fill_bottom);
        pointColor = ContextCompat.getColor(context, R.color.chart_point);

        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(dp(2.5f));
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setStrokeJoin(Paint.Join.ROUND);
        linePaint.setColor(lineColor);

        fillPaint.setStyle(Paint.Style.FILL);

        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(dp(1f));
        gridPaint.setColor(ContextCompat.getColor(context, R.color.chart_grid));

        axisPaint.setColor(ContextCompat.getColor(context, R.color.chart_axis_text));
        axisPaint.setTextSize(dp(9.5f));

        dotPaint.setStyle(Paint.Style.FILL);
        dotPaint.setColor(pointColor);

        emptyPaint.setColor(ContextCompat.getColor(context, R.color.text_hint));
        emptyPaint.setTextSize(dp(13f));
        emptyPaint.setTextAlign(Paint.Align.CENTER);
    }

    /** Supplies the data to plot; pass an empty series to show the empty state. */
    public void setSeries(@Nullable TrendEngine.Series value) {
        this.series = value;
        invalidate();
    }

    public void setEmptyText(String value) {
        this.emptyText = value == null ? "" : value;
        invalidate();
    }

    public void setLineColor(int color) {
        this.lineColor = color;
        linePaint.setColor(color);
        invalidate();
    }

    private float dp(float value) {
        return value * density;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float left = getPaddingLeft() + dp(AXIS_GUTTER_DP);
        float right = getWidth() - getPaddingRight() - dp(RIGHT_INSET_DP);
        float top = getPaddingTop() + dp(TOP_INSET_DP);
        float bottom = getHeight() - getPaddingBottom() - dp(DATE_GUTTER_DP);

        if (right - left <= dp(24f) || bottom - top <= dp(24f)) {
            return;
        }

        List<TrendEngine.Point> points = series == null ? null : series.getPoints();
        if (points == null || points.isEmpty()) {
            drawEmptyState(canvas, left, top, right, bottom);
            return;
        }

        double min = Math.max(0d, series.getMinPrice() - series.getPadding());
        double max = series.getMaxPrice() + series.getPadding();
        if (max <= min) {
            max = min + 1d;
        }
        double span = max - min;

        drawGridAndPriceAxis(canvas, left, right, top, bottom, min, span);

        int count = points.size();
        float usableWidth = right - left;
        float xStep = count > 1 ? usableWidth / (count - 1) : 0f;

        linePath.reset();
        float firstX = 0f;
        float lastX = 0f;
        float lastY = 0f;

        for (int i = 0; i < count; i++) {
            float x = count == 1 ? left + usableWidth / 2f : left + xStep * i;
            float y = yFor(points.get(i).getPrice(), min, span, top, bottom);
            if (i == 0) {
                firstX = x;
                linePath.moveTo(x, y);
            } else {
                linePath.lineTo(x, y);
            }
            lastX = x;
            lastY = y;
        }

        drawAreaUnderLine(canvas, top, bottom, firstX, lastX, count);
        canvas.drawPath(linePath, linePaint);

        if (count <= 60) {
            for (int i = 0; i < count; i++) {
                float x = count == 1 ? left + usableWidth / 2f : left + xStep * i;
                float y = yFor(points.get(i).getPrice(), min, span, top, bottom);
                canvas.drawCircle(x, y, dp(3f), dotPaint);
            }
        }

        canvas.drawCircle(lastX, lastY, dp(6f), dotPaint);
        dotPaint.setColor(lineColor);
        canvas.drawCircle(lastX, lastY, dp(3.6f), dotPaint);
        dotPaint.setColor(pointColor);

        drawDateAxis(canvas, points, left, right, bottom, count);
    }

    private void drawGridAndPriceAxis(Canvas canvas, float left, float right, float top,
                                      float bottom, double min, double span) {
        axisPaint.setTextAlign(Paint.Align.RIGHT);
        for (int i = 0; i <= GRID_LINES; i++) {
            float ratio = (float) i / GRID_LINES;
            float y = top + (bottom - top) * ratio;
            canvas.drawLine(left, y, right, y, gridPaint);
            double value = min + span * (1d - ratio);
            canvas.drawText(TrendEngine.formatPriceShort(value), left - dp(6f),
                    y + dp(3.5f), axisPaint);
        }
    }

    private void drawAreaUnderLine(Canvas canvas, float top, float bottom,
                                   float firstX, float lastX, int count) {
        if (count < 2) {
            return;
        }
        fillPath.set(linePath);
        fillPath.lineTo(lastX, bottom);
        fillPath.lineTo(firstX, bottom);
        fillPath.close();

        if (gradientTop != top || gradientBottom != bottom) {
            fillPaint.setShader(new LinearGradient(0f, top, 0f, bottom,
                    fillTopColor, fillBottomColor, Shader.TileMode.CLAMP));
            gradientTop = top;
            gradientBottom = bottom;
        }
        canvas.drawPath(fillPath, fillPaint);
    }

    private void drawDateAxis(Canvas canvas, List<TrendEngine.Point> points,
                              float left, float right, float bottom, int count) {
        float labelY = bottom + dp(14f);
        axisPaint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText(Dates.compact(points.get(0).getDateMillis()), left, labelY, axisPaint);

        if (count >= 3) {
            axisPaint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(Dates.compact(points.get(count / 2).getDateMillis()),
                    (left + right) / 2f, labelY, axisPaint);
        }

        axisPaint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText(Dates.compact(points.get(count - 1).getDateMillis()), right, labelY, axisPaint);
    }

    private void drawEmptyState(Canvas canvas, float left, float top, float right, float bottom) {
        String text = emptyText == null || emptyText.isEmpty() ? "No data yet" : emptyText;
        String[] lines = text.split("\n");
        float centreX = (left + right) / 2f;
        float lineHeight = dp(19f);
        float y = (top + bottom) / 2f - (lines.length - 1) * lineHeight / 2f;
        for (String line : lines) {
            canvas.drawText(line, centreX, y, emptyPaint);
            y += lineHeight;
        }
    }

    private float yFor(double value, double min, double span, float top, float bottom) {
        double ratio = (value - min) / span;
        return (float) (bottom - ratio * (bottom - top));
    }
}
