package com.example.pingpong

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.Choreographer
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import kotlin.math.abs

/**
 * Custom view that runs the whole ping pong game: game loop (vsync-driven
 * via [Choreographer]), physics, AI opponent, input and rendering.
 *
 * The player controls the bottom paddle by dragging horizontally;
 * the AI defends the top. First to [winScore] points wins the match.
 */
class GameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs), Choreographer.FrameCallback {

    enum class State { READY, PLAYING, PAUSED, GAME_OVER }

    private var state = State.READY

    // --- Entities ---
    private val ball = Ball()
    private val playerPaddle = Paddle()
    private val aiPaddle = Paddle()

    // --- Score ---
    var playerScore = 0
        private set
    var aiScore = 0
        private set
    var winScore = 5
    var winningPlayer: Boolean? = null
        private set

    /** Listener notified when the score or game state changes. */
    var onScoreListener: ((player: Int, ai: Int) -> Unit)? = null

    // --- Difficulty ---
    enum class Difficulty(val aiSpeedFactor: Float, val aiErrorPx: Float) {
        EASY(0.55f, 90f),
        MEDIUM(0.8f, 45f),
        HARD(1.15f, 12f)
    }

    var difficulty = Difficulty.MEDIUM
        set(value) {
            field = value
            applyDifficulty()
        }

    val soundManager = SoundManager()

    // --- Game loop ---
    private var lastFrameNanos = 0L
    private var running = false

