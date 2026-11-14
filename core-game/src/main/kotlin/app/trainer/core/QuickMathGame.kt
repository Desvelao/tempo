package app.trainer.core

import kotlin.random.Random

enum class MathOp(val symbol: String) { ADD("+"), SUB("−"), MUL("×") }

data class MathRound(val a: Int, val b: Int, val op: MathOp, val options: List<Int>) {
    val answer: Int
        get() = when (op) {
            MathOp.ADD -> a + b
            MathOp.SUB -> a - b
            MathOp.MUL -> a * b
        }
}

/** Speed: timed arithmetic with multiple-choice answers. */
class QuickMathGame : Game<MathRound, Int> {
    override val id = "quick_math"
    override val skill = Skill.SPEED

    fun opsFor(difficulty: Int): List<MathOp> =
        if (difficulty < 5) listOf(MathOp.ADD, MathOp.SUB) else MathOp.entries

    fun maxOperandFor(difficulty: Int, op: MathOp): Int =
        if (op == MathOp.MUL) (3 + difficulty / 2).coerceAtMost(12) else 9 + difficulty * 3

    override fun generateRound(difficulty: Int, random: Random): MathRound {
        val d = difficulty.coerceAtLeast(1)
        val op = opsFor(d).random(random)
        val max = maxOperandFor(d, op)
        var a = random.nextInt(2, max + 1)
        var b = random.nextInt(2, max + 1)
        if (op == MathOp.SUB && b > a) a = b.also { b = a }
        val correct = MathRound(a, b, op, emptyList()).answer
        val options = linkedSetOf(correct)
        while (options.size < 4) {
            val delta = random.nextInt(1, 6) * (if (random.nextBoolean()) 1 else -1)
            options += correct + delta
        }
        return MathRound(a, b, op, options.shuffled(random))
    }

    override fun evaluate(round: MathRound, answer: Int) = answer == round.answer

    override fun timeLimitMs(difficulty: Int): Long = (10_000L - difficulty * 300L).coerceAtLeast(4000L)
}
