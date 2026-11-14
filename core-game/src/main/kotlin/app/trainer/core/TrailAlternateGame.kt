package app.trainer.core

import kotlin.random.Random

/**
 * [labels] is the sequence to tap in order (1, A, 2, B, ...). [cellOfStep] says which grid cell holds each step;
 * the grid has [columns] columns and [cells] cells in total.
 */
data class TrailRound(val labels: List<String>, val cellOfStep: List<Int>, val columns: Int, val cells: Int)

/** Flexibility: tap numbers and letters alternately in order. The answer is the number of wrong taps. */
class TrailAlternateGame : Game<TrailRound, Int> {
    override val id = "trail_alternate"
    override val skill = Skill.FLEXIBILITY

    fun lengthFor(difficulty: Int): Int = (6 + (difficulty.coerceAtLeast(1) - 1) / 2).coerceAtMost(16)

    /** Step 0 -> "1", 1 -> "A", 2 -> "2", 3 -> "B", ... */
    fun labelFor(step: Int): String = if (step % 2 == 0) (step / 2 + 1).toString() else ('A' + step / 2).toString()

    override fun generateRound(difficulty: Int, random: Random): TrailRound {
        val length = lengthFor(difficulty)
        val cells = (length + COLUMNS - 1) / COLUMNS * COLUMNS
        val cellOfStep = (0 until cells).shuffled(random).take(length)
        return TrailRound((0 until length).map(::labelFor), cellOfStep, COLUMNS, cells)
    }

    fun errorBudget(length: Int): Int = length / 4

    override fun evaluate(round: TrailRound, answer: Int) = answer in 0..errorBudget(round.labels.size)

    companion object {
        const val COLUMNS = 4
    }
}
