package app.trainer.core

import kotlin.random.Random

/** A [size] x [size] grid of [base] glyphs with one [odd] glyph at cell [oddIndex]. */
data class OddOneOutRound(val size: Int, val base: String, val odd: String, val oddIndex: Int)

/** Attention: find the one symbol that differs. Answer is the tapped cell index. */
class OddOneOutGame : Game<OddOneOutRound, Int> {
    override val id = "odd_one_out"
    override val skill = Skill.ATTENTION

    fun sizeFor(difficulty: Int): Int = (3 + (difficulty.coerceAtLeast(1) - 1) / 7).coerceAtMost(5)

    /** 0 = very different glyphs, 2 = look-alikes. */
    fun tierFor(difficulty: Int): Int = ((difficulty.coerceAtLeast(1) - 1) / 7).coerceAtMost(2)

    override fun generateRound(difficulty: Int, random: Random): OddOneOutRound {
        val size = sizeFor(difficulty)
        val (a, b) = PAIRS[tierFor(difficulty)].random(random)
        val swap = random.nextBoolean()
        return OddOneOutRound(size, if (swap) b else a, if (swap) a else b, random.nextInt(size * size))
    }

    override fun evaluate(round: OddOneOutRound, answer: Int) = answer == round.oddIndex

    override fun timeLimitMs(difficulty: Int): Long = (8000L - difficulty * 200L).coerceAtLeast(3500L)

    companion object {
        val PAIRS: List<List<Pair<String, String>>> = listOf(
            listOf("O" to "X", "A" to "H", "S" to "K", "T" to "B"),
            listOf("E" to "F", "P" to "R", "M" to "N", "V" to "Y"),
            listOf("O" to "Q", "C" to "G", "6" to "9", "D" to "O"),
        )
    }
}
