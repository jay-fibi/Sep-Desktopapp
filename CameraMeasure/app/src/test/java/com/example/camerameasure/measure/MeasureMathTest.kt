package com.example.camerameasure.measure

import com.example.camerameasure.model.NormPoint
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for the reference-scale geometry. The values mirror real-world usage: a 12 MP
 * photo decoded to 2016 x 1512 px, calibrated with the long edge of an A4 sheet (297 mm).
 */
class MeasureMathTest {

    @Test
    fun `pixel distance uses the image size of each axis`() {
        val distance = MeasureMath.pixelDistance(NormPoint(0f, 0f), NormPoint(1f, 0f), 1000, 2000)
        assertEquals(1000f, distance, 0.001f)
    }

    @Test
    fun `pixel distance follows a 3-4-5 triangle`() {
        val distance = MeasureMath.pixelDistance(NormPoint(0f, 0f), NormPoint(1f, 1f), 300, 400)
        assertEquals(500f, distance, 0.001f)
    }

    @Test
    fun `a4 reference yields the expected scale`() {
        val millimetersPerPixel = MeasureMath.millimetersPerPixel(
            pixelDistance = 1000f,
            referenceMillimeters = 297f
        )
        assertEquals(0.297f, millimetersPerPixel, 1e-6f)
    }

    @Test
    fun `measured length uses the calibration factor`() {
        // Half of the decoded height (1512 px / 2 = 756 px) at 0.5 mm per pixel.
        val millimeters = MeasureMath.millimetersBetween(
            start = NormPoint(0.2f, 0.1f),
            end = NormPoint(0.2f, 0.6f),
            imageWidth = 1200,
            imageHeight = 1600,
            millimetersPerPixel = 0.5f
        )
        assertEquals(400f, millimeters, 0.001f)
    }

    @Test
    fun `measured length is independent of the zoom or preview resolution`() {
        val start = NormPoint(0.1f, 0.2f)
        val end = NormPoint(0.4f, 0.6f)
        val scale = MeasureMath.millimetersPerPixel(800f, 297f)

        val atHalfResolution = MeasureMath.millimetersBetween(start, end, 2016, 1512, scale)
        val atFullResolution = MeasureMath.millimetersBetween(start, end, 4032, 3024, scale)

        assertEquals(atFullResolution / 2f, atHalfResolution, 0.001f)
    }

    @Test
    fun `realistic measurement of a 400 px object`() {
        // A4 long edge spans 800 px, the object spans 400 px -> 148.5 mm.
        val scale = MeasureMath.millimetersPerPixel(800f, 297f)
        val objectWidth = 400f / 2016f
        val millimeters = MeasureMath.millimetersBetween(
            start = NormPoint(0.1f, 0.2f),
            end = NormPoint(0.1f + objectWidth, 0.2f),
            imageWidth = 2016,
            imageHeight = 1512,
            millimetersPerPixel = scale
        )
        assertEquals(148.5f, millimeters, 0.05f)
    }

    @Test
    fun `angle of a diagonal is 45 degrees`() {
        val angle = MeasureMath.angleDegrees(NormPoint(0f, 0f), NormPoint(1f, 1f), 100, 100)
        assertEquals(45f, angle, 0.001f)
    }

    @Test
    fun `angle of a vertical segment is 90 degrees`() {
        val angle = MeasureMath.angleDegrees(NormPoint(0.5f, 0.2f), NormPoint(0.5f, 0.8f), 100, 100)
        assertEquals(90f, angle, 0.001f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `coincident reference points are rejected`() {
        MeasureMath.millimetersPerPixel(pixelDistance = 0f, referenceMillimeters = 297f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `negative reference length is rejected`() {
        MeasureMath.millimetersPerPixel(pixelDistance = 100f, referenceMillimeters = -1f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `empty image size is rejected`() {
        MeasureMath.pixelDistance(NormPoint(0f, 0f), NormPoint(1f, 1f), 0, 0)
    }
}
