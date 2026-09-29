package com.stocktrend.app.net;

import android.os.Handler;
import android.os.Looper;

import com.stocktrend.app.engine.TrendEngine;

import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Minimal client for the open (no API key) NSE India equity endpoints.
 *
 * <p>NSE serves these routes only to browser-like sessions, so the client keeps
 * a {@link CookieManager}, warms it up by loading the public quote page and then
 * calls {@code /api/quote-equity}. If NSE rejects the first call the session is
 * rebuilt once and the call retried, which is what a browser refresh does.
 * There is no third party dependency: plain {@link HttpURLConnection}.</p>
 */
public class NseApiClient {

    private static final String HOST = "https://www.nseindia.com";
    private static final String QUOTE_PATH = "/api/quote-equity?symbol=";
    private static final String QUOTE_PAGE = "/get-quotes/equity?symbol=";

    private static final String USER_AGENT =
            "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 "
                    + "(KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36";

    private static final int TIMEOUT_MS = 15000;

    /** Result of one HTTP round trip. */
    private static class Response {
        final int code;
        final String body;

        Response(int code, String body) {
            this.code = code;
            this.body = body;
        }
    }

    /** Delivery target for the asynchronous fetch. */
    public interface Callback {
        void onSuccess(Quote quote);

        void onError(String message);
    }

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private boolean sessionPrimed = false;

    /** Fetches the quote off the main thread and always answers on the main thread. */
    public void fetchQuote(final String symbol, final Callback callback) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    final Quote quote = fetchQuoteSync(symbol);
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onSuccess(quote);
                        }
                    });
                } catch (final NseException e) {
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onError(e.getMessage());
                        }
                    });
                }
            }
        });
    }

    /** Blocking variant, usable from a background thread or a test harness. */
    public Quote fetchQuoteSync(String symbol) throws NseException {
        String normalized = TrendEngine.normalizeSymbol(symbol);
        if (!TrendEngine.isValidSymbol(normalized)) {
            throw new NseException("Enter a valid NSE symbol, e.g. RELIANCE");
        }

        String url = HOST + QUOTE_PATH + normalized;
        Exception failure = null;

        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                if (!sessionPrimed) {
                    primeSession(normalized);
                }
                Response response = get(url, HOST + QUOTE_PAGE + normalized);

                if (response.code == HttpURLConnection.HTTP_OK) {
                    Quote quote = NseQuoteParser.parse(response.body);
                    if (!quote.isUsable()) {
                        throw new NseException("NSE returned no price for " + normalized
                                + ". Check the symbol and try again.");
                    }
                    return quote;
                }

                if (response.code == HttpURLConnection.HTTP_NOT_FOUND) {
                    throw new NseException("NSE does not list the symbol " + normalized + ".");
                }

                if (response.code == HttpURLConnection.HTTP_FORBIDDEN
                        || response.code == HttpURLConnection.HTTP_UNAUTHORIZED) {
                    // Stale cookies: rebuild the browsing session and retry once.
                    sessionPrimed = false;
                    failure = new NseException("NSE refused the request (HTTP " + response.code
                            + "). The network or IP is probably blocked by NSE India, so please "
                            + "enter the price manually.");
                    continue;
                }

                throw new NseException("NSE replied with HTTP " + response.code + ".");
            } catch (NseException e) {
                throw e;
            } catch (JSONException e) {
                throw new NseException("Could not read the NSE response.", e);
            } catch (Exception e) {
                failure = e;
                sessionPrimed = false;
            }
        }

        throw new NseException("Could not reach NSE India. Check your connection and try again.",
                failure);
    }

    private void primeSession(String symbol) throws IOException {
        CookieHandler handler = CookieHandler.getDefault();
        if (!(handler instanceof CookieManager)) {
            CookieManager manager = new CookieManager();
            manager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
            CookieHandler.setDefault(manager);
        }
        // The public quote page hands out the nsit / nseappid cookies the API expects.
        get(HOST + QUOTE_PAGE + symbol, HOST + "/");
        sessionPrimed = true;
    }

    private Response get(String url, String referer) throws IOException {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(TIMEOUT_MS);
            connection.setReadTimeout(TIMEOUT_MS);
            connection.setInstanceFollowRedirects(true);
            connection.setRequestProperty("User-Agent", USER_AGENT);
            connection.setRequestProperty("Accept", "*/*");
            connection.setRequestProperty("Accept-Language", "en-US,en;q=0.9");
            connection.setRequestProperty("Referer", referer);
            connection.setRequestProperty("Connection", "keep-alive");
            connection.setUseCaches(false);

            int code = connection.getResponseCode();
            InputStream stream = code >= 400
                    ? connection.getErrorStream()
                    : connection.getInputStream();
            return new Response(code, readBody(stream));
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private String readBody(InputStream stream) throws IOException {
        if (stream == null) {
            return "";
        }
        StringBuilder body = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                body.append(line);
            }
        } finally {
            reader.close();
        }
        return body.toString();
    }

    /** Releases the background thread; call it from {@code Activity#onDestroy}. */
    public void shutdown() {
        executor.shutdownNow();
    }
}
