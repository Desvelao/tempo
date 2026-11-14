package app.trainer.ui

import androidx.compose.ui.graphics.Color
import app.trainer.core.InkColor
import app.trainer.games.stroop.colorBlindInk
import app.trainer.ui.theme.FocusDark
import app.trainer.ui.theme.FocusDarkColorBlind
import app.trainer.ui.theme.FocusLight
import app.trainer.ui.theme.FocusLightColorBlind
import app.trainer.ui.theme.focusScheme
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Colour-vision deficiency is simulated with the Machado et al. (2009) full-severity matrices and the distance is
 * measured as CIE76 delta E, so "far apart" has a number. Around 10 is hard to tell apart, 50 or more is obvious.
 */
class ColorBlindPaletteTest {
    private val deuteranopia = arrayOf(
        doubleArrayOf(0.367322, 0.860646, -0.227968),
        doubleArrayOf(0.280085, 0.672501, 0.047413),
        doubleArrayOf(-0.011820, 0.042940, 0.968881),
    )
    private val protanopia = arrayOf(
        doubleArrayOf(0.152286, 1.052583, -0.204868),
        doubleArrayOf(0.114503, 0.786281, 0.099216),
        doubleArrayOf(-0.003882, -0.048116, 1.051998),
    )

    private fun linear(c: Float): Double = c.toDouble().let { if (it <= 0.04045) it / 12.92 else ((it + 0.055) / 1.055).pow(2.4) }

    private fun rgb(color: Color) = doubleArrayOf(linear(color.red), linear(color.green), linear(color.blue))

    private fun simulate(color: Color, matrix: Array<DoubleArray>?): DoubleArray {
        val l = rgb(color)
        return if (matrix == null) l else DoubleArray(3) { i -> (0..2).sumOf { j -> matrix[i][j] * l[j] } }
    }

    private fun lab(l: DoubleArray): DoubleArray {
        val x = 0.4124 * l[0] + 0.3576 * l[1] + 0.1805 * l[2]
        val y = 0.2126 * l[0] + 0.7152 * l[1] + 0.0722 * l[2]
        val z = 0.0193 * l[0] + 0.1192 * l[1] + 0.9505 * l[2]
        fun f(t: Double) = if (t > 0.008856) t.pow(1.0 / 3) else 7.787 * t + 16.0 / 116
        val fx = f(x / 0.95047); val fy = f(y); val fz = f(z / 1.08883)
        return doubleArrayOf(116 * fy - 16, 500 * (fx - fy), 200 * (fy - fz))
    }

    private fun deltaE(a: Color, b: Color, matrix: Array<DoubleArray>?): Double {
        val la = lab(simulate(a, matrix)); val lb = lab(simulate(b, matrix))
        return sqrt((0..2).sumOf { (la[it] - lb[it]).pow(2) })
    }

    private fun luminance(c: Color) = rgb(c).let { 0.2126 * it[0] + 0.7152 * it[1] + 0.0722 * it[2] }

    private fun contrast(a: Color, b: Color): Double {
        val (hi, lo) = listOf(luminance(a), luminance(b)).sortedDescending()
        return (hi + 0.05) / (lo + 0.05)
    }

    private val modes = listOf("deuteranopia" to deuteranopia, "protanopia" to protanopia)

    private fun worstInkPair(dark: Boolean, matrix: Array<DoubleArray>): Double {
        val inks = InkColor.entries.map { it.colorBlindInk(dark) }
        return inks.indices.flatMap { i -> (i + 1 until inks.size).map { j -> deltaE(inks[i], inks[j], matrix) } }.min()
    }

    @Test fun successAndErrorStayFarApartInTheColourBlindSchemes() {
        for ((name, m) in modes) {
            assertTrue("dark $name", deltaE(FocusDarkColorBlind.primary, FocusDarkColorBlind.error, m) >= 60)
            assertTrue("light $name", deltaE(FocusLightColorBlind.primary, FocusLightColorBlind.error, m) >= 60)
        }
    }

    @Test fun theDefaultLightSchemeIsHardForRedGreenColourBlindness() {
        // Why the option exists: olive success vs red error is close under deuteranopia.
        assertTrue(deltaE(FocusLight.primary, FocusLight.error, deuteranopia) < 20)
        // ...and the colour-blind scheme is several times better.
        assertTrue(
            deltaE(FocusLightColorBlind.primary, FocusLightColorBlind.error, deuteranopia) >
                3 * deltaE(FocusLight.primary, FocusLight.error, deuteranopia),
        )
    }

    @Test fun textOnAccentsIsReadable() {
        for (scheme in listOf(FocusDarkColorBlind, FocusLightColorBlind)) {
            assertTrue(contrast(scheme.onPrimary, scheme.primary) >= 4.5)
            assertTrue(contrast(scheme.onError, scheme.error) >= 4.5)
            assertTrue(contrast(scheme.onSecondary, scheme.secondary) >= 4.5)
        }
    }

    @Test fun colourBlindInksAreFarApartAndReadable() {
        for ((name, m) in modes) {
            assertTrue("dark $name", worstInkPair(dark = true, matrix = m) >= 15)
            assertTrue("light $name", worstInkPair(dark = false, matrix = m) >= 7)
        }
        val darkPanel = FocusDarkColorBlind.surfaceContainer
        val lightPanel = FocusLightColorBlind.surfaceContainer
        // The inks are huge display text, so the 3:1 large-text ratio applies on light panels.
        for (ink in InkColor.entries) {
            assertTrue("dark $ink", contrast(ink.colorBlindInk(true), darkPanel) >= 4.3)
            assertTrue("light $ink", contrast(ink.colorBlindInk(false), lightPanel) >= 3.0)
        }
    }

    @Test fun schemeSelection() {
        assertSame(FocusDark, focusScheme(dark = true, colorBlind = false))
        assertSame(FocusLight, focusScheme(dark = false, colorBlind = false))
        assertSame(FocusDarkColorBlind, focusScheme(dark = true, colorBlind = true))
        assertSame(FocusLightColorBlind, focusScheme(dark = false, colorBlind = true))
    }

    @Test fun surfaceVariantStaysDifferentFromSurfaceContainer() {
        // Theme.kt gotcha: equal values make Material pick the muted text colour on panels.
        for (s in listOf(FocusDarkColorBlind, FocusLightColorBlind)) assertTrue(s.surfaceVariant != s.surfaceContainer)
    }
}
