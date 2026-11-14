package app.trainer.core

object Scoring {
    const val POINTS_PER_CORRECT = 100
    const val MAX_SPEED_BONUS = 50
    const val FAST_MS = 500L
    const val SLOW_MS = 5000L

    fun accuracy(results: List<RoundResult>): Double =
        if (results.isEmpty()) 0.0 else results.count { it.correct }.toDouble() / results.size

    /** Mean reaction time of correct answers, or 0 when there are none. */
    fun avgReactionMs(results: List<RoundResult>): Long {
        val correct = results.filter { it.correct }
        return if (correct.isEmpty()) 0L else correct.map { it.reactionMs }.average().toLong()
    }

    /** 100 points per correct answer plus up to 50 for speed (linear between FAST_MS and SLOW_MS). */
    fun score(results: List<RoundResult>): Int = results.filter { it.correct }.sumOf {
        val t = it.reactionMs.coerceIn(FAST_MS, SLOW_MS)
        val bonus = (MAX_SPEED_BONUS * (SLOW_MS - t) / (SLOW_MS - FAST_MS)).toInt()
        POINTS_PER_CORRECT + bonus
    }
}
