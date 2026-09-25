package com.notecalc.app.engine;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.*;

/**
 * Result of evaluating a single line.
 */
public class LineResult {
    private final int lineNumber;
    private final String originalText;
    private final String variableAssigned; // e.g. "rent", or null if not an assignment
    private final Double numericValue;      // null if purely a comment / text with no math
    private final String formattedResult;  // e.g. "1,250", "$ 1,250", "Error", or ""
    private final boolean isError;
    private final String errorMessage;
    private final boolean isHeader;

    public LineResult(int lineNumber, String originalText, String variableAssigned,
                      Double numericValue, String formattedResult, boolean isError,
                      String errorMessage, boolean isHeader) {
        this.lineNumber = lineNumber;
        this.originalText = originalText;
        this.variableAssigned = variableAssigned;
        this.numericValue = numericValue;
        this.formattedResult = formattedResult;
        this.isError = isError;
        this.errorMessage = errorMessage;
        this.isHeader = isHeader;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public String getOriginalText() {
        return originalText;
    }

    public String getVariableAssigned() {
        return variableAssigned;
    }

    public Double getNumericValue() {
        return numericValue;
    }

    public String getFormattedResult() {
        return formattedResult;
    }

    public boolean isError() {
        return isError;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public boolean isHeader() {
        return isHeader;
    }

    public boolean hasResult() {
        return formattedResult != null && !formattedResult.isEmpty();
    }
}
