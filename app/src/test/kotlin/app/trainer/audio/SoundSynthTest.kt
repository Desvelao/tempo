package app.trainer.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class SoundSynthTest {
    @Test fun everyEffectIsAudibleAndShort() {
        Effect.entries.forEach {
            val pcm = SoundSynth.effect(it)
            assertTrue("$it empty", pcm.isNotEmpty() && pcm.size < SoundSynth.SAMPLE_RATE * 2)
            assertTrue("$it silent", pcm.any { s -> abs(s.toInt()) > 1000 })
        }
    }

    @Test fun ambientLoopHasTheExpectedLengthAndNoJumpAtTheLoopPoint() {
        val pcm = SoundSynth.ambientLoop()
        assertEquals(SoundSynth.LOOP_SECONDS * SoundSynth.SAMPLE_RATE, pcm.size)
        assertTrue(pcm.any { abs(it.toInt()) > 1000 })
        assertTrue("click at the loop point", abs(pcm.last() - pcm.first()) < 2000)
    }

    @Test fun wavHasAValidHeader() {
        val pcm = SoundSynth.effect(Effect.TAP)
        val wav = SoundSynth.wav(pcm)
        assertEquals(44 + pcm.size * 2, wav.size)
        assertEquals("RIFF", String(wav, 0, 4))
        assertEquals("WAVE", String(wav, 8, 4))
        assertEquals("data", String(wav, 36, 4))
    }
}
