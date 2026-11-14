package app.trainer.core

import kotlin.random.Random

/**
 * A [size] x [size] pattern, row-major. [options] holds four patterns: exactly one ([correctIndex]) is the
 * [target] turned by 90, 180 or 270 degrees; the other three are turned mirror images.
 */
data class MentalRotationRound(
    val size: Int,
    val target: List<Boolean>,
    val options: List<List<Boolean>>,
    val correctIndex: Int,
)

/** Attention (visuospatial): pick the option that is the target rotated, not mirrored. Answer is the option index. */
class MentalRotationGame : Game<MentalRotationRound, Int> {
    override val id = "mental_rotation"
    override val skill = Skill.ATTENTION

    fun sizeFor(difficulty: Int): Int = (3 + (difficulty.coerceAtLeast(1) - 1) / 7).coerceAtMost(5)

    /** Filled cells grow with the grid and a little with the level. */
    fun cellsFor(difficulty: Int): Int {
        val size = sizeFor(difficulty)
        return (size + 1 + (difficulty.coerceAtLeast(1) - 1) % 7 / 3).coerceAtMost(size * size - 2)
    }

    override fun generateRound(difficulty: Int, random: Random): MentalRotationRound {
        val size = sizeFor(difficulty)
        val cells = cellsFor(difficulty)
        val target = randomUsablePattern(size, cells, random)
        val turned = (1..3).random(random) // 90, 180 or 270 degrees
        val correct = rotate(target, size, turned)
        val mirrored = mirror(target, size)
        // The mirror's four rotations are distinct, so any three of them make three different wrong options.
        val wrong = (0..3).shuffled(random).take(3).map { rotate(mirrored, size, it) }
        val correctIndex = random.nextInt(4)
        val options = wrong.toMutableList().also { it.add(correctIndex, correct) }
        return MentalRotationRound(size, target, options, correctIndex)
    }

    override fun evaluate(round: MentalRotationRound, answer: Int) = answer == round.correctIndex

    override fun timeLimitMs(difficulty: Int): Long = (14000L - difficulty * 300L).coerceAtLeast(7000L)

    /**
     * A pattern is usable when, as a *shape* (ignoring where it sits in the grid), it has no rotational symmetry and
     * is not congruent to its mirror image (it is chiral). Then rotated copies and mirrored copies can never be
     * confused with each other, even though the player sees them at different positions.
     */
    fun isUsable(pattern: List<Boolean>, size: Int): Boolean {
        val rotations = (0..3).map { canonical(rotate(pattern, size, it), size) }
        if (rotations.toSet().size != 4) return false
        val mirroredRotations = (0..3).map { canonical(rotate(mirror(pattern, size), size, it), size) }
        return rotations.none { it in mirroredRotations }
    }

    private fun randomUsablePattern(size: Int, cells: Int, random: Random): List<Boolean> {
        repeat(MAX_ATTEMPTS) {
            val filled = (0 until size * size).shuffled(random).take(cells).toSet()
            val pattern = List(size * size) { it in filled }
            if (isUsable(pattern, size)) return pattern
        }
        error("no usable pattern for size=$size cells=$cells")
    }

    companion object {
        private const val MAX_ATTEMPTS = 10_000

        /** Rotates a row-major [size] x [size] pattern clockwise by [quarterTurns] x 90 degrees. */
        fun rotate(pattern: List<Boolean>, size: Int, quarterTurns: Int): List<Boolean> {
            var current = pattern
            repeat(quarterTurns.mod(4)) {
                // After a clockwise turn, cell (r, c) holds what was at (size - 1 - c, r).
                current = List(size * size) { i -> current[(size - 1 - i % size) * size + i / size] }
            }
            return current
        }

        /** The filled cells shifted so the shape touches the top-left corner: equal for equal shapes. */
        fun canonical(pattern: List<Boolean>, size: Int): Set<Pair<Int, Int>> {
            val cells = pattern.indices.filter { pattern[it] }.map { it / size to it % size }
            val minRow = cells.minOfOrNull { it.first } ?: 0
            val minCol = cells.minOfOrNull { it.second } ?: 0
            return cells.map { (r, c) -> r - minRow to c - minCol }.toSet()
        }

        fun mirror(pattern: List<Boolean>, size: Int): List<Boolean> =
            List(size * size) { i -> pattern[(i / size) * size + (size - 1 - i % size)] }
    }
}
