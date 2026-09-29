package com.stocktrend.app.data;

/**
 * A single stored price observation for one NSE stock symbol.
 *
 * <p>The class is deliberately free of Android imports so that the trend
 * calculations built on top of it can be exercised by plain JVM unit tests.</p>
 */
public class StockPrice {

    /** Price was typed in by the user. */
    public static final String SOURCE_MANUAL = "MANUAL";
    /** Price was pulled from the live NSE India quote endpoint. */
    public static final String SOURCE_NSE = "NSE";
    /** Price belongs to the bundled demo data set. */
    public static final String SOURCE_SAMPLE = "SAMPLE";

    private String id;
    private String symbol;
    private String companyName;
    private double price;
    /** Local midnight of the trading/observation day. */
    private long dateMillis;
    private String source;
    private long addedAt;

    public StockPrice(String id,
                      String symbol,
                      String companyName,
                      double price,
                      long dateMillis,
                      String source,
                      long addedAt) {
        this.id = id;
        this.symbol = symbol;
        this.companyName = companyName;
        this.price = price;
        this.dateMillis = dateMillis;
        this.source = source;
        this.addedAt = addedAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public long getDateMillis() {
        return dateMillis;
    }

    public void setDateMillis(long dateMillis) {
        this.dateMillis = dateMillis;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public long getAddedAt() {
        return addedAt;
    }

    public void setAddedAt(long addedAt) {
        this.addedAt = addedAt;
    }

    public boolean isFromNse() {
        return SOURCE_NSE.equals(source);
    }

    public boolean isSample() {
        return SOURCE_SAMPLE.equals(source);
    }

    public boolean hasCompanyName() {
        return companyName != null && !companyName.trim().isEmpty();
    }
}
