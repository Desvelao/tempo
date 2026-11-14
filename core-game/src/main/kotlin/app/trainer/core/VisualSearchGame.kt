package app.trainer.core

import kotlin.random.Random

/** Find [target] among look-alike [cells] (row-major, [size] x [size]); it appears once, at [targetIndex]. */
data class VisualSearchRound(val size: Int, val target: String, val cells: List<String>, val targetIndex: Int)

/** Speed (visual search): tap the one cell that matches the target. Answer is the tapped cell index. */
class VisualSearchGame : Game<VisualSearchRound, Int> {
    override val id = "visual_search"
    override val skill = Skill.SPEED

    fun sizeFor(difficulty: Int): Int = (4 + (difficulty.coerceAtLeast(1) - 1) / 7).coerceAtMost(6)

    /** How many different distractor glyphs are mixed in: more variety means a harder search. */
    fun varietyFor(difficulty: Int): Int = (1 + (difficulty.coerceAtLeast(1) - 1) / 7).coerceAtMost(3)

    override fun generateRound(difficulty: Int, random: Random): VisualSearchRound {
        val size = sizeFor(difficulty)
        val (target, pool) = SETS.random(random)
        val distractors = pool.shuffled(random).take(varietyFor(difficulty))
        val targetIndex = random.nextInt(size * size)
        val cells = List(size * size) { if (it == targetIndex) target else distractors.random(random) }
        return VisualSearchRound(size, target, cells, targetIndex)
    }

    override fun evaluate(round: VisualSearchRound, answer: Int) = answer == round.targetIndex

    override fun timeLimitMs(difficulty: Int): Long = (11000L - difficulty * 200L).coerceAtLeast(6000L)

    companion object {
        /** A target and the look-alikes it hides among. A target is never part of its own pool. */
        val SETS: List<Pair<String, List<String>>> = listOf(
            "Q" to listOf("O", "C", "G", "D"),
            "E" to listOf("F", "L", "T", "I"),
            "6" to listOf("9", "8", "0", "5"),
            "b" to listOf("d", "p", "q", "h"),
        )
    }
}