    // --- Paints ---
    private val tablePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.table_dark)
        style = Paint.Style.FILL
    }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.table_line)
        style = Paint.Style.STROKE
        strokeWidth = 6f
        pathEffect = DashPathEffect(floatArrayOf(28f, 24f), 0f)
    }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.table_line)
        style = Paint.Style.STROKE
        strokeWidth = 8f
    }
    private val playerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.player_paddle)
        style = Paint.Style.FILL
    }
    private val aiPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.ai_paddle)
        style = Paint.Style.FILL
    }
    private val ballPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.ball_color)
        style = Paint.Style.FILL
    }
    private val ballGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.ball_color)
        alpha = 60
        style = Paint.Style.FILL
    }
    private val scorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.score_text)
        textAlign = Paint.Align.CENTER
        alpha = 170
        isFakeBoldText = true
    }
    private val messagePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    private val subMessagePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.LTGRAY
        textAlign = Paint.Align.CENTER
    }
    private val overlayPaint = Paint().apply {
        color = Color.argb(140, 0, 0, 0)
        style = Paint.Style.FILL
    }

    // --- Layout ---
    private var courtLeft = 0f
    private var courtRight = 0f
    private var courtTop = 0f
    private var courtBottom = 0f
    private var touchTargetX: Float? = null

    init {
        setBackgroundColor(Color.BLACK)
        isFocusable = true
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val margin = w * 0.04f
        courtLeft = margin
        courtRight = w - margin
        courtTop = h * 0.06f
        courtBottom = h * 0.94f

        val scale = (w / 1080f).coerceIn(0.6f, 1.6f)
        ball.radius = 26f * scale
        ball.baseSpeed = h * 0.75f
        ball.maxSpeed = h * 1.9f

        playerPaddle.width = w * 0.22f
        playerPaddle.height = 30f * scale
        aiPaddle.width = w * 0.22f
        aiPaddle.height = playerPaddle.height

        playerPaddle.y = courtBottom - h * 0.05f
        aiPaddle.y = courtTop + h * 0.05f
        playerPaddle.moveTo(w / 2f, courtLeft, courtRight)
        aiPaddle.moveTo(w / 2f, courtLeft, courtRight)

        scorePaint.textSize = w * 0.12f
        messagePaint.textSize = w * 0.085f
        subMessagePaint.textSize = w * 0.045f

        applyDifficulty()
        ball.reset(w / 2f, h / 2f, serveDown = true)
    }

    private fun applyDifficulty() {
        val h = height.toFloat()
        aiPaddle.aiSpeed = if (h > 0) h * 0.55f * difficulty.aiSpeedFactor else 620f
    }

    // --- Public control ---

    fun startGame() {
        when (state) {
            State.READY, State.GAME_OVER -> {
                resetMatch()
                state = State.PLAYING
            }
            State.PAUSED -> state = State.PLAYING
            State.PLAYING -> Unit
        }
    }

    fun pauseGame() {
        if (state == State.PLAYING) state = State.PAUSED
    }

    fun resumeGame() {
        if (state == State.PAUSED) state = State.PLAYING
    }

    fun isPlaying(): Boolean = state == State.PLAYING

    fun resetMatch() {
        playerScore = 0
        aiScore = 0
        winningPlayer = null
        ball.reset(width / 2f, height / 2f, serveDown = kotlin.random.Random.nextBoolean())
        playerPaddle.moveTo(width / 2f, courtLeft, courtRight)
        aiPaddle.moveTo(width / 2f, courtLeft, courtRight)
        onScoreListener?.invoke(playerScore, aiScore)
    }

    // --- Game loop ---

    fun startLoop() {
        if (running) return
        running = true
        lastFrameNanos = 0L
        Choreographer.getInstance().postFrameCallback(this)
    }

    fun stopLoop() {
        running = false
        Choreographer.getInstance().removeFrameCallback(this)
    }

    override fun doFrame(frameTimeNanos: Long) {
        if (!running) return
        if (lastFrameNanos != 0L) {
            // Clamp dt to avoid huge jumps after a stall (e.g. app switch).
            val dt = ((frameTimeNanos - lastFrameNanos) / 1_000_000_000f)
                .coerceAtMost(0.033f)
            if (state == State.PLAYING) update(dt)
        }
        lastFrameNanos = frameTimeNanos
        invalidate()
        Choreographer.getInstance().postFrameCallback(this)
    }


    // --- Update logic ---

    private fun update(dt: Float) {
        // Player follows the finger target.
        touchTargetX?.let { target ->
            val maxStep = width * 2.4f * dt // fast but smooth tracking
            val diff = target - playerPaddle.x
            val step = diff.coerceIn(-maxStep, maxStep)
            playerPaddle.moveTo(playerPaddle.x + step, courtLeft, courtRight)
        }

        updateAi(dt)

        ball.update(dt)

        // Side walls.
        if (ball.x - ball.radius <= courtLeft && ball.velocityX < 0) {
            ball.x = courtLeft + ball.radius
            ball.bounceX()
            soundManager.playWallBounce()
        } else if (ball.x + ball.radius >= courtRight && ball.velocityX > 0) {
            ball.x = courtRight - ball.radius
            ball.bounceX()
            soundManager.playWallBounce()
        }

        // Paddle collisions.
        if (ball.velocityY > 0 &&
            playerPaddle.intersectsCircle(ball.x, ball.y, ball.radius) &&
            ball.y < playerPaddle.y
        ) {
            val relativeHit = (ball.x - playerPaddle.x) / (playerPaddle.width / 2f)
            ball.y = playerPaddle.top - ball.radius
            ball.bounceOffPaddle(relativeHit, goingDown = false)
            soundManager.playPaddleHit()
        } else if (ball.velocityY < 0 &&
            aiPaddle.intersectsCircle(ball.x, ball.y, ball.radius) &&
            ball.y > aiPaddle.y
        ) {
            val relativeHit = (ball.x - aiPaddle.x) / (aiPaddle.width / 2f)
            ball.y = aiPaddle.bottom + ball.radius
            ball.bounceOffPaddle(relativeHit, goingDown = true)
            soundManager.playPaddleHit()
        }

        // Scoring.
        if (ball.y - ball.radius > courtBottom) {
            scorePoint(playerScored = false)
        } else if (ball.y + ball.radius < courtTop) {
            scorePoint(playerScored = true)
        }
    }

    private fun updateAi(dt: Float) {
        // The AI only reacts when the ball travels toward it, with a small
        // aiming error based on difficulty; otherwise it drifts to center.
        val target = if (ball.velocityY < 0) {
            predictBallXAt(aiPaddle.y) + aimOffset()
        } else {
            width / 2f
        }
        aiPaddle.moveToward(target, dt, courtLeft, courtRight)
    }

    /** Simple linear prediction of where the ball crosses the AI line. */
    private fun predictBallXAt(targetY: Float): Float {
        if (abs(ball.velocityY) < 1f) return ball.x
        val t = (targetY - ball.y) / ball.velocityY
        if (t < 0) return ball.x
        var projectedX = ball.x + ball.velocityX * t
        // Reflect the projection against the side walls.
        val low = courtLeft + ball.radius
        val high = courtRight - ball.radius
        val span = high - low
        if (span <= 0) return ball.x
        var rel = (projectedX - low) % (2 * span)
        if (rel < 0) rel += 2 * span
        projectedX = if (rel > span) high - (rel - span) else low + rel
        return projectedX
    }

    private fun aimOffset(): Float {
        val err = difficulty.aiErrorPx
        return (Math.random() * 2 - 1).toFloat() * err
    }

    private fun scorePoint(playerScored: Boolean) {
        if (playerScored) {
            playerScore++
            soundManager.playScore()
        } else {
            aiScore++
            soundManager.playLose()
        }
        onScoreListener?.invoke(playerScore, aiScore)

        if (playerScore >= winScore || aiScore >= winScore) {
            winningPlayer = playerScore >= winScore
            state = State.GAME_OVER
        } else {
            // Loser of the point serves next.
            ball.reset(width / 2f, height / 2f, serveDown = playerScored)
        }
    }


    // --- Input ---

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                when (state) {
                    State.READY, State.GAME_OVER -> startGame()
                    State.PAUSED -> resumeGame()
                    State.PLAYING -> touchTargetX = event.x
                }
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (state == State.PLAYING) touchTargetX = event.x
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                touchTargetX = null
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    // --- Rendering ---

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawCourt(canvas)
        drawScores(canvas)
        drawEntities(canvas)
        when (state) {
            State.READY -> drawOverlay(
                canvas,
                context.getString(R.string.app_name),
                context.getString(R.string.tap_to_start)
            )
            State.PAUSED -> drawOverlay(
                canvas,
                context.getString(R.string.paused),
                context.getString(R.string.tap_to_start)
            )
            State.GAME_OVER -> drawOverlay(
                canvas,
                context.getString(
                    if (winningPlayer == true) R.string.you_win else R.string.you_lose
                ),
                context.getString(R.string.tap_to_restart)
            )
            State.PLAYING -> Unit
        }
    }

    private fun drawCourt(canvas: Canvas) {
        canvas.drawRect(courtLeft, courtTop, courtRight, courtBottom, tablePaint)
        // Center dashed line (the "net").
        val centerY = (courtTop + courtBottom) / 2f
        canvas.drawLine(courtLeft, centerY, courtRight, centerY, linePaint)
        // Border.
        canvas.drawRect(
            RectF(courtLeft, courtTop, courtRight, courtBottom),
            borderPaint
        )
    }

    private fun drawScores(canvas: Canvas) {
        val centerX = width / 2f
        val quarter = (courtBottom - courtTop) / 4f
        canvas.drawText(
            aiScore.toString(), centerX,
            courtTop + quarter + scorePaint.textSize / 3f, scorePaint
        )
        canvas.drawText(
            playerScore.toString(), centerX,
            courtBottom - quarter + scorePaint.textSize / 3f, scorePaint
        )
    }

    private fun drawEntities(canvas: Canvas) {
        // Ball glow + ball.
        canvas.drawCircle(ball.x, ball.y, ball.radius * 1.8f, ballGlowPaint)
        canvas.drawCircle(ball.x, ball.y, ball.radius, ballPaint)
        // Paddles with rounded corners.
        val corner = playerPaddle.height / 2f
        canvas.drawRoundRect(
            RectF(
                playerPaddle.left, playerPaddle.top,
                playerPaddle.right, playerPaddle.bottom
            ),
            corner, corner, playerPaint
        )
        canvas.drawRoundRect(
            RectF(aiPaddle.left, aiPaddle.top, aiPaddle.right, aiPaddle.bottom),
            corner, corner, aiPaint
        )
    }

    private fun drawOverlay(canvas: Canvas, title: String, subtitle: String) {
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), overlayPaint)
        val centerX = width / 2f
        val centerY = height / 2f
        canvas.drawText(
            title, centerX,
            centerY - messagePaint.textSize * 0.3f, messagePaint
        )
        canvas.drawText(
            subtitle, centerX,
            centerY + subMessagePaint.textSize * 1.2f, subMessagePaint
        )
    }
}

