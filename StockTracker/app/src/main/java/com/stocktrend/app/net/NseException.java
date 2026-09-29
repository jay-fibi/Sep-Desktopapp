package com.stocktrend.app.net;

/**
 * Raised when a live NSE quote could not be retrieved. The message is written
 * for the user, not for a log file.
 */
public class NseException extends Exception {

    public NseException(String message) {
        super(message);
    }

    public NseException(String message, Throwable cause) {
        super(message, cause);
    }
}
