package com.example.pingpong

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.sin

/**
 * Generates short retro sound effects (paddle hit, wall bounce, score)
 * programmatically so the game needs no bundled audio assets.
 * Sounds are synthesized on a background thread to avoid jank.
 */
class SoundManager {

    @Volatile
    var enabled: Boolean = true

    private val sampleRate = 22050

    fun playPaddleHit() = playTone(520f, 60, decay = true)

    fun playWallBounce() = playTone(320f, 45, decay = true)

    fun playScore() {
        playTone(660f, 90, decay = false)
        playTone(880f, 140, decay = true, delayMs = 90)
    }

    fun playLose() {
        playTone(440f, 120, decay = false)
        playTone(220f, 220, decay = true, delayMs = 120)
    }

    private fun playTone(
        frequency: Float,
        durationMs: Int,
        decay: Boolean,
        delayMs: Long = 0
    ) {
        if (!enabled) return
        Thread {
            try {
                if (delayMs > 0) Thread.sleep(delayMs)
                val count = (sampleRate * durationMs / 1000)
                val samples = ShortArray(count)
                for (i in 0 until count) {
                    val t = i.toFloat() / sampleRate
                    val envelope = if (decay) 1f - i.toFloat() / count else 1f
                    samples[i] = (sin(2.0 * Math.PI * frequency * t) *
                            Short.MAX_VALUE * 0.35f * envelope).toInt().toShort()
                }
                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setSampleRate(sampleRate)
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(samples.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
                track.write(samples, 0, samples.size)
                track.play()
                // Release after playback completes.
                Thread.sleep(durationMs.toLong() + 40)
                track.stop()
                track.release()
            } catch (_: Exception) {
                // Audio must never crash the game loop.
            }
        }.start()
    }
}
