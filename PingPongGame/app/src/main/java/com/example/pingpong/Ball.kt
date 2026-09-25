package com.example.pingpong

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Represents the ball in the ping pong game.
 * Handles position, velocity and speed progression.
 */
class Ball(
    var x: Float = 0f,
    var y: Float = 0f,
    var radius: Float = 20f
) {
    var velocityX = 0f
    var velocityY = 0f

    /** Base speed in pixels per second, scaled with screen size. */
    var baseSpeed = 900f

    /** Maximum speed the ball can reach after rally acceleration. */
    var maxSpeed = 2200f

    /** Speed multiplier grows with every paddle hit to make rallies faster. */
    private var speedMultiplier = 1f
    private val speedGrowthPerHit = 1.06f
    private val maxMultiplier = 2.2f

    private val maxBounceAngle = Math.toRadians(60.0)

    /** Resets the ball to the center and serves toward [serveDown]. */
    fun reset(centerX: Float, centerY: Float, serveDown: Boolean) {
        x = centerX
        y = centerY
        speedMultiplier = 1f
        // Random horizontal launch angle between -35 and 35 degrees.
        val angle = Math.toRadians(Random.nextDouble(-35.0, 35.0))
        val direction = if (serveDown) 1f else -1f
        velocityX = (baseSpeed * sin(angle)).toFloat()
        velocityY = (baseSpeed * cos(angle)).toFloat() * direction
    }

    /** Advances the ball position by [deltaTime] seconds. */
    fun update(deltaTime: Float) {
        x += velocityX * deltaTime
        y += velocityY * deltaTime
    }

    /** Reflects the ball horizontally (side wall bounce). */
    fun bounceX() {
        velocityX = -velocityX
    }

    /**
     * Bounces the ball off a paddle. The bounce angle depends on where the
     * ball hit the paddle, giving the player control over the return angle.
     *
     * @param relativeHit position of the hit relative to the paddle center,
     *                    normalized to [-1, 1].
     * @param goingDown true when the ball should travel downward afterwards.
     */
    fun bounceOffPaddle(relativeHit: Float, goingDown: Boolean) {
        speedMultiplier = (speedMultiplier * speedGrowthPerHit).coerceAtMost(maxMultiplier)
        val clamped = relativeHit.coerceIn(-1f, 1f)
        val angle = clamped * maxBounceAngle
        val speed = (baseSpeed * speedMultiplier).coerceAtMost(maxSpeed)
        velocityX = (speed * sin(angle)).toFloat()
        velocityY = (speed * cos(angle)).toFloat() * if (goingDown) 1f else -1f
        // Guarantee a minimum vertical component so rallies cannot stall.
        val minVertical = speed * 0.45f
        if (abs(velocityY) < minVertical) {
            velocityY = minVertical * if (goingDown) 1f else -1f
        }
    }

    fun speed(): Float = kotlin.math.sqrt(velocityX * velocityX + velocityY * velocityY)
}
