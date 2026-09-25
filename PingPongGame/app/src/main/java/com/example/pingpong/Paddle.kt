package com.example.pingpong

/**
 * A rectangular paddle controlled either by the player (touch) or the AI.
 */
class Paddle(
    var x: Float = 0f,          // center x
    var y: Float = 0f,          // center y
    var width: Float = 220f,
    var height: Float = 28f
) {
    /** Pixels per second used by the AI when tracking the ball. */
    var aiSpeed: Float = 620f

    val left: Float get() = x - width / 2f
    val right: Float get() = x + width / 2f
    val top: Float get() = y - height / 2f
    val bottom: Float get() = y + height / 2f

    /** Moves the paddle center, clamped inside [minX]..[maxX]. */
    fun moveTo(targetX: Float, minX: Float, maxX: Float) {
        val half = width / 2f
        x = targetX.coerceIn(minX + half, maxX - half)
    }

    /** AI movement: step toward [targetX] limited by [aiSpeed] * [deltaTime]. */
    fun moveToward(targetX: Float, deltaTime: Float, minX: Float, maxX: Float) {
        val maxStep = aiSpeed * deltaTime
        val diff = targetX - x
        val step = diff.coerceIn(-maxStep, maxStep)
        moveTo(x + step, minX, maxX)
    }

    /** Checks overlap with a circle (the ball) using closest-point clamping. */
    fun intersectsCircle(cx: Float, cy: Float, radius: Float): Boolean {
        val closestX = cx.coerceIn(left, right)
        val closestY = cy.coerceIn(top, bottom)
        val dx = cx - closestX
        val dy = cy - closestY
        return dx * dx + dy * dy <= radius * radius
    }
}
