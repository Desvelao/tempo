package app.trainer.core

import kotlin.random.Random

/** [position] is a cell 0..8 of a 3x3 grid. Answer `true` = "matches the one [n] steps back". */
data class NBackRound(val position: Int, val n: Int, val isMatch: Boolean)

/** Working memory: stateful stream, call [reset] before each session. Not pressing = answering `false`. */
class NBackGame : Game<NBackRound, Boolean> {
    override val id = "n_back"
    override val skill = Skill.WORKING_MEMORY

    private val history = ArrayList<Int>()

    fun nFor(difficulty: Int): Int = (1 + (difficulty.coerceAtLeast(1) - 1) / 7).coerceAtMost(3)

    override fun reset() = history.clear()

    override fun generateRound(difficulty: Int, random: Random): NBackRound {
        val n = nFor(difficulty)
        val target = if (history.size >= n) history[history.size - n] else null
        val isMatch = target != null && random.nextDouble() < MATCH_PROBABILITY
        val position = when {
            isMatch -> target
            target == null -> random.nextInt(CELLS)
            else -> (0 until CELLS).filter { it != target }.random(random)
        }
        history += position
        return NBackRound(position, n, isMatch)
    }

    override fun evaluate(round: NBackRound, answer: Boolean) = answer == round.isMatch

    override fun timeLimitMs(difficulty: Int): Long = 2500L

    override fun timeoutAnswer(round: NBackRound): Boolean = false

    companion object {
        const val CELLS = 9
        const val MATCH_PROBABILITY = 0.3
    }
}
