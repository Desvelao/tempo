package app.trainer.core

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StroopGameTest {
    private val game = StroopGame()

    @Test fun inkAlwaysDiffersFromWordAndIsAnOption() {
        val r = Random(1)
        for (level in 1..20) repeat(30) {
            val round = game.generateRound(level, r)
            assertNotEquals(round.word, round.ink)
            assertTrue(round.ink in round.options)
            assertEquals(game.colorCountFor(level), round.options.size)
        }
    }

    @Test fun evaluateAndTimeout() {
        val round = StroopRound(InkColor.RED, InkColor.BLUE, listOf(InkColor.RED, InkColor.BLUE, InkColor.GREEN))
        assertTrue(game.evaluate(round, InkColor.BLUE))
        assertFalse(game.evaluate(round, InkColor.RED))
        assertNull(game.timeoutAnswer(round))
    }

    @Test fun scalingAndDeterminism() {
        assertEquals(3, game.colorCountFor(1)); assertEquals(6, game.colorCountFor(20))
        assertTrue(game.timeLimitMs(20) < game.timeLimitMs(1))
        assertEquals(game.generateRound(9, Random(3)), game.generateRound(9, Random(3)))
    }
}

class QuickMathGameTest {
    private val game = QuickMathGame()

    @Test fun optionsContainDistinctCorrectAnswer() {
        val r = Random(2)
        for (level in 1..20) repeat(30) {
            val round = game.generateRound(level, r)
            assertEquals(4, round.options.toSet().size)
            assertTrue(round.answer in round.options)
            assertTrue(game.evaluate(round, round.answer))
            assertFalse(game.evaluate(round, round.answer + 1))
        }
    }

    @Test fun operationsScaleWithDifficulty() {
        assertEquals(listOf(MathOp.ADD, MathOp.SUB), game.opsFor(1))
        assertTrue(MathOp.MUL in game.opsFor(10))
        val r = Random(4)
        repeat(100) { assertTrue(game.generateRound(1, r).op != MathOp.MUL) }
        assertTrue(game.maxOperandFor(10, MathOp.ADD) > game.maxOperandFor(1, MathOp.ADD))
    }

    @Test fun subtractionNeverNegative() {
        val r = Random(6)
        repeat(300) {
            val round = game.generateRound(3, r)
            if (round.op == MathOp.SUB) assertTrue(round.answer >= 0)
        }
    }
}

class NBackGameTest {
    @Test fun matchFlagMatchesHistory() {
        val game = NBackGame()
        val r = Random(11)
        val seen = mutableListOf<Int>()
        repeat(200) {
            val round = game.generateRound(1, r)
            assertEquals(1, round.n)
            if (seen.size >= round.n) assertEquals(seen[seen.size - round.n] == round.position, round.isMatch)
            else assertFalse(round.isMatch)
            assertTrue(round.position in 0 until NBackGame.CELLS)
            seen += round.position
        }
    }

    @Test fun nScalesAndTimeoutMeansNoMatch() {
        val game = NBackGame()
        assertEquals(1, game.nFor(1)); assertEquals(2, game.nFor(8)); assertEquals(3, game.nFor(20))
        val round = NBackRound(0, 2, false)
        assertEquals(false, game.timeoutAnswer(round))
        assertTrue(game.evaluate(round, game.timeoutAnswer(round)))
        assertFalse(game.evaluate(NBackRound(0, 2, true), false))
    }

    @Test fun resetClearsStreamAndIsDeterministic() {
        val a = NBackGame(); val b = NBackGame()
        val ra = Random(5); val rb = Random(5)
        assertEquals(List(20) { a.generateRound(2, ra) }, List(20) { b.generateRound(2, rb) })
        a.reset()
        assertFalse(a.generateRound(2, Random(1)).isMatch)
    }
}

class RuleSwitchGameTest {
    @Test fun correctAnswerFollowsActiveRule() {
        val game = RuleSwitchGame()
        val c = RuleSwitchRound(CardColor.BLUE, CardShape.CIRCLE, Rule.COLOR, false)
        assertEquals(1, c.correctAnswer)
        assertTrue(game.evaluate(c, 1)); assertFalse(game.evaluate(c, 0))
        val s = c.copy(rule = Rule.SHAPE)
        assertEquals(0, s.correctAnswer)
    }

