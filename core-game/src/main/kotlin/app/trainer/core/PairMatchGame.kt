package app.trainer.core

import kotlin.random.Random

/** [cards] holds one symbol index (into [PairMatchGame.SYMBOLS]) per card; every symbol appears exactly twice. */
data class PairMatchRound(val pairCount: Int, val cards: List<Int>, val columns: Int)

/**
 * Memory: flip cards two at a time to find all pairs. The answer is the number of flips used; the board counts as
 * correct when it was cleared within [flipBudget] flips.
 */
class PairMatchGame : Game<PairMatchRound, Int> {
    override val id = "pair_match"
    override val skill = Skill.MEMORY

    fun pairCountFor(difficulty: Int): Int = (3 + (difficulty.coerceAtLeast(1) - 1) / 3).coerceAtMost(SYMBOLS.size)

    fun columnsFor(cardCount: Int): Int = when {
        cardCount <= 6 -> 3
        cardCount <= 16 -> 4
        else -> 5
    }

    /** A fair budget: perfect memory needs 2 flips per pair, so allow 3. */
    fun flipBudget(pairCount: Int): Int = pairCount * 3

    override fun generateRound(difficulty: Int, random: Random): PairMatchRound {
        val pairs = pairCountFor(difficulty)
        val symbols = SYMBOLS.indices.shuffled(random).take(pairs)
        val cards = (symbols + symbols).shuffled(random)
        return PairMatchRound(pairs, cards, columnsFor(cards.size))
    }

    fun isMatch(round: PairMatchRound, a: Int, b: Int): Boolean = a != b && round.cards[a] == round.cards[b]

    override fun evaluate(round: PairMatchRound, answer: Int) = answer in 1..flipBudget(round.pairCount)

    companion object {
        val SYMBOLS = listOf("★", "●", "■", "▲", "♦", "♥", "♣", "♠", "☀", "☂")
    }
}
