package com.stocktrend.app.net;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class NseQuoteParserTest {

    /** Shape of a real {@code /api/quote-equity?symbol=RELIANCE} response. */
    private static final String RELIANCE_PAYLOAD = "{"
            + "\"info\":{"
            + "  \"symbol\":\"RELIANCE\","
            + "  \"companyName\":\"Reliance Industries Limited\","
            + "  \"industry\":\"Refineries\","
            + "  \"series\":\"EQ\"},"
            + "\"metadata\":{"
            + "  \"series\":\"EQ\","
            + "  \"symbol\":\"RELIANCE\","
            + "  \"lastUpdateTime\":\"28-Sep-2026 15:30:00\"},"
            + "\"securityInfo\":{"
            + "  \"symbol\":\"RELIANCE\","
            + "  \"companyName\":\"Reliance Industries Limited\"},"
            + "\"priceInfo\":{"
            + "  \"lastPrice\":3055.9,"
            + "  \"change\":45.75,"
            + "  \"pChange\":1.52,"
            + "  \"previousClose\":3010.15,"
            + "  \"open\":3015.0,"
            + "  \"close\":3055.9,"
            + "  \"intraDayHighLow\":{\"min\":3008.5,\"max\":3062.3,\"value\":3055.9}}"
            + "}";

    @Test
    public void parsesTheFullQuotePayload() throws Exception {
        Quote quote = NseQuoteParser.parse(RELIANCE_PAYLOAD);

        assertEquals("RELIANCE", quote.getSymbol());
        assertEquals("Reliance Industries Limited", quote.getCompanyName());
        assertEquals("EQ", quote.getSeries());
        assertEquals("28-Sep-2026 15:30:00", quote.getLastUpdateTime());

        assertEquals(3055.9d, quote.getLastPrice(), 0.001d);
        assertEquals(3010.15d, quote.getPreviousClose(), 0.001d);
        assertEquals(3015.0d, quote.getOpen(), 0.001d);
        assertEquals(3062.3d, quote.getDayHigh(), 0.001d);
        assertEquals(3008.5d, quote.getDayLow(), 0.001d);
        assertEquals(45.75d, quote.getChange(), 0.001d);
        assertEquals(1.52d, quote.getPercentChange(), 0.001d);
        assertTrue(quote.isUsable());
    }

    @Test
    public void fallsBackToCloseAndDerivesTheChange() throws Exception {
        String payload = "{"
                + "\"info\":{\"symbol\":\"INFY\"},"
                + "\"metadata\":{\"symbol\":\"INFY\",\"previousClose\":2400.0},"
                + "\"priceInfo\":{\"close\":2500.0}"
                + "}";

        Quote quote = NseQuoteParser.parse(payload);

        assertEquals("INFY", quote.getSymbol());
        // No company name in the payload: the symbol is used instead.
        assertEquals("INFY", quote.getCompanyName());
        assertEquals(2500d, quote.getLastPrice(), 0.001d);
        assertEquals(2400d, quote.getPreviousClose(), 0.001d);
        assertEquals(100d, quote.getChange(), 0.001d);
        assertEquals(4.1667d, quote.getPercentChange(), 0.001d);
    }

    @Test
    public void readsNumericStringsToo() throws Exception {
        String payload = "{\"priceInfo\":{\"lastPrice\":\"1,234.55\",\"pChange\":\"2.5\"}}";

        Quote quote = NseQuoteParser.parse(payload);

        assertEquals(1234.55d, quote.getLastPrice(), 0.001d);
        assertEquals(2.5d, quote.getPercentChange(), 0.001d);
    }

    @Test
    public void emptyPayloadIsNotUsable() throws Exception {
        Quote quote = NseQuoteParser.parse("{\"info\":{},\"priceInfo\":{}}");

        assertEquals("", quote.getSymbol());
        assertEquals(0d, quote.getLastPrice(), 0.001d);
        assertFalse(quote.isUsable());
    }
}
