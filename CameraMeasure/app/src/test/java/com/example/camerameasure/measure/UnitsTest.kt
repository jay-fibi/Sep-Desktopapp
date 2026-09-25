package com.example.camerameasure.measure

import com.example.camerameasure.model.LengthUnit
import org.junit.Assert.assertEquals
import org.junit.Test

class UnitsTest {

    @Test
    fun `centimetres conversion`() {
        assertEquals(29.7f, Units.convert(297f, LengthUnit.CENTIMETERS), 1e-4f)
    }

    @Test
    fun `metres conversion`() {
        assertEquals(2.97f, Units.convert(2970f, LengthUnit.METERS), 1e-5f)
    }

    @Test
    fun `inches conversion uses the 25_4 mm definition`() {
        assertEquals(1f, Units.convert(Units.MILLIMETERS_PER_INCH, LengthUnit.INCHES), 1e-5f)
        assertEquals(0.5f, Units.convert(12.7f, LengthUnit.INCHES), 1e-5f)
        // Long edge of a bank card: 85.6 mm == 3.3701 in.
        assertEquals(3.3701f, Units.convert(85.6f, LengthUnit.INCHES), 1e-3f)
    }

    @Test
    fun `millimetres are passed through unchanged`() {
        assertEquals(150f, Units.convert(150f, LengthUnit.MILLIMETERS), 1e-6f)
    }

    @Test
    fun `formatting shows the unit and the configured number of decimals`() {
        assertEquals("29.70 cm", Units.format(297f, LengthUnit.CENTIMETERS))
        assertEquals("297.0 mm", Units.format(297f, LengthUnit.MILLIMETERS))
        assertEquals("2.9700 m", Units.format(2970f, LengthUnit.METERS))
        assertEquals("1.000 in", Units.format(25.4f, LengthUnit.INCHES))
    }

    @Test
    fun `preset lengths drop a redundant decimal part`() {
        assertEquals("297", Units.formatMillimeters(297f))
        assertEquals("120", Units.formatMillimeters(120f))
        assertEquals("53.98", Units.formatMillimeters(53.98f))
        assertEquals("279.4", Units.formatMillimeters(279.4f))
    }
}
