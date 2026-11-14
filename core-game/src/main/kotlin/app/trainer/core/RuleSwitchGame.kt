package app.trainer.core

import kotlin.random.Random

enum class Rule { COLOR, SHAPE }
enum class CardColor { RED, BLUE }
enum class CardShape { CIRCLE, SQUARE }

data class RuleSwitchRound(val color: CardColor, val shape: CardShape, val rule: Rule, val isSwitch: Boolean) {
    /** 0/1: index of the colour (RED, BLUE) or shape (CIRCLE, SQUARE) matching the active rule. */
    val correctAnswer: Int get() = if (rule == Rule.COLOR) color.ordinal else shape.ordinal
}

/** Flexibility: classify a card by the active rule; the rule changes with a cue. Stateful, call [reset] per session. */
class RuleSwitchGame : Game<RuleSwitchRound, Int> {
    override val id = "rule_switch"
    override val skill = Skill.FLEXIBILITY

    private var lastRule: Rule? = null

    fun switchProbabilityFor(difficulty: Int): Double = (0.2 + 0.02 * difficulty).coerceAtMost(0.5)

    override fun reset() { lastRule = null }

    override fun generateRound(difficulty: Int, random: Random): RuleSwitchRound {
        val previous = lastRule
        val switch = previous != null && random.nextDouble() < switchProbabilityFor(difficulty)
        val rule = when {
            previous == null -> Rule.entries.random(random)
            switch -> if (previous == Rule.COLOR) Rule.SHAPE else Rule.COLOR
            else -> previous
        }
        lastRule = rule
        return RuleSwitchRound(
            CardColor.entries.random(random), CardShape.entries.random(random), rule, switch,
        )
    }

    override fun evaluate(round: RuleSwitchRound, answer: Int) = answer == round.correctAnswer

    override fun timeLimitMs(difficulty: Int): Long = (5000L - difficulty * 100L).coerceAtLeast(2500L)
}