    @Test fun switchFlagReflectsRuleChange() {
        val game = RuleSwitchGame()
        val r = Random(8)
        var prev: Rule? = null
        var switches = 0
        repeat(500) {
            val round = game.generateRound(10, r)
            if (prev != null) assertEquals(round.rule != prev, round.isSwitch) else assertFalse(round.isSwitch)
            if (round.isSwitch) switches++
            prev = round.rule
        }
        assertTrue(switches in 50..200)
    }

    @Test fun switchRateGrowsAndResetWorks() {
        val game = RuleSwitchGame()
        assertTrue(game.switchProbabilityFor(20) > game.switchProbabilityFor(1))
        assertTrue(game.switchProbabilityFor(20) <= 0.5)
        game.generateRound(5, Random(1)); game.reset()
        assertFalse(game.generateRound(20, Random(2)).isSwitch)
    }
}

class OddOneOutGameTest {
    private val game = OddOneOutGame()

    @Test fun exactlyOneOddCellInsideTheGrid() {
        val r = Random(5)
        for (level in 1..20) repeat(30) {
            val round = game.generateRound(level, r)
            assertTrue(round.oddIndex in 0 until round.size * round.size)
            assertNotEquals(round.base, round.odd)
            assertTrue(game.evaluate(round, round.oddIndex))
            assertFalse(game.evaluate(round, (round.oddIndex + 1) % (round.size * round.size)))
        }
    }

    @Test fun scalingAndDeterminism() {
        assertEquals(3, game.sizeFor(1)); assertEquals(5, game.sizeFor(20))
        assertEquals(0, game.tierFor(1)); assertEquals(2, game.tierFor(20))
        assertTrue(game.timeLimitMs(20) < game.timeLimitMs(1))
        assertEquals(game.generateRound(9, Random(3)), game.generateRound(9, Random(3)))
    }
}

class ReverseSequenceGameTest {
    private val game = ReverseSequenceGame()

    @Test fun lengthGrowsAndDigitsNeverRepeatInARow() {
        assertEquals(3, game.lengthFor(1)); assertEquals(9, game.lengthFor(20))
        val r = Random(6)
        for (level in 1..20) repeat(30) {
            val d = game.generateRound(level, r).digits
            assertEquals(game.lengthFor(level), d.size)
            assertTrue(d.all { it in 1..9 })
            assertTrue(d.zipWithNext().none { (a, b) -> a == b })
        }
    }

    @Test fun onlyTheReversedSequenceIsCorrect() {
        val round = ReverseSequenceRound(listOf(1, 2, 3))
        assertTrue(game.evaluate(round, listOf(3, 2, 1)))
        assertFalse(game.evaluate(round, listOf(1, 2, 3)))
        assertFalse(game.evaluate(round, listOf(3, 2)))
    }
}

class ReactionTapGameTest {
    private val game = ReactionTapGame()

    @Test fun delayStaysInRangeAndThresholdTightens() {
        val r = Random(7)
        repeat(100) { assertTrue(game.generateRound(5, r).delayMs in 1000L..3000L) }
        assertTrue(game.thresholdFor(20) < game.thresholdFor(1))
        assertEquals(300L, game.thresholdFor(20))
    }

    @Test fun evaluateHandlesEarlyAndMissedTaps() {
        val round = ReactionRound(1500, 400)
        assertTrue(game.evaluate(round, 250)); assertTrue(game.evaluate(round, 400))
        assertFalse(game.evaluate(round, 401))
        assertFalse(game.evaluate(round, ReactionTapGame.EARLY))
        assertFalse(game.evaluate(round, ReactionTapGame.NO_RESPONSE))
        assertEquals(ReactionTapGame.NO_RESPONSE, game.timeoutAnswer(round))
    }
}

class PairMatchGameTest {
    private val game = PairMatchGame()

    @Test fun everySymbolAppearsExactlyTwice() {
        val r = Random(8)
        for (level in 1..20) repeat(20) {
            val round = game.generateRound(level, r)
            assertEquals(game.pairCountFor(level), round.pairCount)
            assertEquals(round.pairCount * 2, round.cards.size)
            assertTrue(round.cards.groupBy { it }.values.all { it.size == 2 })
            assertTrue(round.cards.all { it in PairMatchGame.SYMBOLS.indices })
        }
    }

