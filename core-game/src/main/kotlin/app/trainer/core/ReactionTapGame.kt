package app.trainer.core

import kotlin.random.Random

/** Wait [delayMs], then tap within [thresholdMs] of the go signal. */
data class ReactionRound(val delayMs: Long, val thresholdMs: Long)

/**
 * Speed: tap as soon as the signal appears. The answer is the reaction time in ms, or [EARLY] for a tap before
 * the signal, or [NO_RESPONSE] when the player never tapped.
 */
class ReactionTapGame : Game<ReactionRound, Long> {
    override val id = "reaction_tap"
    override val skill = Skill.SPEED

    fun thresholdFor(difficulty: Int): Long = (650L - difficulty.coerceAtLeast(1) * 18L).coerceAtLeast(300L)

    override fun generateRound(difficulty: Int, random: Random) =
        ReactionRound(random.nextLong(MIN_DELAY_MS, MAX_DELAY_MS + 1), thresholdFor(difficulty))

    override fun evaluate(round: ReactionRound, answer: Long) = answer in 0..round.thresholdMs

    override fun timeoutAnswer(round: ReactionRound): Long = NO_RESPONSE

    companion object {
        const val EARLY = -1L
        const val NO_RESPONSE = -2L
        const val MIN_DELAY_MS = 1000L
        const val MAX_DELAY_MS = 3000L
        /** How long the signal stays up before the round counts as missed. */
        const val GRACE_MS = 1500L
    }
}
