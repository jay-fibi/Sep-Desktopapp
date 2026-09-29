package com.stocktrend.app.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.stocktrend.app.util.Dates;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Persists user entered prices in {@link SharedPreferences} as a JSON array.
 *
 * <p>One {@link StockPrice} is stored per symbol per calendar day, so recording
 * a newer price for a day replaces the older reading instead of duplicating the
 * point on the trend chart.</p>
 */
public class StockRepository {

    private static final String PREF_NAME = "stocktrend_prefs";
    private static final String KEY_ENTRIES = "price_entries_json";

    private final SharedPreferences prefs;

    public StockRepository(Context context) {
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    /** All stored observations, newest first. */
    public List<StockPrice> getAll() {
        List<StockPrice> list = new ArrayList<>();
        String json = prefs.getString(KEY_ENTRIES, null);
        if (json == null || json.isEmpty()) {
            return list;
        }
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                list.add(new StockPrice(
                        obj.optString("id", UUID.randomUUID().toString()),
                        obj.optString("symbol", ""),
                        obj.optString("companyName", ""),
                        obj.optDouble("price", 0d),
                        obj.optLong("dateMillis", Dates.today()),
                        obj.optString("source", StockPrice.SOURCE_MANUAL),
                        obj.optLong("addedAt", System.currentTimeMillis())
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    /** All observations for one symbol (case insensitive), newest first. */
    public List<StockPrice> getBySymbol(String symbol) {
        List<StockPrice> result = new ArrayList<>();
        if (symbol == null) {
            return result;
        }
        for (StockPrice entry : getAll()) {
            if (symbol.equalsIgnoreCase(entry.getSymbol())) {
                result.add(entry);
            }
        }
        return result;
    }

    public boolean isEmpty() {
        return getAll().isEmpty();
    }

    /**
     * Inserts a new observation, or replaces the existing one that shares the
     * same symbol and calendar day.
     */
    public void save(StockPrice entry) {
        List<StockPrice> all = getAll();
        String entryKey = Dates.key(entry.getDateMillis());
        boolean replaced = false;

        for (int i = 0; i < all.size(); i++) {
            StockPrice existing = all.get(i);
            boolean sameSlot = existing.getSymbol().equalsIgnoreCase(entry.getSymbol())
                    && Dates.key(existing.getDateMillis()).equals(entryKey);
            if (sameSlot) {
                entry.setId(existing.getId());
                all.set(i, entry);
                replaced = true;
                break;
            }
        }
        if (!replaced) {
            all.add(0, entry);
        }
        saveAll(all);
    }

    public void delete(String id) {
        List<StockPrice> all = getAll();
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).getId().equals(id)) {
                all.remove(i);
                break;
            }
        }
        saveAll(all);
    }

    public void deleteSymbol(String symbol) {
        List<StockPrice> all = getAll();
        List<StockPrice> kept = new ArrayList<>();
        for (StockPrice entry : all) {
            if (!entry.getSymbol().equalsIgnoreCase(symbol)) {
                kept.add(entry);
            }
        }
        saveAll(kept);
    }

    public void clear() {
        prefs.edit().remove(KEY_ENTRIES).apply();
    }

    /** Last known company name for a symbol, or an empty string. */
    public String findCompanyName(String symbol) {
        if (symbol == null) {
            return "";
        }
        for (StockPrice entry : getAll()) {
            if (symbol.equalsIgnoreCase(entry.getSymbol()) && entry.hasCompanyName()) {
                return entry.getCompanyName();
            }
        }
        return "";
    }

    private void saveAll(List<StockPrice> entries) {
        try {
            JSONArray arr = new JSONArray();
            for (StockPrice item : entries) {
                JSONObject obj = new JSONObject();
                obj.put("id", item.getId());
                obj.put("symbol", item.getSymbol());
                obj.put("companyName", item.getCompanyName());
                obj.put("price", item.getPrice());
                obj.put("dateMillis", item.getDateMillis());
                obj.put("source", item.getSource());
                obj.put("addedAt", item.getAddedAt());
                arr.put(obj);
            }
            prefs.edit().putString(KEY_ENTRIES, arr.toString()).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
