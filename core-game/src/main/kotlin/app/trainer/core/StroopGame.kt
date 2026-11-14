package app.trainer.core

import kotlin.random.Random

enum class InkColor { RED, BLUE, GREEN, YELLOW, ORANGE, PURPLE }

/** [word] is the colour name that is displayed, [ink] the colour it is drawn in (always different). */
data class StroopRound(val word: InkColor, val ink: InkColor, val options: List<InkColor>)

/** Attention: pick the ink colour, ignore the word. */
class StroopGame : Game<StroopRound, InkColor> {
    override val id = "stroop"
    override val skill = Skill.ATTENTION

    fun colorCountFor(difficulty: Int): Int = (3 + (difficulty.coerceAtLeast(1) - 1) / 5).coerceAtMost(6)

    override fun generateRound(difficulty: Int, random: Random): StroopRound {
        val pool = InkColor.entries.take(colorCountFor(difficulty))
        val word = pool.random(random)
        val ink = (pool - word).random(random)
        return StroopRound(word, ink, pool.shuffled(random))
    }

    override fun evaluate(round: StroopRound, answer: InkColor) = answer == round.ink

    override fun timeLimitMs(difficulty: Int): Long = (6000L - difficulty * 200L).coerceAtLeast(2500L)

    override fun timeoutAnswer(round: StroopRound): InkColor? = null
}
