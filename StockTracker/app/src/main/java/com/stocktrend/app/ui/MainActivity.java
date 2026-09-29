package com.stocktrend.app.ui;

import android.app.DatePickerDialog;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.stocktrend.app.R;
import com.stocktrend.app.data.SampleData;
import com.stocktrend.app.data.StockPrice;
import com.stocktrend.app.data.StockRepository;
import com.stocktrend.app.engine.TrendEngine;
import com.stocktrend.app.net.NseApiClient;
import com.stocktrend.app.net.Quote;
import com.stocktrend.app.util.Dates;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Single screen of the app: pick a stock, see its price trend, add or fetch
 * prices.
 *
 * <p>Prices come from two sources: what the user types and what the public NSE
 * India quote endpoint returns. Everything is stored locally through
 * {@link StockRepository}, and the chart is drawn by {@link TrendChartView}.</p>
 */
public class MainActivity extends AppCompatActivity {

    /** Handy symbols offered as tappable chips inside the add dialog. */
    static final String[] QUICK_SYMBOLS = {
            "RELIANCE", "TCS", "INFY", "HDFCBANK", "ICICIBANK", "SBIN",
            "ITC", "TATAMOTORS", "WIPRO", "BHARTIARTL", "LT", "AXISBANK"
    };

    private final TrendEngine engine = new TrendEngine();
    private StockRepository repository;
    private NseApiClient nseClient;

    private Spinner spinnerSymbol;
    private TextView tvCompanyName;
    private TextView tvEntryCount;
    private TextView tvChartSubtitle;
    private TextView tvHistoryEmpty;
    private TextView tvSampleHint;
    private TextView tvStatLast;
    private TextView tvStatChange;
    private TextView tvStatLow;
    private TextView tvStatHigh;
    private TextView tvStatAvg;
    private TrendChartView chartView;
    private RecyclerView recyclerHistory;
    private ProgressBar progressLive;

    private StockAdapter adapter;

    private final List<String> symbols = new ArrayList<>();
    private final List<StockPrice> history = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        repository = new StockRepository(this);
        nseClient = new NseApiClient();

        if (repository.isEmpty()) {
            seedSampleData();
        }

