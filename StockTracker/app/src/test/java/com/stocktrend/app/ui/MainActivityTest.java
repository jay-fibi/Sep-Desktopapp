package com.stocktrend.app.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.Dialog;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.stocktrend.app.R;
import com.stocktrend.app.data.SampleData;
import com.stocktrend.app.data.StockPrice;
import com.stocktrend.app.data.StockRepository;
import com.stocktrend.app.engine.TrendEngine;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import org.robolectric.shadows.ShadowDialog;

import java.util.Collections;

/**
 * Runs the real screen on the JVM: the layout is inflated, the seeded sample
 * data is charted and the "add price" dialog is opened and saved. This catches
 * resource/binding/render problems that a pure logic test cannot see.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class MainActivityTest {

    private MainActivity launch() {
        ActivityController<MainActivity> controller =
                Robolectric.buildActivity(MainActivity.class).setup();
        return controller.get();
    }

    @Test
    public void firstLaunchSeedsSampleDataAndChartsIt() {
        MainActivity activity = launch();
        Spinner spinner = activity.findViewById(R.id.spinnerSymbol);

        // One spinner row per seeded symbol, A to Z.
        assertEquals(3, spinner.getAdapter().getCount());
        assertEquals("INFY", spinner.getAdapter().getItem(0));
        assertEquals("RELIANCE", spinner.getAdapter().getItem(1));
        assertEquals("TCS", spinner.getAdapter().getItem(2));

        // The sample set is written to the repository as well.
        StockRepository repository = new StockRepository(activity);
        assertEquals(SampleData.build().size(), repository.getAll().size());

        // Statistics for the selected symbol are populated, not placeholders.
        TextView last = activity.findViewById(R.id.tvStatLast);
        TextView high = activity.findViewById(R.id.tvStatHigh);
        assertTrue(last.getText().toString().startsWith("\u20b9"));
        assertTrue(high.getText().toString().startsWith("\u20b9"));
        assertEquals(View.VISIBLE, activity.findViewById(R.id.tvSampleHint).getVisibility());
    }

    @Test
    public void chartRendersItsSeriesAndEmptyState() {
        MainActivity activity = launch();
        TrendChartView chart = activity.findViewById(R.id.chartView);

        // 720x420 is a realistic phone-sized canvas for the 210dp chart.
        chart.measure(View.MeasureSpec.makeMeasureSpec(720, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(420, View.MeasureSpec.EXACTLY));
        chart.layout(0, 0, 720, 420);

        Bitmap bitmap = Bitmap.createBitmap(720, 420, Bitmap.Config.ARGB_8888);
        bitmap.eraseColor(Color.WHITE);
        chart.draw(new Canvas(bitmap));

        // The seeded series must actually paint: grid, line and points.
        assertTrue("chart painted only " + countNonWhite(bitmap) + " pixels",
                countNonWhite(bitmap) > 1000);

        // Switching to a symbol with no data must render the empty state
        // instead of throwing.
        TrendChartView empty = new TrendChartView(activity);
        empty.setEmptyText(activity.getString(R.string.no_history));
        empty.setSeries(new TrendEngine().buildSeries(Collections.emptyList(), "WIPRO"));
        empty.measure(View.MeasureSpec.makeMeasureSpec(720, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(420, View.MeasureSpec.EXACTLY));
        empty.layout(0, 0, 720, 420);
        empty.draw(new Canvas(Bitmap.createBitmap(720, 420, Bitmap.Config.ARGB_8888)));
    }

    private static int countNonWhite(Bitmap bitmap) {
        int count = 0;
        for (int x = 0; x < bitmap.getWidth(); x += 4) {
            for (int y = 0; y < bitmap.getHeight(); y += 4) {
                if (bitmap.getPixel(x, y) != Color.WHITE) {
                    count++;
                }
            }
        }
        return count;
    }

    @Test
    public void addPriceDialogSavesAManualEntry() {
        MainActivity activity = launch();

        activity.findViewById(R.id.btnAddPrice).performClick();

        Dialog dialog = ShadowDialog.getLatestDialog();
        assertNotNull(dialog);

        EditText symbol = dialog.findViewById(R.id.etSymbol);
        EditText price = dialog.findViewById(R.id.etPrice);
        LinearLayout chips = dialog.findViewById(R.id.chipsContainer);
        assertNotNull(symbol);
        assertNotNull(price);
        // The quick-pick chips are generated for the popular symbols.
        assertEquals(MainActivity.QUICK_SYMBOLS.length, chips.getChildCount());

        symbol.setText("wipro");
        price.setText("\u20b91,234.50");
        dialog.findViewById(R.id.btnDialogSave).performClick();

        StockRepository repository = new StockRepository(activity);
        StockPrice saved = null;
        for (StockPrice entry : repository.getBySymbol("WIPRO")) {
            saved = entry;
        }
        assertNotNull(saved);
        // Symbol is normalised and the decorated price is parsed.
        assertEquals("WIPRO", saved.getSymbol());
        assertEquals(1234.5d, saved.getPrice(), 0.001d);
        assertFalse(saved.isFromNse());

        // The new symbol shows up in the selector.
        Spinner spinner = activity.findViewById(R.id.spinnerSymbol);
        assertEquals(4, spinner.getAdapter().getCount());
    }

    @Test
    public void historyListRendersTheSelectedSymbol() {
        MainActivity activity = launch();
        RecyclerView recycler = activity.findViewById(R.id.recyclerHistory);
        assertNotNull(recycler.getAdapter());
        assertEquals(10, recycler.getAdapter().getItemCount());
        assertEquals(View.GONE, activity.findViewById(R.id.tvHistoryEmpty).getVisibility());
    }
}
