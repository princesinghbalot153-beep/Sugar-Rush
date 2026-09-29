package com.example.game.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

object SoundEffects {
    private val scope = CoroutineScope(Dispatchers.Default)
    private const val SAMPLE_RATE = 44100

    private fun playTone(
        durationMs: Int,
        frequencyProducer: (Double) -> Double,
        amplitudeEnvelope: (Double) -> Double
    ) {
        scope.launch {
            try {
                val numSamples = (SAMPLE_RATE * durationMs / 1000.0).toInt()
                val buffer = ShortArray(numSamples)

                var phase = 0.0
                for (i in 0 until numSamples) {
                    val t = i.toDouble() / SAMPLE_RATE
                    val progress = i.toDouble() / numSamples
                    val freq = frequencyProducer(progress)
                    phase += 2 * PI * freq / SAMPLE_RATE
                    val sample = sin(phase) * amplitudeEnvelope(progress)
                    buffer[i] = (sample * 32767).toInt().coerceIn(-32768, 32767).toShort()
                }

                val audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(SAMPLE_RATE)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(buffer.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                audioTrack.write(buffer, 0, buffer.size)
                audioTrack.play()
                // Let track play then release
                scope.launch {
                    kotlinx.coroutines.delay(durationMs.toLong() + 100)
                    audioTrack.release()
                }
            } catch (_: Exception) {}
        }
    }

    /**
     * Sweet melodic pop with ascending pitch for cascade combos (1x, 2x, 3x...)
     */
    fun playPop(combo: Int = 1) {
        val notes = listOf(523.25, 587.33, 659.25, 783.99, 880.00, 1046.50, 1174.66, 1318.51)
        val baseFreq = notes.getOrElse((combo - 1) % notes.size) { 523.25 }
        playTone(
            durationMs = 120,
            frequencyProducer = { progress -> baseFreq * (1.0 + 0.2 * (1.0 - progress)) },
            amplitudeEnvelope = { progress -> exp(-progress * 6.0) }
        )
    }

    /**
     * Soft whoosh on candy swipe
     */
    fun playSwap() {
        playTone(
            durationMs = 90,
            frequencyProducer = { progress -> 350.0 + progress * 200.0 },
            amplitudeEnvelope = { progress -> sin(progress * PI) * 0.4 }
        )
    }

    /**
     * Thump on invalid move
     */
    fun playInvalid() {
        playTone(
            durationMs = 140,
            frequencyProducer = { 130.0 },
            amplitudeEnvelope = { progress -> exp(-progress * 8.0) * 0.5 }
        )
    }

    /**
     * Deep resonant explosion rumble for Color Bombs and Wrapped explosions
     */
    fun playExplosion() {
        playTone(
            durationMs = 350,
            frequencyProducer = { progress -> 180.0 * (1.0 - progress * 0.6) },
            amplitudeEnvelope = { progress -> exp(-progress * 4.0) * 0.9 }
        )
    }

    /**
     * High laser zap for Striped Candies
     */
    fun playStripedLaser() {
        playTone(
            durationMs = 200,
            frequencyProducer = { progress -> 1200.0 - progress * 800.0 },
            amplitudeEnvelope = { progress -> exp(-progress * 4.0) * 0.6 }
        )
    }

    /**
     * Glorious victory fanfare when level is completed
     */
    fun playVictory() {
        val fanfare = listOf(523.25, 659.25, 783.99, 1046.50)
        scope.launch {
            fanfare.forEachIndexed { i, freq ->
                playTone(
                    durationMs = 220,
                    frequencyProducer = { freq },
                    amplitudeEnvelope = { progress -> exp(-progress * 3.0) * 0.7 }
                )
                kotlinx.coroutines.delay(120)
            }
        }
    }

    /**
     * Crisp mechanical peg tick for Fortune Wheel
     */
    fun playWheelTick() {
        playTone(
            durationMs = 45,
            frequencyProducer = { 880.0 },
            amplitudeEnvelope = { progress -> exp(-progress * 15.0) * 0.5 }
        )
    }

    /**
     * High-energy powerup arpeggio for Hyper Fever activation
     */
    fun playFeverActive() {
        val notes = listOf(440.0, 554.37, 659.25, 880.0, 1108.73, 1318.51)
        scope.launch {
            notes.forEach { freq ->
                playTone(
                    durationMs = 90,
                    frequencyProducer = { freq },
                    amplitudeEnvelope = { progress -> exp(-progress * 5.0) * 0.7 }
                )
                kotlinx.coroutines.delay(45)
            }
        }
    }

    /**
     * Sweet crystal chime for rewards & achievement unlocks
     */
    fun playChime() {
        val chords = listOf(1046.50, 1318.51, 1567.98)
        scope.launch {
            chords.forEach { freq ->
                playTone(
                    durationMs = 180,
                    frequencyProducer = { freq },
                    amplitudeEnvelope = { progress -> exp(-progress * 4.0) * 0.5 }
                )
                kotlinx.coroutines.delay(60)
            }
        }
    }

    /**
     * Urgent countdown pulse for Blitz final seconds
     */
    fun playBlitzTick() {
        playTone(
            durationMs = 70,
            frequencyProducer = { 987.77 },
            amplitudeEnvelope = { progress -> exp(-progress * 10.0) * 0.6 }
        )
    }

    /**
     * Futuristic Quantum Hyper Blast sound for mega cross laser detonations
     */
    fun playQuantumLaser() {
        scope.launch {
            playTone(
                durationMs = 280,
                frequencyProducer = { progress -> 1800.0 - progress * 1400.0 },
                amplitudeEnvelope = { progress -> exp(-progress * 3.5) * 0.85 }
            )
            kotlinx.coroutines.delay(60)
            playTone(
                durationMs = 320,
                frequencyProducer = { progress -> 2400.0 - progress * 1900.0 },
                amplitudeEnvelope = { progress -> exp(-progress * 4.0) * 0.7 }
            )
        }
    }

    /**
     * Deep cosmic sub-bass shockwave for Color Bomb annihilation
     */
    fun playCosmicShockwave() {
        playTone(
            durationMs = 450,
            frequencyProducer = { progress -> 90.0 * (1.0 - progress * 0.4) },
            amplitudeEnvelope = { progress -> exp(-progress * 2.5) * 0.95 }
        )
    }
}
