# StockTrend

A small Android app for tracking NSE India stock prices and their trend.

- Add a stock name, its price and the date by hand, **or**
- pull the *current* price straight from the open NSE India quote endpoint.
- Every saved observation is plotted on a trend chart, with last / change / low /
  high / average statistics for the selected symbol.

## Features

| Screen element | What it does |
| --- | --- |
| Stock spinner | Switches the chart and history between the symbols you have stored |
| Add Price (＋) | Symbol, price and date; a "Fetch live from NSE" button fills the price |
| Fetch live (⟳) | Saves today's NSE quote for the selected symbol in one tap |
| Trend chart | Custom `View`: scaled Y axis, shaded area under the line, emphasised latest point |
| Price history | Newest first, with the change against the previous reading and a source badge |

Prices are stored on the device only (`SharedPreferences` + JSON), grouped to one
observation per symbol per calendar day — saving a second price for the same day
replaces the first instead of adding a duplicate point to the chart.

### Source badges

| Badge | Meaning |
| --- | --- |
| `NSE` | Price came from the live NSE India quote |
| `Manual` | Price was typed by the user |
| `Sample` | Part of the demo data set seeded on first launch |

On a clean install a small demo set (RELIANCE, TCS, INFY) is seeded so the chart
is not empty. It is clearly badged and can be deleted like any other entry.

## NSE India integration

No API key and no third-party SDK — plain `HttpURLConnection` in
`net/NseApiClient.java`:

1. `GET https://www.nseindia.com/get-quotes/equity?symbol=<SYM>` warms up a
   `CookieManager` (NSE expects the `nsit` / `nseappid` cookies).
2. `GET https://www.nseindia.com/api/quote-equity?symbol=<SYM>` with a browser
   `User-Agent` and a `Referer` header returns the quote JSON.
3. A 401/403 rebuilds the session once and retries; a 404 or an unusable body is
   reported back to the user in plain language.

`net/NseQuoteParser.java` maps that JSON defensively (NSE renames keys between
releases, so `lastPrice → close → previousClose` fallbacks and string-number
handling are all covered by `NseQuoteParserTest`).

> NSE blocks many datacenter/VPN IP ranges with HTTP 403. The app detects that,
> says so, and offers to record the price manually instead — the offline flow
> always works.

## Layout of the code

```
app/src/main/java/com/stocktrend/app/
├── data/     StockPrice, StockRepository (SharedPreferences + JSON), SampleData
├── engine/   TrendEngine — ordering, de-duplication, statistics, formatting
├── net/      NseApiClient, NseQuoteParser, Quote, NseException
├── ui/       MainActivity, StockAdapter, TrendChartView (custom canvas chart)
└── util/     Dates
```

`TrendEngine`, `NseQuoteParser`, `Quote`, `StockPrice` and `Dates` have no Android
imports, which is what lets the whole calculation layer be unit-tested on the JVM.

## Build

```bash
cd StockTracker
./gradlew :app:testDebugUnitTest     # 19 tests: engine, NSE parser, screen smoke test
./gradlew :app:assembleDebug         # -> app/build/outputs/apk/debug/app-debug.apk
```

The suite covers:

| File | Tests | What it pins down |
| --- | --- | --- |
| `TrendEngineTest` | 11 | ordering, per-day de-duplication, statistics, symbol/price validation, Indian digit grouping |
| `NseQuoteParserTest` | 4 | the real NSE payload shape, key fallbacks, numeric strings, empty responses |
| `MainActivityTest` | 4 | Robolectric: layout inflation, sample seeding, statistics binding, chart rasterisation (`GraphicsMode.NATIVE`), the add-price dialog saving a normalised entry, history list |

Requirements: JDK 17, Android SDK with `platforms;android-34` and
`build-tools;34.0.0`. Point the build at the SDK with `local.properties`
(`sdk.dir=...`) or `ANDROID_HOME`.

Toolchain: AGP 8.2.2 / Gradle 8.5, `minSdk 24`, `targetSdk 34`, Java 17.
Dependencies: AppCompat, Material Components, ConstraintLayout, RecyclerView,
CardView, JUnit. No charting or HTTP library.

A prebuilt debug APK is published to `../Ready APK/StockTrend.apk`.
