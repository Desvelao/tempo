package app.trainer.core

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AdaptivePlannerTest {
    private val day = 24L * 60 * 60 * 1000
    private val now = 100 * day
    private val none = emptyMap<Skill, Long>()

    @Test fun returnsDistinctSkillsOfRequestedCount() {
        for (count in 3..5) {
            val plan = SessionPlanner.planAdaptive(emptyMap(), none, now, emptySet(), Random(count), count)
            assertEquals(count, plan.size)
            assertEquals(count, plan.toSet().size)
        }
    }

    @Test fun isDeterministicForASeed() {
        val levels = mapOf(Skill.MEMORY to 3, Skill.SPEED to 15)
        assertEquals(
            SessionPlanner.planAdaptive(levels, none, now, emptySet(), Random(5)),
            SessionPlanner.planAdaptive(levels, none, now, emptySet(), Random(5)),
        )
    }

    @Test fun lowerLevelWeighsMoreThanHigher() {
        val levels = mapOf(Skill.MEMORY to 2, Skill.SPEED to 18)
        val low = SessionPlanner.weight(Skill.MEMORY, levels, none, now, emptySet())
        val high = SessionPlanner.weight(Skill.SPEED, levels, none, now, emptySet())
        assertTrue(low > high)
    }

    @Test fun staleSkillWeighsMoreThanFreshOne() {
        val last = mapOf(Skill.MEMORY to now, Skill.SPEED to now - 10 * day)
        val levels = mapOf(Skill.MEMORY to 5, Skill.SPEED to 5)
        assertTrue(
            SessionPlanner.weight(Skill.SPEED, levels, last, now, emptySet()) >
                SessionPlanner.weight(Skill.MEMORY, levels, last, now, emptySet()),
        )
    }

    @Test fun focusDoublesTheWeight() {
        val plain = SessionPlanner.weight(Skill.ATTENTION, emptyMap(), none, now, emptySet())
        val focused = SessionPlanner.weight(Skill.ATTENTION, emptyMap(), none, now, setOf(Skill.ATTENTION))
        assertEquals(plain * 2, focused, 1e-9)
    }

    @Test fun everySkillStaysReachable() {
        val levels = Skill.entries.associateWith { 20 }
        val seen = mutableSetOf<Skill>()
        val random = Random(1)
        repeat(300) { seen += SessionPlanner.planAdaptive(levels, none, now, emptySet(), random, 3) }
        assertEquals(Skill.entries.toSet(), seen)
    }

    @Test fun weakSkillIsPickedFirstMoreOftenThanStrongOne() {
        val levels = Skill.entries.associateWith { 20 } + (Skill.FLEXIBILITY to 1)
        val last = Skill.entries.associateWith { now }
        val random = Random(42)
        val firsts = (1..600).map { SessionPlanner.planAdaptive(levels, last, now, emptySet(), random, 3).first() }
        assertTrue(firsts.count { it == Skill.FLEXIBILITY } > firsts.count { it == Skill.MEMORY } * 3)
    }

    @Test fun adaptiveModeUsesNoList() =
        assertNull(ListPlanner.pick(SessionMode.ADAPTIVE, listOf(GameList(1, "A", listOf("x"), false)), 1, Random(1)))
}
