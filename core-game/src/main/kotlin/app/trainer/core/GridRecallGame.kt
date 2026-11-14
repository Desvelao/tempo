package app.trainer.core

import kotlin.random.Random

data class GridRecallRound(val gridSize: Int, val tiles: Set<Int>)

/** Memory: tiles flash on a grid, the player taps them back. Answer = set of tapped tile indexes. */
class GridRecallGame : Game<GridRecallRound, Set<Int>> {
    override val id = "grid_recall"
    override val skill = Skill.MEMORY

    fun gridSizeFor(difficulty: Int): Int = (3 + (difficulty.coerceAtLeast(1) - 1) / 5).coerceAtMost(6)

    fun tileCountFor(difficulty: Int): Int {
        val size = gridSizeFor(difficulty)
        return (2 + (difficulty.coerceAtLeast(1) + 1) / 2).coerceAtMost(size * size / 2)
    }

    override fun generateRound(difficulty: Int, random: Random): GridRecallRound {
        val size = gridSizeFor(difficulty)
        val tiles = (0 until size * size).shuffled(random).take(tileCountFor(difficulty)).toSet()
        return GridRecallRound(size, tiles)
    }

    override fun evaluate(round: GridRecallRound, answer: Set<Int>): Boolean = answer == round.tiles
}
