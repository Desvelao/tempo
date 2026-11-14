package app.trainer.core

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ListPlannerTest {
    private val a = GameList(1, "A", listOf("x", "y", "z"), shuffle = false)
    private val b = GameList(2, "B", listOf("y"), shuffle = true)
    private val empty = GameList(3, "E", emptyList(), shuffle = false)

    @Test fun keepsOrderAndDedupes() =
        assertEquals(listOf("x", "y", "z"), ListPlanner.plan(listOf("x", "y", "x", "z"), false, Random(1)))

    @Test fun shuffleIsSeededAndKeepsMembers() {
        val ids = listOf("a", "b", "c", "d", "e")
        val one = ListPlanner.plan(ids, true, Random(7))
        assertEquals(one, ListPlanner.plan(ids, true, Random(7)))
        assertEquals(ids.toSet(), one.toSet())
    }

    @Test fun unknownIdsAreDropped() {
        val known = setOf("x", "z")
        assertEquals(listOf("x", "z"), ListPlanner.plan(listOf("x", "future", "z"), false, Random(1), known))
        assertEquals(emptyList(), ListPlanner.plan(listOf("future"), false, Random(1), known))
        assertEquals(known, ListPlanner.plan(listOf("x", "future", "z"), true, Random(2), known).toSet())
    }

    @Test fun randomModeUsesNoList() = assertNull(ListPlanner.pick(SessionMode.RANDOM, listOf(a), 1, Random(1)))

    @Test fun specificListAndFallbacks() {
        assertEquals(b, ListPlanner.pick(SessionMode.LIST, listOf(a, b), 2, Random(1)))
        assertNull(ListPlanner.pick(SessionMode.LIST, listOf(a, b), 99, Random(1)))
        assertNull(ListPlanner.pick(SessionMode.LIST, listOf(empty), 3, Random(1)))
    }

    @Test fun anyListSkipsEmptyOnes() {
        repeat(20) { assertEquals(true, ListPlanner.pick(SessionMode.ANY_LIST, listOf(a, empty, b), 0, Random(it))!!.id != 3L) }
        assertNull(ListPlanner.pick(SessionMode.ANY_LIST, listOf(empty), 0, Random(1)))
    }
}
