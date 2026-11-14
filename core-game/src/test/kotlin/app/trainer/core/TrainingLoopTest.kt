package app.trainer.core

import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class StreakTest {
    private val today = LocalDate.of(2026, 11, 12)

    @Test fun emptyIsZero() = assertEquals(0, Streak.compute(emptyList(), today))

    @Test fun countsConsecutiveDaysIncludingToday() {
        val days = listOf(today, today.minusDays(1), today.minusDays(2), today.minusDays(4))
        assertEquals(3, Streak.compute(days, today))
    }

    @Test fun yesterdayKeepsStreakAlive() =
        assertEquals(2, Streak.compute(listOf(today.minusDays(1), today.minusDays(2)), today))

    @Test fun gapBreaksStreak() = assertEquals(0, Streak.compute(listOf(today.minusDays(2)), today))

    @Test fun duplicatesIgnored() = assertEquals(1, Streak.compute(listOf(today, today, today), today))
}

class SessionPlannerTest {
    @Test fun distinctSkillsOfRequestedSize() {
        for (count in 3..5) {
            val plan = SessionPlanner.plan(Random(count), count)
            assertEquals(count, plan.size)
            assertEquals(count, plan.toSet().size)
        }
    }

    @Test fun deterministicAndRejectsBadCount() {
        assertEquals(SessionPlanner.plan(Random(1)), SessionPlanner.plan(Random(1)))
        assertFailsWith<IllegalArgumentException> { SessionPlanner.plan(Random(1), 2) }
        assertFailsWith<IllegalArgumentException> { SessionPlanner.plan(Random(1), 6) }
    }

    @Test fun differentSeedsVaryOrder() {
        assertTrue((1..20).map { SessionPlanner.plan(Random(it)) }.toSet().size > 1)
    }
}

class ReminderTimeTest {
    private val zone = ZoneId.of("UTC")
    private fun at(h: Int, m: Int) = ZonedDateTime.of(2026, 11, 12, h, m, 30, 0, zone)

    @Test fun laterTodayWhenTimeNotPassed() =
        assertEquals(3_600_000L - 30_000L, ReminderTime.delayUntilNextMillis(at(8, 0), 9, 0))

    @Test fun tomorrowWhenTimePassed() =
        assertEquals(24 * 3_600_000L - 30_000L, ReminderTime.delayUntilNextMillis(at(9, 0), 9, 0))

    @Test fun exactlyNowMeansTomorrow() {
        val now = ZonedDateTime.of(2026, 11, 12, 9, 0, 0, 0, zone)
        assertEquals(24 * 3_600_000L, ReminderTime.delayUntilNextMillis(now, 9, 0))
    }

    @Test fun alwaysPositiveAndWithinADay() {
        for (h in 0..23) for (m in listOf(0, 30, 59)) {
            val d = ReminderTime.delayUntilNextMillis(at(12, 15), h, m)
            assertTrue(d in 1..24 * 3_600_000L)
        }
    }
}
