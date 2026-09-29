package com.stocktrend.app.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.stocktrend.app.R;
import com.stocktrend.app.data.StockPrice;
import com.stocktrend.app.engine.TrendEngine;
import com.stocktrend.app.util.Dates;

import java.util.ArrayList;
import java.util.List;

/**
 * Renders the stored price history of the selected symbol, newest first.
 *
 * <p>Each row compares its price with the next older reading in the list, so the
 * "change" column always shows the move against the previous observation.</p>
 */
public class StockAdapter extends RecyclerView.Adapter<StockAdapter.ViewHolder> {

    /** Row interactions handled by {@code MainActivity}. */
    public interface OnEntryListener {
        void onEntryClick(StockPrice entry);

        void onEntryDelete(StockPrice entry);
    }

    private final List<StockPrice> entries = new ArrayList<>();
    private final OnEntryListener listener;

    public StockAdapter(OnEntryListener listener) {
        this.listener = listener;
    }

    /**
     * Replaces the displayed rows.
     *
     * <p>The list is published with range notifications rather than
     * {@code notifyDataSetChanged} so the {@link RecyclerView} keeps its scroll
     * position when a single price is added or removed.</p>
     */
    public void submit(List<StockPrice> items) {
        int previousSize = entries.size();
        entries.clear();
        entries.addAll(items);
        int newSize = entries.size();

        if (previousSize > newSize) {
            notifyItemRangeRemoved(newSize, previousSize - newSize);
        }
        if (newSize > 0) {
            notifyItemRangeChanged(0, newSize);
        }
        if (newSize > previousSize) {
            notifyItemRangeInserted(previousSize, newSize - previousSize);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_stock_price, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        final StockPrice entry = entries.get(position);
        Context context = holder.itemView.getContext();

        holder.tvDate.setText(Dates.pretty(entry.getDateMillis()));
        holder.tvPrice.setText(context.getString(R.string.price_currency,
                TrendEngine.formatPrice(entry.getPrice())));
        holder.tvMeta.setText(entry.hasCompanyName() ? entry.getCompanyName() : entry.getSymbol());
        bindBadge(holder, entry);

        StockPrice older = position + 1 < entries.size() ? entries.get(position + 1) : null;
        if (older == null) {
            holder.tvChange.setText("");
            holder.tvChange.setVisibility(View.INVISIBLE);
        } else {
            double change = entry.getPrice() - older.getPrice();
            double percent = older.getPrice() == 0d ? 0d : change / older.getPrice() * 100d;
            holder.tvChange.setVisibility(View.VISIBLE);
            holder.tvChange.setText(context.getString(R.string.change_with_percent,
                    TrendEngine.formatChange(change), TrendEngine.formatPercent(percent)));
            holder.tvChange.setTextColor(ContextCompat.getColor(context,
                    change > 0d ? R.color.trend_up
                            : change < 0d ? R.color.trend_down : R.color.trend_flat));
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onEntryClick(entry);
                }
            }
        });

        holder.btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onEntryDelete(entry);
                }
            }
        });
    }

    private void bindBadge(ViewHolder holder, StockPrice entry) {
        int background;
        int textColor;
        String label;

        if (entry.isFromNse()) {
            background = R.drawable.bg_badge_live;
            textColor = R.color.badge_live_text;
            label = holder.itemView.getContext().getString(R.string.badge_live);
        } else if (entry.isSample()) {
            background = R.drawable.bg_badge_sample;
            textColor = R.color.badge_sample_text;
            label = holder.itemView.getContext().getString(R.string.badge_sample);
        } else {
            background = R.drawable.bg_badge_manual;
            textColor = R.color.chip_text;
            label = holder.itemView.getContext().getString(R.string.badge_manual);
        }

        holder.tvBadge.setBackgroundResource(background);
        holder.tvBadge.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), textColor));
        holder.tvBadge.setText(label);
    }

    @Override
    public int getItemCount() {
        return entries.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvDate;
        final TextView tvBadge;
        final TextView tvMeta;
        final TextView tvPrice;
        final TextView tvChange;
        final ImageButton btnDelete;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tvEntryDate);
            tvBadge = itemView.findViewById(R.id.tvEntryBadge);
            tvMeta = itemView.findViewById(R.id.tvEntryMeta);
            tvPrice = itemView.findViewById(R.id.tvEntryPrice);
            tvChange = itemView.findViewById(R.id.tvEntryChange);
            btnDelete = itemView.findViewById(R.id.btnDeleteEntry);
        }
    }
}
