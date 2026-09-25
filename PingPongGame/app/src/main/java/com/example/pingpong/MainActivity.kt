package com.example.pingpong

import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

/**
 * Hosts the [GameView], keeps the screen on in immersive mode, and
 * pauses/resumes the game loop with the activity lifecycle.
 *
 * A floating gear button opens the settings dialog (difficulty + sound).
 */
class MainActivity : AppCompatActivity() {

    private lateinit var gameView: GameView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        gameView = findViewById(R.id.gameView)

        findViewById<ImageButton>(R.id.settingsButton).setOnClickListener {
            showSettingsDialog()
        }

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enterImmersiveMode()
    }

    override fun onResume() {
        super.onResume()
        enterImmersiveMode()
        gameView.startLoop()
    }

    override fun onPause() {
        super.onPause()
        gameView.pauseGame()
        gameView.stopLoop()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) enterImmersiveMode()
    }

    // --- Settings dialog ------------------------------------------------------

    private fun showSettingsDialog() {
        val wasPlaying = gameView.isPlaying()
        gameView.pauseGame()

        val difficulties = GameView.Difficulty.entries.toTypedArray()
        val padH = (20 * resources.displayMetrics.density).toInt()

        val radioGroup = RadioGroup(this)
        difficulties.forEachIndexed { index, diff ->
            radioGroup.addView(
                RadioButton(this).apply {
                    id = index + 1 // id 0 is invalid; shift by one
                    text = diff.name.lowercase()
                        .replaceFirstChar { it.titlecase() }
                    isChecked = gameView.difficulty == diff
                }
            )
        }

        val soundCheck = CheckBox(this).apply {
            text = getString(R.string.sound_effects)
            isChecked = gameView.soundManager.enabled
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padH, padH / 2, padH, 0)
            addView(TextView(context).apply {
                text = getString(R.string.difficulty)
                setTypeface(typeface, Typeface.BOLD)
            })
            addView(radioGroup)
            addView(soundCheck)
        }

        val restore = { if (wasPlaying) gameView.resumeGame() }
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.settings))
            .setView(container)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val checked = radioGroup.checkedRadioButtonId
                if (checked > 0) {
                    gameView.difficulty = difficulties[checked - 1]
                }
                gameView.soundManager.enabled = soundCheck.isChecked
                restore()
            }
            .setOnCancelListener { restore() }
            .show()
    }

    // --- Immersive mode -------------------------------------------------------

    @Suppress("DEPRECATION")
    private fun enterImmersiveMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.let {
                it.hide(WindowInsets.Type.systemBars())
                it.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            or View.SYSTEM_UI_FLAG_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    )
        }
    }
}
