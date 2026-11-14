package app.trainer.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ChartMathTest {
    @Test fun xFractions() {
        assertEquals(emptyList(), ChartMath.xFractions(0))
        assertEquals(listOf(0.5f), ChartMath.xFractions(1))
        assertEquals(listOf(0f, 0.5f, 1f), ChartMath.xFractions(3))
    }

    @Test fun yFractionsScaleAndClamp() {
        assertEquals(listOf(0f, 0.5f, 1f, 1f, 0f), ChartMath.yFractions(listOf(0.0, 50.0, 100.0, 150.0, -5.0), 0.0, 100.0))
        assertEquals(emptyList(), ChartMath.yFractions(emptyList(), 0.0, 1.0))
        assertEquals(listOf(0f, 0f), ChartMath.yFractions(listOf(3.0, 4.0), 5.0, 5.0))
    }

    @Test fun axisMax() {
        assertEquals(100.0, ChartMath.axisMax(emptyList()))
        assertEquals(1100.0, ChartMath.axisMax(listOf(1088.0, 924.0)))
        assertTrue(ChartMath.axisMax(listOf(10.0)) >= 100.0)
    }
}
