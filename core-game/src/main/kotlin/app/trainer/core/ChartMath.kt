package app.trainer.core

/** Pure helpers turning data series into 0..1 chart coordinates (UI scales them to pixels). */
object ChartMath {
    /** Horizontal positions: one point sits in the middle, otherwise evenly spread from 0 to 1. */
    fun xFractions(count: Int): List<Float> = when {
        count <= 0 -> emptyList()
        count == 1 -> listOf(0.5f)
        else -> List(count) { it.toFloat() / (count - 1) }
    }

    /** Vertical positions, 0 = bottom, 1 = top, values clamped to [min]..[max]. */
    fun yFractions(values: List<Double>, min: Double, max: Double): List<Float> {
        val span = max - min
        if (span <= 0.0) return values.map { 0f }
        return values.map { ((it.coerceIn(min, max) - min) / span).toFloat() }
    }

    /** A "nice" upper bound for a score axis: at least [floor], rounded up to a multiple of 100. */
    fun axisMax(values: List<Double>, floor: Double = 100.0): Double {
        val m = values.maxOrNull() ?: return floor
        return maxOf(floor, kotlin.math.ceil(m / 100.0) * 100.0)
    }
}
