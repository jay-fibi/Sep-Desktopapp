package com.notecalc.app.engine;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class NoteCalcEngineTest {

    private NoteCalcEngine engine;

    @Before
    public void setUp() {
        engine = new NoteCalcEngine();
    }

    @Test
    public void testBasicArithmetic() {
        NoteCalcEngine.EvaluationResult res = engine.evaluate("10 + 20 * 3");
        assertEquals(1, res.getLineResults().size());
        assertEquals("70", res.getLineResults().get(0).getFormattedResult());
        assertEquals(70.0, res.getLineResults().get(0).getNumericValue(), 0.001);
    }

    @Test
    public void testParenthesesAndOrder() {
        NoteCalcEngine.EvaluationResult res = engine.evaluate("(10 + 20) * 3");
        assertEquals("90", res.getLineResults().get(0).getFormattedResult());
    }

    @Test
    public void testVariableAssignmentAndUsage() {
        String doc = "salary = 5000\n" +
                     "taxRate = 0.20\n" +
                     "tax = salary * taxRate\n" +
                     "net = salary - tax";
        NoteCalcEngine.EvaluationResult res = engine.evaluate(doc);
        assertEquals(4, res.getLineResults().size());
        assertEquals("5,000", res.getLineResults().get(0).getFormattedResult());
        assertEquals("0.2", res.getLineResults().get(1).getFormattedResult());
        assertEquals("1,000", res.getLineResults().get(2).getFormattedResult());
        assertEquals("4,000", res.getLineResults().get(3).getFormattedResult());
        assertEquals(4000.0, res.getFinalVariables().get("net"), 0.001);
    }

    @Test
    public void testNotesWithFigures() {
        String doc = "Monthly Budget\n" +
                     "Apartment rent: $1200\n" +
                     "Groceries: 350 + 50\n" +
                     "Utilities 150.50\n" +
                     "total";
        NoteCalcEngine.EvaluationResult res = engine.evaluate(doc);
        assertEquals(5, res.getLineResults().size());
        // line 0: title/note -> no result
        assertFalse(res.getLineResults().get(0).hasResult());
        // line 1: rent 1200
        assertEquals("1,200", res.getLineResults().get(1).getFormattedResult());
        // line 2: groceries 400
        assertEquals("400", res.getLineResults().get(2).getFormattedResult());
        // line 3: utilities 150.50
        assertEquals("150.5", res.getLineResults().get(3).getFormattedResult());
        // line 4: total = 1200 + 400 + 150.50 = 1750.5
        assertEquals("1,750.5", res.getLineResults().get(4).getFormattedResult());
    }

    @Test
    public void testPercentageMath() {
        String doc = "100 + 15%\n" +
                     "200 - 20%\n" +
                     "15% of 300";
        NoteCalcEngine.EvaluationResult res = engine.evaluate(doc);
        assertEquals("115", res.getLineResults().get(0).getFormattedResult());
        assertEquals("160", res.getLineResults().get(1).getFormattedResult());
        assertEquals("45", res.getLineResults().get(2).getFormattedResult());
    }

    @Test
    public void testMathFunctions() {
        String doc = "sqrt(144)\n" +
                     "round(3.14159, 2)\n" +
                     "max(10, 25)";
        NoteCalcEngine.EvaluationResult res = engine.evaluate(doc);
        assertEquals("12", res.getLineResults().get(0).getFormattedResult());
        assertEquals("3.14", res.getLineResults().get(1).getFormattedResult());
        assertEquals("25", res.getLineResults().get(2).getFormattedResult());
    }
}
