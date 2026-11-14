package app.trainer.core

import kotlin.random.Random

/**
 * Show [digits] one by one, then ask whether `a + b = shownSum` before the player recalls the digits in order.
 * The sum check is the distractor that interferes with holding the digits.
 */
data class DigitSpanRound(val digits: List<Int>, val a: Int, val b: Int, val shownSum: Int) {
    val sumIsCorrect: Boolean get() = a + b == shownSum
}

/** What the player did: the verdict on the sum and the digits typed back. */
data class DigitSpanAnswer(val sumIsCorrect: Boolean, val digits: List<Int>)

/** Working memory: forward digit span with an arithmetic distractor. Both parts must be right. */
class DigitSpanGame : Game<DigitSpanRound, DigitSpanAnswer> {
    override val id = "digit_span"
    override val skill = Skill.WORKING_MEMORY

    fun lengthFor(difficulty: Int): Int = (3 + (difficulty.coerceAtLeast(1) - 1) / 3).coerceAtMost(8)

    /** Larger addends at higher levels, so the distractor competes harder for attention. */
    fun addendMaxFor(difficulty: Int): Int = (9 + (difficulty.coerceAtLeast(1) - 1) / 2).coerceAtMost(19)

    override fun generateRound(difficulty: Int, random: Random): DigitSpanRound {
        val digits = ArrayList<Int>()
        repeat(lengthFor(difficulty)) {
            digits += (1..9).filter { it != digits.lastOrNull() }.random(random)
        }
        val max = addendMaxFor(difficulty)
        val a = (2..max).random(random)
        val b = (2..max).random(random)
        val shown = if (random.nextBoolean()) a + b else a + b + listOf(-2, -1, 1, 2).random(random)
        return DigitSpanRound(digits, a, b, shown)
    }

    override fun evaluate(round: DigitSpanRound, answer: DigitSpanAnswer) =
        answer.sumIsCorrect == round.sumIsCorrect && answer.digits == round.digits

    override fun timeLimitMs(difficulty: Int): Long = 30_000L
}
