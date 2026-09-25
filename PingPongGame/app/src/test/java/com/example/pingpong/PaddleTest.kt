package com.example.pingpong

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PaddleTest {

    @Test
    fun `moveTo clamps paddle inside the court`() {
        val paddle = Paddle(width = 100f)
        paddle.moveTo(0f, minX = 0f, maxX = 1000f)
        assertEquals(50f, paddle.x, 0.001f)

        paddle.moveTo(9999f, minX = 0f, maxX = 1000f)
        assertEquals(950f, paddle.x, 0.001f)

        paddle.moveTo(500f, minX = 0f, maxX = 1000f)
        assertEquals(500f, paddle.x, 0.001f)
    }

    @Test
    fun `moveToward steps are limited by speed and time`() {
        val paddle = Paddle(x = 500f).apply { aiSpeed = 100f }
        paddle.moveToward(900f, deltaTime = 1f, minX = 0f, maxX = 1000f)
        assertEquals(600f, paddle.x, 0.001f)

        // Target within reach: lands exactly on it.
        paddle.moveToward(550f, deltaTime = 1f, minX = 0f, maxX = 1000f)
        assertEquals(550f, paddle.x, 0.001f)
    }

    @Test
    fun `circle intersection detects hits and misses`() {
        val paddle = Paddle(x = 500f, y = 100f, width = 200f, height = 20f)
        // Directly above the paddle center, touching its top edge.
        assertTrue(paddle.intersectsCircle(500f, 80f, 10f))
        // Near the corner, just within the radius.
        assertTrue(paddle.intersectsCircle(600f, 90f, 5f))
        // Far away: no collision.
        assertFalse(paddle.intersectsCircle(500f, 300f, 10f))
        assertFalse(paddle.intersectsCircle(800f, 100f, 10f))
    }

    @Test
    fun `edge geometry accessors are consistent`() {
        val paddle = Paddle(x = 200f, y = 50f, width = 100f, height = 20f)
        assertEquals(150f, paddle.left, 0.001f)
        assertEquals(250f, paddle.right, 0.001f)
        assertEquals(40f, paddle.top, 0.001f)
        assertEquals(60f, paddle.bottom, 0.001f)
    }
}
