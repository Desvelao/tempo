package app.trainer.core

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GridRecallGameTest {
    private val game = GridRecallGame()

    @Test fun sizeAndCountScaleWithDifficulty() {
        assertEquals(3, game.gridSizeFor(1)); assertEquals(3, game.tileCountFor(1))
        assertEquals(4, game.gridSizeFor(6))
        assertEquals(6, game.gridSizeFor(20))
        assertTrue(game.tileCountFor(20) > game.tileCountFor(1))
    }

    @Test fun roundsAreValidForAllLevels() {
        val r = Random(5)
        for (level in 1..20) {
            val round = game.generateRound(level, r)
            assertEquals(game.tileCountFor(level), round.tiles.size)
            assertTrue(round.tiles.all { it in 0 until round.gridSize * round.gridSize })
        }
    }

    @Test fun seededGenerationIsReproducible() {
        assertEquals(game.generateRound(8, Random(9)), game.generateRound(8, Random(9)))
    }

    @Test fun evaluateRequiresExactSet() {
        val round = GridRecallRound(3, setOf(1, 4, 7))
        assertTrue(game.evaluate(round, setOf(7, 1, 4)))
        assertFalse(game.evaluate(round, setOf(1, 4)))
        assertFalse(game.evaluate(round, setOf(1, 4, 8)))
    }
}
