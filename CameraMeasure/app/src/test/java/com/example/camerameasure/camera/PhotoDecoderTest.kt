package com.example.camerameasure.camera

import org.junit.Assert.assertEquals
import org.junit.Test

/** The down-sampling maths of [PhotoDecoder] is pure Kotlin and therefore unit testable. */
class PhotoDecoderTest {

    @Test
    fun `12 MP photos are decoded at half size`() {
        // 4032 / 2 = 2016, the closest power-of-two sample size that stays above 2048? No:
        // 2016 is below 2048, so the next smaller factor (2) is the best available match.
        assertEquals(2, PhotoDecoder.sampleSizeFor(4032, 3024, 2048))
    }

    @Test
    fun `small photos are not down-sampled`() {
        assertEquals(1, PhotoDecoder.sampleSizeFor(1920, 1080, 2048))
        assertEquals(1, PhotoDecoder.sampleSizeFor(2048, 1536, 2048))
    }

    @Test
    fun `very large photos are down-sampled to the next power of two`() {
        assertEquals(4, PhotoDecoder.sampleSizeFor(8000, 6000, 2048))
    }

    @Test
    fun `the longest edge decides the sample size`() {
        assertEquals(2, PhotoDecoder.sampleSizeFor(3024, 4032, 2048))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `a non positive maximum dimension is rejected`() {
        PhotoDecoder.sampleSizeFor(1000, 1000, 0)
    }
}