    @Test fun matchingAndBudget() {
        val round = PairMatchRound(3, listOf(0, 1, 2, 0, 1, 2), 3)
        assertTrue(game.isMatch(round, 0, 3))
        assertFalse(game.isMatch(round, 0, 1))
        assertFalse(game.isMatch(round, 2, 2))
        assertTrue(game.evaluate(round, 9)); assertFalse(game.evaluate(round, 10))
        assertFalse(game.evaluate(round, 0))
    }

    @Test fun scalingAndDeterminism() {
        assertEquals(3, game.pairCountFor(1)); assertEquals(9, game.pairCountFor(20))
        assertEquals(3, game.columnsFor(6)); assertEquals(4, game.columnsFor(12)); assertEquals(5, game.columnsFor(18))
        assertEquals(game.generateRound(9, Random(3)), game.generateRound(9, Random(3)))
    }
}

class TrailAlternateGameTest {
    private val game = TrailAlternateGame()

    @Test fun labelsAlternateNumbersAndLetters() {
        assertEquals(listOf("1", "A", "2", "B", "3", "C"), (0 until 6).map(game::labelFor))
        assertEquals(listOf("7", "G", "8", "H"), (12 until 16).map(game::labelFor))
    }

    @Test fun everyStepHasADistinctCellInsideTheGrid() {
        val r = Random(9)
        for (level in 1..20) repeat(20) {
            val round = game.generateRound(level, r)
            assertEquals(game.lengthFor(level), round.labels.size)
            assertEquals(round.labels.size, round.cellOfStep.toSet().size)
            assertTrue(round.cellOfStep.all { it in 0 until round.cells })
            assertEquals(0, round.cells % round.columns)
        }
    }

    @Test fun scalingAndErrorBudget() {
        assertEquals(6, game.lengthFor(1)); assertEquals(15, game.lengthFor(20))
        val round = game.generateRound(1, Random(1))
        assertTrue(game.evaluate(round, 0)); assertTrue(game.evaluate(round, 1))
        assertFalse(game.evaluate(round, 2))
        assertEquals(game.generateRound(9, Random(3)), game.generateRound(9, Random(3)))
    }
}

class MentalRotationGameTest {
    private val game = MentalRotationGame()

    @Test fun rotationAndMirrorBehaveLikeGeometry() {
        // An L-shape in a 3x3 grid: cells (0,0), (1,0), (2,0), (2,1).
        val l = listOf(true, false, false, true, false, false, true, true, false)
        assertEquals(l, MentalRotationGame.rotate(l, 3, 0))
        assertEquals(l, MentalRotationGame.rotate(l, 3, 4))
        assertEquals(l, MentalRotationGame.rotate(MentalRotationGame.rotate(l, 3, 1), 3, 3))
        assertEquals(l, MentalRotationGame.mirror(MentalRotationGame.mirror(l, 3), 3))
        // One clockwise quarter turn sends the left column to the top row.
        assertEquals(listOf(true, true, true, true, false, false, false, false, false), MentalRotationGame.rotate(l, 3, 1))
        assertTrue(game.isUsable(l, 3))
    }

    @Test fun symmetricPatternsAreNotUsable() {
        assertFalse(game.isUsable(List(9) { it == 4 }, 3)) // single centre cell: all rotations equal
        assertFalse(game.isUsable(listOf(true, false, true, false, false, false, true, false, true), 3)) // 4-fold symmetric
        assertFalse(game.isUsable(listOf(true, true, false, false, false, false, false, false, false), 3)) // domino: looks the same mirrored
        assertFalse(game.isUsable(listOf(true, true, false, true, true, false, false, false, false), 3)) // square block
    }

    @Test fun exactlyOneOptionIsTheRotatedTargetAndTheRestAreMirrored() {
        val r = Random(11)
        for (level in 1..20) repeat(25) {
            val round = game.generateRound(level, r)
            val n = round.size
            assertEquals(game.sizeFor(level), n)
            assertEquals(4, round.options.size)
            assertEquals(4, round.options.map { MentalRotationGame.canonical(it, n) }.toSet().size) // four different shapes
            val shapes = (0..3).map { MentalRotationGame.canonical(MentalRotationGame.rotate(round.target, n, it), n) }
            val matching = round.options.indices.filter { MentalRotationGame.canonical(round.options[it], n) in shapes }
            assertEquals(listOf(round.correctIndex), matching)
            assertFalse(round.options[round.correctIndex] == round.target) // it is actually turned
            assertTrue(game.evaluate(round, round.correctIndex))
            for (i in 0..3) if (i != round.correctIndex) assertFalse(game.evaluate(round, i))
            assertEquals(game.cellsFor(level), round.target.count { it })
        }
    }

