package app.trainer.core

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Toy game: answer is the sum of two operands whose size grows with difficulty. */
private class SumGame : Game<Pair<Int, Int>, Int> {
    override val id = "sum"
    override val skill = Skill.SPEED
    override fun generateRound(difficulty: Int, random: Random) =
        random.nextInt(0, difficulty * 10) to random.nextInt(0, difficulty * 10)
    override fun evaluate(round: Pair<Int, Int>, answer: Int) = round.first + round.second == answer
}

class GameTest {
    private val game = SumGame()

    @Test fun roundsAreDeterministicForSameSeed() {
        val a = List(5) { game.generateRound(3, Random(42)) }
        val r1 = Random(7); val r2 = Random(7)
        assertEquals(List(5) { game.generateRound(3, r1) }, List(5) { game.generateRound(3, r2) })
        assertEquals(1, a.toSet().size)
    }

    @Test fun roundsRespectDifficulty() {
        val r = Random(1)
        repeat(100) {
            val (x, y) = game.generateRound(2, r)
            assertTrue(x in 0 until 20 && y in 0 until 20)
        }
    }

    @Test fun evaluate() {
        assertTrue(game.evaluate(2 to 3, 5))
        assertFalse(game.evaluate(2 to 3, 6))
    }
}

class ScoringTest {
    @Test fun emptyResults() {
        assertEquals(0, Scoring.score(emptyList()))
        assertEquals(0.0, Scoring.accuracy(emptyList()))
        assertEquals(0L, Scoring.avgReactionMs(emptyList()))
    }

    @Test fun accuracyAndReaction() {
        val rs = listOf(RoundResult(true, 1000), RoundResult(false, 9000), RoundResult(true, 2000), RoundResult(true, 3000))
        assertEquals(0.75, Scoring.accuracy(rs))
        assertEquals(2000L, Scoring.avgReactionMs(rs))
    }

    @Test fun scoreAddsSpeedBonusOnlyForCorrect() {
        assertEquals(150, Scoring.score(listOf(RoundResult(true, 100))))
        assertEquals(100, Scoring.score(listOf(RoundResult(true, 60_000))))
        assertEquals(0, Scoring.score(listOf(RoundResult(false, 100))))
    }

    @Test fun gameUsesDefaultScoring() {
        val rs = listOf(RoundResult(true, 500))
        assertEquals(Scoring.score(rs), SumGame().score(rs))
    }
}

class DifficultyTest {
    @Test fun risesAfterStreakOfCorrect() {
        var d = Difficulty(level = 5)
        repeat(2) { d = d.record(true) }
        assertEquals(5, d.level)
        d = d.record(true)
        assertEquals(6, d.level)
        assertEquals(0, d.streak)
    }

    @Test fun dropsAfterError() {
        val d = Difficulty(level = 5).record(true).record(false)
        assertEquals(4, d.level)
    }

    @Test fun errorBreaksStreak() {
        var d = Difficulty(level = 5, downAfter = 2)
        d = d.record(true).record(true).record(false).record(true)
        assertEquals(5, d.level)
    }

    @Test fun clampedToBounds() {
        assertEquals(Difficulty.MIN_LEVEL, Difficulty().record(false).level)
        val top = Difficulty(level = Difficulty.MAX_LEVEL)
        assertEquals(Difficulty.MAX_LEVEL, top.record(true).record(true).record(true).level)
    }

    @Test fun seededSimulationStaysInRange() {
        val r = Random(123)
        var d = Difficulty()
        repeat(1000) { d = d.record(r.nextDouble() < 0.7); assertTrue(d.level in 1..20) }
    }

    @Test fun skillLevelsAreIndependent() {
        val s = SkillLevels().record(Skill.MEMORY, false).record(Skill.SPEED, true)
        assertEquals(1, s[Skill.MEMORY].level)
        assertEquals(1, s[Skill.SPEED].level)
        assertEquals(1, s[Skill.ATTENTION].level)
        val up = (1..3).fold(SkillLevels()) { acc, _ -> acc.record(Skill.MEMORY, true) }
        assertEquals(2, up[Skill.MEMORY].level)
        assertEquals(1, up[Skill.SPEED].level)
    }
}
