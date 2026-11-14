package app.trainer.audio

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin

/** Extra effects, played only when the user turns "more sound effects" on. */
enum class Effect { TAP, TICK, GO, DONE }

/** Generates every sound as 16-bit mono PCM, so the app ships no audio files. No Android dependencies. */
object SoundSynth {
    const val SAMPLE_RATE = 22_050

    private fun hz(semitonesFromA4: Int) = 440.0 * 2.0.pow(semitonesFromA4 / 12.0)

    /** A decaying note with a short attack, added into [out] starting at [start] (wraps around when [wrap]). */
    private fun note(
        out: FloatArray, start: Int, freq: Double, seconds: Double, gain: Double,
        decay: Double = 6.0, triangle: Boolean = false, wrap: Boolean = false,
    ) {
        val n = (seconds * SAMPLE_RATE).toInt()
        val attack = (0.005 * SAMPLE_RATE).toInt().coerceAtLeast(1)
        for (i in 0 until n) {
            val idx = start + i
            if (idx >= out.size && !wrap) break
            val t = i.toDouble() / SAMPLE_RATE
            val phase = 2 * PI * freq * t
            val wave = if (triangle) 2 / PI * kotlin.math.asin(sin(phase)) else sin(phase)
            val env = (if (i < attack) i.toDouble() / attack else 1.0) * exp(-decay * t)
            out[idx % out.size] += (wave * env * gain).toFloat()
        }
    }

    private fun pcm(buf: FloatArray, peak: Double): ShortArray {
        val max = buf.maxOf { abs(it) }.coerceAtLeast(1e-6f)
        val scale = (peak / max).coerceAtMost(1.0).toFloat() * Short.MAX_VALUE
        return ShortArray(buf.size) { (buf[it] * scale).toInt().toShort() }
    }

    private fun buffer(seconds: Double) = FloatArray((seconds * SAMPLE_RATE).toInt())
    private fun at(seconds: Double) = (seconds * SAMPLE_RATE).toInt()

    fun effect(effect: Effect): ShortArray = when (effect) {
        Effect.TAP -> buffer(0.07).also { note(it, 0, hz(15), 0.07, 1.0, decay = 45.0) }
        Effect.TICK -> buffer(0.12).also { note(it, 0, hz(3), 0.12, 1.0, decay = 30.0, triangle = true) }
        Effect.GO -> buffer(0.4).also {
            note(it, 0, hz(3), 0.4, 0.6, decay = 8.0, triangle = true)
            note(it, 0, hz(15), 0.4, 0.6, decay = 8.0)
        }
        Effect.DONE -> buffer(1.1).also {
            // Rising major arpeggio: C5 E5 G5 C6.
            listOf(3, 7, 10, 15).forEachIndexed { i, s -> note(it, at(i * 0.12), hz(s), 0.6, 0.5, decay = 5.0, triangle = i == 3) }
        }
    }.let { pcm(it, 0.9) }

    /** Wraps 16-bit mono PCM in a minimal WAV container. */
    fun wav(pcm: ShortArray): ByteArray {
        val data = pcm.size * 2
        val b = java.nio.ByteBuffer.allocate(44 + data).order(java.nio.ByteOrder.LITTLE_ENDIAN)
        b.put("RIFF".toByteArray()).putInt(36 + data).put("WAVE".toByteArray())
        b.put("fmt ".toByteArray()).putInt(16).putShort(1).putShort(1)
            .putInt(SAMPLE_RATE).putInt(SAMPLE_RATE * 2).putShort(2).putShort(16)
        b.put("data".toByteArray()).putInt(data)
        pcm.forEach { b.putShort(it) }
        return b.array()
    }

    const val LOOP_SECONDS = 32

    /**
     * A calm, seamless ambient loop: four slow pad chords with a soft pentatonic sparkle on top.
     * Notes that run past the end wrap to the start, so the loop point has no click.
     */
    fun ambientLoop(): ShortArray {
        val out = buffer(LOOP_SECONDS.toDouble())
        // Semitones from A4: Cmaj7, Am7, Fmaj7, G6 (voiced low).
        val chords = listOf(
            listOf(-21, -17, -14, -10), listOf(-24, -17, -14, -9),
            listOf(-28, -24, -21, -17), listOf(-26, -19, -17, -10),
        )
        chords.forEachIndexed { c, notes ->
            val start = at(c * 8.0)
            notes.forEach { s ->
                val f = hz(s)
                val n = at(10.0)
                for (i in 0 until n) {
                    val t = i.toDouble() / SAMPLE_RATE
                    val env = sin(PI * i / n).pow(2) // fades in and out, overlapping the next chord
                    val v = sin(2 * PI * f * t) + 0.3 * sin(2 * PI * f * 2.003 * t)
                    out[(start + i) % out.size] += (v * env * 0.12).toFloat()
                }
            }
        }
        val sparkle = listOf(3, 7, 10, 12, 15, 19) // C pentatonic-ish, above the pad
        for (k in 0 until LOOP_SECONDS) {
            // Deterministic, uneven pattern; skips some beats so it breathes.
            if ((k * 7 + k / 4) % 3 == 2) continue
            note(out, at(k + 0.5), hz(sparkle[(k * 5 + k / 8) % sparkle.size]), 2.5, 0.05, decay = 2.5, wrap = true)
        }
        return pcm(out, 0.85)
    }
}
