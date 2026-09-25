package com.example.pingpong

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.sqrt

class BallTest {

    @Test
    fun `reset places ball at center with non-zero vertical velocity`() {
        val ball = Ball()
        ball.reset(500f, 800f, serveDown = true)

        assertEquals(500f, ball.x, 0.001f)
        assertEquals(800f, ball.y, 0.001f)
        assertTrue(ball.velocityY > 0f)
        assertTrue(abs(ball.velocityX) < ball.baseSpeed)
    }

    @Test
    fun `reset serve direction follows serveDown flag`() {
        val ball = Ball()
        repeat(20) {
            ball.reset(0f, 0f, serveDown = false)
            assertTrue(ball.velocityY < 0f)
        }
    }

    @Test
    fun `update integrates position with velocity`() {
        val ball = Ball(x = 100f, y = 200f)
        ball.velocityX = 300f
        ball.velocityY = -400f
        ball.update(0.5f)

        assertEquals(250f, ball.x, 0.001f)
        assertEquals(0f, ball.y, 0.001f)
    }

    @Test
    fun `bounceX reflects horizontal velocity`() {
        val ball = Ball()
        ball.velocityX = 350f
        ball.bounceX()
        assertEquals(-350f, ball.velocityX, 0.001f)
    }

    @Test
    fun `paddle bounce sends ball in requested direction`() {
        val ball = Ball()
        ball.reset(0f, 0f, serveDown = true)

        ball.bounceOffPaddle(relativeHit = 0f, goingDown = false)
        assertTrue(ball.velocityY < 0f)

        ball.bounceOffPaddle(relativeHit = 0f, goingDown = true)
        assertTrue(ball.velocityY > 0f)
    }

    @Test
    fun `edge hits produce stronger horizontal angle than center hits`() {
        val center = Ball().apply { bounceOffPaddle(0f, goingDown = false) }
        val edge = Ball().apply { bounceOffPaddle(1f, goingDown = false) }
        assertTrue(abs(edge.velocityX) > abs(center.velocityX))
    }

    @Test
    fun `rallies accelerate but never exceed max speed`() {
        val ball = Ball()
        repeat(100) { ball.bounceOffPaddle(0.3f, goingDown = true) }
        assertTrue(ball.speed() <= ball.maxSpeed + 0.001f)
    }

    @Test
    fun `vertical component never stalls the rally`() {
        val ball = Ball()
        // Extreme edge hit: angle clamp must still leave vertical motion.
        ball.bounceOffPaddle(1f, goingDown = true)
        assertTrue(abs(ball.velocityY) > 0.2f * ball.speed())
        assertTrue(sqrt(ball.velocityX * ball.velocityX + ball.velocityY * ball.velocityY) > 0f)
    }
}