    @Test fun scalingAndDeterminism() {
        assertEquals(3, game.sizeFor(1)); assertEquals(5, game.sizeFor(20))
        assertTrue(game.cellsFor(20) <= 5 * 5 - 2)
        assertTrue(game.timeLimitMs(20) < game.timeLimitMs(1))
        assertEquals(game.generateRound(9, Random(3)), game.generateRound(9, Random(3)))
    }
}

class VisualSearchGameTest {
    private val game = VisualSearchGame()

    @Test fun targetAppearsExactlyOnceAndNeverAsADistractor() {
        val r = Random(21)
        for (level in 1..20) repeat(25) {
            val round = game.generateRound(level, r)
            assertEquals(round.size * round.size, round.cells.size)
            assertEquals(1, round.cells.count { it == round.target })
            assertEquals(round.target, round.cells[round.targetIndex])
            assertTrue(game.evaluate(round, round.targetIndex))
            assertFalse(game.evaluate(round, (round.targetIndex + 1) % round.cells.size))
            assertTrue(round.cells.toSet().size - 1 <= game.varietyFor(level))
        }
    }

    @Test fun setsNeverContainTheirOwnTarget() {
        for ((target, pool) in VisualSearchGame.SETS) {
            assertFalse(target in pool)
            assertTrue(pool.size >= 3)
        }
    }

    @Test fun scalingAndDeterminism() {
        assertEquals(4, game.sizeFor(1)); assertEquals(6, game.sizeFor(20))
        assertEquals(1, game.varietyFor(1)); assertEquals(3, game.varietyFor(20))
        assertTrue(game.timeLimitMs(20) < game.timeLimitMs(1))
        assertEquals(game.generateRound(9, Random(3)), game.generateRound(9, Random(3)))
    }
}

class DigitSpanGameTest {
    private val game = DigitSpanGame()

    @Test fun lengthGrowsDigitsNeverRepeatAndSumIsSometimesWrong() {
        assertEquals(3, game.lengthFor(1)); assertEquals(8, game.lengthFor(20))
        val r = Random(31)
        var right = 0; var wrong = 0
        for (level in 1..20) repeat(30) {
            val round = game.generateRound(level, r)
            assertEquals(game.lengthFor(level), round.digits.size)
            assertTrue(round.digits.all { it in 1..9 })
            assertTrue(round.digits.zipWithNext().none { (a, b) -> a == b })
            assertTrue(round.a in 2..game.addendMaxFor(level) && round.b in 2..game.addendMaxFor(level))
            if (round.sumIsCorrect) right++ else wrong++
        }
        assertTrue(right > 100 && wrong > 100)
    }

    @Test fun bothPartsMustBeRight() {
        val round = DigitSpanRound(listOf(4, 7, 2), a = 6, b = 5, shownSum = 11)
        assertTrue(round.sumIsCorrect)
        assertTrue(game.evaluate(round, DigitSpanAnswer(true, listOf(4, 7, 2))))
        assertFalse(game.evaluate(round, DigitSpanAnswer(false, listOf(4, 7, 2)))) // digits right, sum verdict wrong
        assertFalse(game.evaluate(round, DigitSpanAnswer(true, listOf(2, 7, 4)))) // verdict right, wrong order
        assertFalse(game.evaluate(round, DigitSpanAnswer(true, listOf(4, 7)))) // incomplete
        val off = DigitSpanRound(listOf(1, 2, 3), a = 6, b = 5, shownSum = 12)
        assertFalse(off.sumIsCorrect)
        assertTrue(game.evaluate(off, DigitSpanAnswer(false, listOf(1, 2, 3))))
    }

    @Test fun deterministicForASeed() = assertEquals(game.generateRound(9, Random(3)), game.generateRound(9, Random(3)))
}