        initViews();
        setupRecycler();
        setupActions();
        refreshSymbols(null);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        nseClient.shutdown();
    }

    private void initViews() {
        spinnerSymbol = findViewById(R.id.spinnerSymbol);
        tvCompanyName = findViewById(R.id.tvCompanyName);
        tvEntryCount = findViewById(R.id.tvEntryCount);
        tvChartSubtitle = findViewById(R.id.tvChartSubtitle);
        tvHistoryEmpty = findViewById(R.id.tvHistoryEmpty);
        tvSampleHint = findViewById(R.id.tvSampleHint);
        tvStatLast = findViewById(R.id.tvStatLast);
        tvStatChange = findViewById(R.id.tvStatChange);
        tvStatLow = findViewById(R.id.tvStatLow);
        tvStatHigh = findViewById(R.id.tvStatHigh);
        tvStatAvg = findViewById(R.id.tvStatAvg);
        chartView = findViewById(R.id.chartView);
        recyclerHistory = findViewById(R.id.recyclerHistory);
        progressLive = findViewById(R.id.progressLive);
    }

    private void setupRecycler() {
        recyclerHistory.setLayoutManager(new LinearLayoutManager(this));
        recyclerHistory.setNestedScrollingEnabled(false);
        adapter = new StockAdapter(new StockAdapter.OnEntryListener() {
            @Override
            public void onEntryClick(StockPrice entry) {
                showEntryDetails(entry);
            }

            @Override
            public void onEntryDelete(StockPrice entry) {
                confirmDelete(entry);
            }
        });
        recyclerHistory.setAdapter(adapter);
    }

    private void setupActions() {
        findViewById(R.id.btnAddPrice).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddDialog(getSelectedSymbol());
            }
        });

        findViewById(R.id.btnFetchLive).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fetchLiveForSelected();
            }
        });
    }

    private void seedSampleData() {
        for (StockPrice entry : SampleData.build()) {
            repository.save(entry);
        }
    }

    /** Re-reads the stored symbols and keeps the previously selected one. */
    private void refreshSymbols(String preferred) {
        String keep = preferred != null ? preferred : getSelectedSymbol();

        symbols.clear();
        symbols.addAll(engine.distinctSymbols(repository.getAll()));

        List<String> items = symbols.isEmpty()
                ? Collections.singletonList(getString(R.string.no_symbols_title))
                : symbols;

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, items);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSymbol.setAdapter(spinnerAdapter);

        int index = indexOfSymbol(keep);
        spinnerSymbol.setSelection(index < 0 ? 0 : index);
        spinnerSymbol.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                renderSelected();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Nothing to do: the view already shows the empty state.
            }
        });

        renderSelected();
    }

    private int indexOfSymbol(String symbol) {
        if (symbol == null) {
            return -1;
        }
        for (int i = 0; i < symbols.size(); i++) {
            if (symbols.get(i).equalsIgnoreCase(symbol)) {
                return i;
            }
        }
        return -1;
    }

    private String getSelectedSymbol() {
        if (symbols.isEmpty()) {
            return "";
        }
        Object item = spinnerSymbol.getSelectedItem();
        return item == null ? "" : item.toString();
    }

    /** Rebuilds the chart, statistics and history list for the chosen symbol. */
    private void renderSelected() {
        String symbol = getSelectedSymbol();
        List<StockPrice> all = repository.getAll();

        history.clear();
        if (!symbol.isEmpty()) {
            history.addAll(repository.getBySymbol(symbol));
            Collections.sort(history, NEWEST_FIRST);
        }

        TrendEngine.Series series = engine.buildSeries(all, symbol);
        renderHeader(symbol, series);
        renderStats(series);
        renderChart(symbol, series);

        boolean hasSample = false;
        for (StockPrice entry : history) {
            if (entry.isSample()) {
                hasSample = true;
                break;
            }
        }
        tvSampleHint.setVisibility(hasSample ? View.VISIBLE : View.GONE);
        tvHistoryEmpty.setVisibility(!symbol.isEmpty() && history.isEmpty()
                ? View.VISIBLE : View.GONE);

        adapter.submit(history);
    }

    private void renderHeader(String symbol, TrendEngine.Series series) {
        if (symbol.isEmpty()) {
            tvCompanyName.setText(getString(R.string.app_subtitle));
            tvEntryCount.setVisibility(View.GONE);
            tvChartSubtitle.setText("");
            return;
        }

        String company = series.getCompanyName();
        tvCompanyName.setText(company.isEmpty() ? getString(R.string.company_unknown) : company);
        tvEntryCount.setVisibility(View.VISIBLE);
        tvEntryCount.setText(getResources().getQuantityString(
                R.plurals.entries_count, history.size(), history.size()));

        if (series.size() < 2) {
            tvChartSubtitle.setText("");
        } else {
            tvChartSubtitle.setText(getString(R.string.chart_range,
                    Dates.compact(series.getPoints().get(0).getDateMillis()),
                    Dates.compact(series.getLastDateMillis())));
        }
    }

    private void renderStats(TrendEngine.Series series) {
        int muted = ContextCompat.getColor(this, R.color.text_hint);

        if (series.isEmpty()) {
            tvStatLast.setText("\u2014");
            tvStatChange.setText("\u2014");
            tvStatLow.setText("\u2014");
            tvStatHigh.setText("\u2014");
            tvStatAvg.setText("\u2014");
            tvStatChange.setTextColor(muted);
            return;
        }

        tvStatLast.setText(money(series.getLastPrice()));
        tvStatLow.setText(money(series.getMinPrice()));
        tvStatHigh.setText(money(series.getMaxPrice()));
        tvStatAvg.setText(money(series.getAveragePrice()));

        tvStatChange.setText(TrendEngine.formatPercent(series.getPercentChange()));
        int direction = series.getDirection();
        tvStatChange.setTextColor(ContextCompat.getColor(this,
                direction == TrendEngine.UP ? R.color.trend_up
                        : direction == TrendEngine.DOWN ? R.color.trend_down
                        : R.color.trend_flat));
    }

    private void renderChart(String symbol, TrendEngine.Series series) {
        chartView.setSeries(series);
        chartView.setEmptyText(symbol.isEmpty()
                ? getString(R.string.no_symbols_body)
                : getString(R.string.no_history));

        int direction = series.getDirection();
        int color = direction == TrendEngine.UP ? R.color.trend_up
                : direction == TrendEngine.DOWN ? R.color.trend_down
                : R.color.chart_line;
        chartView.setLineColor(ContextCompat.getColor(this, color));
    }

    private String money(double value) {
        return getString(R.string.price_currency, TrendEngine.formatPrice(value));
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static final Comparator<StockPrice> NEWEST_FIRST = new Comparator<StockPrice>() {
        @Override
        public int compare(StockPrice a, StockPrice b) {
            int byDate = Long.compare(b.getDateMillis(), a.getDateMillis());
            return byDate != 0 ? byDate : Long.compare(b.getAddedAt(), a.getAddedAt());
        }
    };


    /** Opens the "add a price" dialog, optionally pre-filled with a symbol. */
    private void showAddDialog(String presetSymbol) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_stock, null);

        final EditText etSymbol = view.findViewById(R.id.etSymbol);
        final EditText etPrice = view.findViewById(R.id.etPrice);
        final EditText etDate = view.findViewById(R.id.etDate);
        final TextView tvFetchStatus = view.findViewById(R.id.tvFetchStatus);
        final TextView tvError = view.findViewById(R.id.tvError);
        final ProgressBar progressFetch = view.findViewById(R.id.progressFetch);
        final MaterialButton btnFetchNse = view.findViewById(R.id.btnFetchNse);
        final MaterialButton btnCancel = view.findViewById(R.id.btnDialogCancel);
        final MaterialButton btnSave = view.findViewById(R.id.btnDialogSave);
        final LinearLayout chipsContainer = view.findViewById(R.id.chipsContainer);

        final long[] selectedDate = {Dates.today()};
        final boolean[] liveFetched = {false};
        final String[] liveCompany = {""};

        etDate.setText(Dates.pretty(selectedDate[0]));
        etDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePicker(selectedDate, etDate);
            }
        });

        if (presetSymbol != null && !presetSymbol.isEmpty()) {
            etSymbol.setText(presetSymbol);
            etSymbol.setSelection(presetSymbol.length());
        }

        for (String quick : QUICK_SYMBOLS) {
            chipsContainer.addView(buildChip(quick, etSymbol));
        }

        etSymbol.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // A fetched quote only applies to the symbol it came from.
                liveFetched[0] = false;
                liveCompany[0] = "";
                hideError(tvError);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        final AlertDialog dialog = new AlertDialog.Builder(this).setView(view).create();

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        btnFetchNse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fetchIntoDialog(dialog, etSymbol, etPrice, tvFetchStatus, tvError,
                        progressFetch, btnFetchNse, liveFetched, liveCompany);
            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveFromDialog(dialog, etSymbol, etPrice, tvError, selectedDate,
                        liveFetched[0], liveCompany[0]);
            }
        });

        dialog.show();
    }


    private void fetchIntoDialog(final AlertDialog dialog, final EditText etSymbol,
                                 final EditText etPrice, final TextView tvFetchStatus,
                                 final TextView tvError, final ProgressBar progressFetch,
                                 final MaterialButton btnFetchNse, final boolean[] liveFetched,
                                 final String[] liveCompany) {

        final String symbol = TrendEngine.normalizeSymbol(etSymbol.getText().toString());
        if (symbol.isEmpty()) {
            showError(tvError, getString(R.string.err_symbol));
            return;
        }
        if (!TrendEngine.isValidSymbol(symbol)) {
            showError(tvError, getString(R.string.err_symbol_chars));
            return;
        }
        etSymbol.setText(symbol);
        if (!isNetworkAvailable()) {
            showError(tvError, getString(R.string.err_no_network));
            return;
        }

        hideError(tvError);
        setFetchBusy(btnFetchNse, progressFetch, tvFetchStatus, true, symbol);

        nseClient.fetchQuote(symbol, new NseApiClient.Callback() {
            @Override
            public void onSuccess(Quote quote) {
                if (isFinishing() || !dialog.isShowing()) {
                    return;
                }
                setFetchBusy(btnFetchNse, progressFetch, tvFetchStatus, false, symbol);

                // The symbol field is written first because its watcher resets
                // the "this price came from NSE" flag.
                if (!quote.getSymbol().isEmpty()) {
                    etSymbol.setText(quote.getSymbol());
                }
                etPrice.setText(plainNumber(quote.getLastPrice()));
                etPrice.setSelection(etPrice.getText().length());

                liveFetched[0] = true;
                liveCompany[0] = quote.getCompanyName();

                tvFetchStatus.setText(buildLiveStatus(quote));
                tvFetchStatus.setTextColor(ContextCompat.getColor(
                        MainActivity.this, R.color.badge_live_text));
                tvFetchStatus.setVisibility(View.VISIBLE);
            }

            @Override
            public void onError(String message) {
                if (isFinishing() || !dialog.isShowing()) {
                    return;
                }
                setFetchBusy(btnFetchNse, progressFetch, tvFetchStatus, false, symbol);
                tvFetchStatus.setVisibility(View.GONE);
                showError(tvError, message + "\n" + getString(R.string.fetch_failed));
            }
        });
    }

    private String buildLiveStatus(Quote quote) {
        StringBuilder status = new StringBuilder();
        if (!quote.getCompanyName().isEmpty()) {
            status.append(quote.getCompanyName()).append("  \u00b7  ");
        }
        status.append(money(quote.getLastPrice()));
        if (quote.getPercentChange() != 0d) {
            status.append("  (").append(TrendEngine.formatPercent(quote.getPercentChange())).append(")");
        }
        if (quote.getLastUpdateTime() != null && !quote.getLastUpdateTime().isEmpty()) {
            status.append("  \u00b7  ").append(quote.getLastUpdateTime());
        }
        return status.toString();
    }


    private void saveFromDialog(AlertDialog dialog, EditText etSymbol, EditText etPrice,
                                TextView tvError, long[] selectedDate,
                                boolean liveFetched, String liveCompany) {

        String symbol = TrendEngine.normalizeSymbol(etSymbol.getText().toString());
        if (symbol.isEmpty()) {
            showError(tvError, getString(R.string.err_symbol));
            return;
        }
        if (!TrendEngine.isValidSymbol(symbol)) {
            showError(tvError, getString(R.string.err_symbol_chars));
            return;
        }
        double price = TrendEngine.parsePrice(etPrice.getText().toString());
        if (Double.isNaN(price)) {
            showError(tvError, getString(R.string.err_price));
            return;
        }
        if (selectedDate[0] > Dates.today()) {
            showError(tvError, getString(R.string.err_date_future));
            return;
        }

        String company = liveFetched && !liveCompany.isEmpty()
                ? liveCompany
                : repository.findCompanyName(symbol);
        String source = liveFetched ? StockPrice.SOURCE_NSE : StockPrice.SOURCE_MANUAL;

        boolean replaced = hasSameDayEntry(symbol, selectedDate[0]);
        repository.save(new StockPrice(UUID.randomUUID().toString(), symbol, company, price,
                selectedDate[0], source, System.currentTimeMillis()));

        Toast.makeText(this, replaced ? R.string.entry_updated : R.string.entry_saved,
                Toast.LENGTH_SHORT).show();
        dialog.dismiss();
        refreshSymbols(symbol);
    }

    private boolean hasSameDayEntry(String symbol, long date) {
        String key = Dates.key(date);
        for (StockPrice entry : repository.getBySymbol(symbol)) {
            if (Dates.key(entry.getDateMillis()).equals(key)) {
                return true;
            }
        }
        return false;
    }

    private TextView buildChip(final String symbol, final EditText target) {
        TextView chip = new TextView(this);
        chip.setText(symbol);
        chip.setTextSize(12f);
        chip.setTextColor(ContextCompat.getColor(this, R.color.chip_text));
        chip.setBackgroundResource(R.drawable.bg_chip);
        chip.setPadding(dp(12f), dp(6f), dp(12f), dp(6f));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMarginEnd(dp(6f));
        chip.setLayoutParams(params);

        chip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                target.setText(symbol);
                target.setSelection(symbol.length());
            }
        });
        return chip;
    }

    private void showDatePicker(final long[] holder, final EditText field) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(holder[0]);

        DatePickerDialog picker = new DatePickerDialog(this,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        Calendar chosen = Calendar.getInstance();
                        chosen.set(year, month, dayOfMonth, 0, 0, 0);
                        chosen.set(Calendar.MILLISECOND, 0);
                        holder[0] = chosen.getTimeInMillis();
                        field.setText(Dates.pretty(holder[0]));
                    }
                }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));

        picker.getDatePicker().setMaxDate(System.currentTimeMillis());
        picker.show();
    }

    private void setFetchBusy(MaterialButton button, ProgressBar bar, TextView status,
                              boolean busy, String symbol) {
        button.setEnabled(!busy);
        bar.setVisibility(busy ? View.VISIBLE : View.GONE);
        if (busy) {
            status.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            status.setText(getString(R.string.fetching_symbol, symbol));
            status.setVisibility(View.VISIBLE);
        }
    }

    private void showError(TextView view, String message) {
        view.setText(message);
        view.setVisibility(View.VISIBLE);
    }

    private void hideError(TextView view) {
        view.setVisibility(View.GONE);
    }

    private String plainNumber(double value) {
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager manager =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (manager == null) {
            return true;
        }
        NetworkInfo info = manager.getActiveNetworkInfo();
        return info != null && info.isConnected();
    }


    /** Toolbar action: pull today's live NSE price for the selected symbol. */
    private void fetchLiveForSelected() {
        final String symbol = getSelectedSymbol();
        if (symbol.isEmpty()) {
            showAddDialog("");
            return;
        }
        if (!isNetworkAvailable()) {
            Toast.makeText(this, R.string.err_no_network, Toast.LENGTH_SHORT).show();
            return;
        }

        setLiveBusy(true);
        nseClient.fetchQuote(symbol, new NseApiClient.Callback() {
            @Override
            public void onSuccess(Quote quote) {
                if (isFinishing()) {
                    return;
                }
                setLiveBusy(false);

                String company = quote.getCompanyName().isEmpty()
                        ? repository.findCompanyName(symbol)
                        : quote.getCompanyName();
                long today = Dates.today();
                boolean replaced = hasSameDayEntry(symbol, today);

                repository.save(new StockPrice(UUID.randomUUID().toString(), symbol, company,
                        quote.getLastPrice(), today, StockPrice.SOURCE_NSE,
                        System.currentTimeMillis()));

                Toast.makeText(MainActivity.this,
                        getString(replaced ? R.string.entry_updated : R.string.entry_saved)
                                + " \u00b7 " + money(quote.getLastPrice()),
                        Toast.LENGTH_SHORT).show();
                refreshSymbols(symbol);
            }

            @Override
            public void onError(final String message) {
                if (isFinishing()) {
                    return;
                }
                setLiveBusy(false);
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle(R.string.fetch_failed_title)
                        .setMessage(message)
                        .setPositiveButton(R.string.action_add_manually,
                                (dialog, which) -> showAddDialog(symbol))
                        .setNegativeButton(R.string.action_close, null)
                        .show();
            }
        });
    }

    private void setLiveBusy(boolean busy) {
        progressLive.setVisibility(busy ? View.VISIBLE : View.GONE);
        ImageButton button = findViewById(R.id.btnFetchLive);
        button.setEnabled(!busy);
    }

    private void confirmDelete(final StockPrice entry) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.confirm_delete_title)
                .setMessage(getString(R.string.confirm_delete_msg) + "\n\n"
                        + entry.getSymbol() + "  \u00b7  " + Dates.pretty(entry.getDateMillis())
                        + "  \u00b7  " + money(entry.getPrice()))
                .setPositiveButton(R.string.delete_entry, (dialog, which) -> {
                    String symbol = entry.getSymbol();
                    repository.delete(entry.getId());
                    Toast.makeText(MainActivity.this, R.string.entry_deleted,
                            Toast.LENGTH_SHORT).show();
                    refreshSymbols(symbol);
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void showEntryDetails(final StockPrice entry) {
        String source = entry.isFromNse() ? getString(R.string.badge_live)
                : entry.isSample() ? getString(R.string.badge_sample)
                : getString(R.string.badge_manual);

        String message = getString(R.string.detail_company) + ": "
                + (entry.hasCompanyName() ? entry.getCompanyName()
                : getString(R.string.company_unknown))
                + "\n" + getString(R.string.detail_price) + ": " + money(entry.getPrice())
                + "\n" + getString(R.string.detail_date) + ": " + Dates.pretty(entry.getDateMillis())
                + "\n" + getString(R.string.detail_source) + ": " + source;

        new AlertDialog.Builder(this)
                .setTitle(entry.getSymbol() + "  \u00b7  " + money(entry.getPrice()))
                .setMessage(message)
                .setPositiveButton(R.string.delete_entry,
                        (dialog, which) -> confirmDelete(entry))
                .setNegativeButton(R.string.action_close, null)
                .show();
    }
}
