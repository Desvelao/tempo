package app.trainer.core

import kotlin.random.Random

/** [digits] are shown one by one; the player must enter them in reverse order. */
data class ReverseSequenceRound(val digits: List<Int>)

/** Working memory: backwards digit span. Answer is the digits typed by the player. */
class ReverseSequenceGame : Game<ReverseSequenceRound, List<Int>> {
    override val id = "reverse_sequence"
    override val skill = Skill.WORKING_MEMORY

    fun lengthFor(difficulty: Int): Int = (3 + (difficulty.coerceAtLeast(1) - 1) / 3).coerceAtMost(9)

    override fun generateRound(difficulty: Int, random: Random): ReverseSequenceRound {
        val digits = ArrayList<Int>()
        repeat(lengthFor(difficulty)) {
            // No immediate repeats, so every step of the sequence is visibly new.
            digits += (1..9).filter { it != digits.lastOrNull() }.random(random)
        }
        return ReverseSequenceRound(digits)
    }

    override fun evaluate(round: ReverseSequenceRound, answer: List<Int>) = answer == round.digits.reversed()
}
