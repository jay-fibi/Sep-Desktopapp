package com.stocktrend.app.net;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Maps the JSON payload of {@code https://www.nseindia.com/api/quote-equity}
 * onto a {@link Quote}.
 *
 * <p>NSE has shuffled these keys between releases, so every field is read
 * defensively with a fallback. Pure Java (only {@code org.json}) which keeps it
 * covered by the JVM unit tests.</p>
 */
public final class NseQuoteParser {

    private NseQuoteParser() {
    }

    /**
     * @param json raw response body from the NSE equity quote endpoint
     * @return the parsed quote; check {@link Quote#isUsable()} because NSE
     *         answers with empty objects for unknown symbols
     * @throws JSONException when the body is not a JSON object
     */
    public static Quote parse(String json) throws JSONException {
        JSONObject root = new JSONObject(json);

        JSONObject info = root.optJSONObject("info");
        JSONObject metadata = root.optJSONObject("metadata");
        JSONObject securityInfo = root.optJSONObject("securityInfo");
        JSONObject priceInfo = root.optJSONObject("priceInfo");

        Quote quote = new Quote();
        quote.setSymbol(firstNonEmpty(
                text(info, "symbol"),
                text(metadata, "symbol"),
                text(securityInfo, "symbol")));
        quote.setCompanyName(firstNonEmpty(
                text(info, "companyName"),
                text(securityInfo, "companyName"),
                quote.getSymbol()));
        quote.setSeries(firstNonEmpty(
                text(info, "series"),
                text(metadata, "series")));
        quote.setLastUpdateTime(text(metadata, "lastUpdateTime"));

        quote.setLastPrice(number(priceInfo, "lastPrice", number(priceInfo, "close", 0d)));
        quote.setPreviousClose(number(priceInfo, "previousClose", number(metadata, "previousClose", 0d)));
        quote.setOpen(number(priceInfo, "open", 0d));

        JSONObject highLow = priceInfo == null ? null : priceInfo.optJSONObject("intraDayHighLow");
        quote.setDayHigh(number(highLow, "max", 0d));
        quote.setDayLow(number(highLow, "min", 0d));

        double change = number(priceInfo, "change", Double.NaN);
        if (Double.isNaN(change) && quote.getPreviousClose() > 0d && quote.getLastPrice() > 0d) {
            change = quote.getLastPrice() - quote.getPreviousClose();
        }
        if (Double.isNaN(change)) {
            change = 0d;
        }
        quote.setChange(change);

        double percentChange = number(priceInfo, "pChange", Double.NaN);
        if (Double.isNaN(percentChange) && quote.getPreviousClose() > 0d) {
            percentChange = change / quote.getPreviousClose() * 100d;
        }
        quote.setPercentChange(Double.isNaN(percentChange) ? 0d : percentChange);

        return quote;
    }

    private static String text(JSONObject obj, String key) {
        if (obj == null || obj.isNull(key)) {
            return "";
        }
        String value = obj.optString(key, "");
        return value == null ? "" : value.trim();
    }

    private static double number(JSONObject obj, String key, double fallback) {
        if (obj == null || obj.isNull(key)) {
            return fallback;
        }
        Object raw = obj.opt(key);
        if (raw instanceof Number) {
            return ((Number) raw).doubleValue();
        }
        if (raw instanceof String) {
            try {
                return Double.parseDouble(((String) raw).replace(",", "").trim());
            } catch (NumberFormatException e) {
                return fallback;
            }
        }
        return fallback;
    }

    private static String firstNonEmpty(String... values) {
        for (String value : values) {
            if (value != null && !value.isEmpty()) {
                return value;
            }
        }
        return "";
    }
}
