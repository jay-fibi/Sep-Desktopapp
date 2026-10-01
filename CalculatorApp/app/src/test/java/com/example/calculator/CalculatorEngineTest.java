package com.example.calculator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

public class CalculatorEngineTest {

    private CalculatorEngine engine;

    @Before
    public void setUp() {
        engine = new CalculatorEngine();
    }

    /** Types the characters of an expression, e.g. type("12+3"). */
    private void type(String expr) {
        for (int i = 0; i < expr.length(); i++) {
            char c = expr.charAt(i);
            if (Character.isDigit(c)) {
                engine.inputDigit(c - '0');
            } else if (c == '.') {
                engine.inputDot();
            } else {
                engine.inputOperator(c);
            }
        }
    }

    @Test
    public void simpleAddition() {
        type("12+7");
        engine.evaluate();
        assertEquals("19", engine.getDisplayExpression());
        assertTrue(engine.isShowingResult());
    }

    @Test
    public void operatorPrecedenceMultiplicationFirst() {
        type("2+3*4");
        engine.evaluate();
        assertEquals("14", engine.getDisplayExpression());
    }

    @Test
    public void divisionProducesDecimal() {
        type("7/2");
        engine.evaluate();
        assertEquals("3.5", engine.getDisplayExpression());
    }

    @Test
    public void divisionByZeroShowsError() {
        type("5/0");
        engine.evaluate();
        assertTrue(engine.isError());
    }

    @Test
    public void subtractAndNegativeResults() {
        type("3-10");
        engine.evaluate();
        assertEquals("−7", engine.getDisplayExpression());
    }

    @Test
    public void unaryMinusAtStart() {
        type("-5+8");
        engine.evaluate();
        assertEquals("3", engine.getDisplayExpression());
    }

    @Test
    public void negativeFactorAfterOperator() {
        type("6*-2");
        engine.evaluate();
        assertEquals("−12", engine.getDisplayExpression());
    }

    @Test
    public void operatorReplacedWhenPressedTwice() {
        type("5+");
        engine.inputOperator('*');
        assertEquals("5×", engine.getDisplayExpression());
    }

    @Test
    public void trailingOperatorIgnoredOnEquals() {
        type("9+");
        engine.evaluate();
        assertEquals("9", engine.getDisplayExpression());
    }

    @Test
    public void displayUsesPrettyGlyphs() {
        type("8/2*2-1");
        assertEquals("8÷2×2−1", engine.getDisplayExpression());
    }

    @Test
    public void percentHalvesByHundred() {
        type("50");
        engine.inputPercent();
        assertEquals("0.5", engine.getDisplayExpression());
    }

    @Test
    public void percentInsideExpression() {
        type("200+10");
        engine.inputPercent();
        engine.evaluate();
        assertEquals("200.1", engine.getDisplayExpression());
    }

    @Test
    public void toggleSignFlipsCurrentNumber() {
        type("25");
        engine.toggleSign();
        assertEquals("−25", engine.getDisplayExpression());
        engine.toggleSign();
        assertEquals("25", engine.getDisplayExpression());
    }

    @Test
    public void toggleSignOnlyAffectsLastNumber() {
        type("10+5");
        engine.toggleSign();
        assertEquals("10+−5", engine.getDisplayExpression());
    }

    @Test
    public void dotOnlyOncePerNumber() {
        type("3");
        engine.inputDot();
        type("14");
        engine.inputDot();
        type("5");
        assertEquals("3.145", engine.getDisplayExpression());
    }

    @Test
    public void dotAfterOperatorStartsZeroDot() {
        type("5+");
        engine.inputDot();
        type("5");
        assertEquals("5+0.5", engine.getDisplayExpression());
    }

    @Test
    public void leadingZeroIsReplaced() {
        type("0");
        type("7");
        assertEquals("7", engine.getDisplayExpression());
    }

    @Test
    public void backspaceRemovesLastChar() {
        type("123");
        engine.backspace();
        assertEquals("12", engine.getDisplayExpression());
    }

    @Test
    public void clearResetsEverything() {
        type("1+2");
        engine.evaluate();
        engine.clear();
        assertEquals("", engine.getDisplayExpression());
        assertFalse(engine.isShowingResult());
        assertFalse(engine.isError());
    }

    @Test
    public void digitAfterEqualsStartsNewCalculation() {
        type("2+2");
        engine.evaluate();
        type("5");
        assertEquals("5", engine.getDisplayExpression());
    }

    @Test
    public void operatorAfterEqualsContinuesFromResult() {
        type("2+2");
        engine.evaluate();
        engine.inputOperator('+');
        type("3");
        engine.evaluate();
        assertEquals("7", engine.getDisplayExpression());
    }

    @Test
    public void previewShowsLiveResult() {
        type("3*4+2");
        assertEquals("14", engine.getPreview());
    }

    @Test
    public void previewHiddenForPlainNumber() {
        type("123");
        assertNull(engine.getPreview());
    }

    @Test
    public void previewNullOnDivisionByZero() {
        type("1/0");
        assertNull(engine.getPreview());
    }

    @Test
    public void errorClearedByNextDigit() {
        type("5/0");
        engine.evaluate();
        assertTrue(engine.isError());
        type("8");
        assertFalse(engine.isError());
        assertEquals("8", engine.getDisplayExpression());
    }

    @Test
    public void floatNoiseIsTrimmed() {
        type("0.1+0.2");
        engine.evaluate();
        assertEquals("0.3", engine.getDisplayExpression());
    }

    @Test
    public void integerResultHasNoTrailingDotZero() {
        type("2*3");
        engine.evaluate();
        assertEquals("6", engine.getDisplayExpression());
    }

    @Test
    public void longChainKeepsPrecedence() {
        type("2+3*4-10/5");
        engine.evaluate();
        assertEquals("12", engine.getDisplayExpression());
    }
}
