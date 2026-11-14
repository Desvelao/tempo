package app.trainer.core

import kotlin.random.Random

/** Outcome of a single round, as reported by the UI layer. */
data class RoundResult(val correct: Boolean, val reactionMs: Long)

/**
 * A mini-game's pure logic. All randomness comes from the injected [Random]
 * so rounds are reproducible in tests.
 */
interface Game<Round, Answer> {
    val id: String
    val skill: Skill

    fun generateRound(difficulty: Int, random: Random): Round

    fun evaluate(round: Round, answer: Answer): Boolean

    /** Per-round time limit, or null for untimed rounds. */
    fun timeLimitMs(difficulty: Int): Long? = null

    /** Answer assumed when the time limit expires; null means the round counts as wrong. */
    fun timeoutAnswer(round: Round): Answer? = null

    /** Clears any cross-round state (streams, previous rule). Called before a session starts. */
    fun reset() {}

    /** Default scoring; games may override. */
    fun score(results: List<RoundResult>): Int = Scoring.score(results)
}
