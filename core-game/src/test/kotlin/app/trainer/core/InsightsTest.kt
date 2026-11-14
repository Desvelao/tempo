package app.trainer.core

import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class InsightsTest {
    private val hour = 60L * 60 * 1000
    private val day = 24 * hour
    private val now = 100 * day

    private fun r(skill: Skill, score: Int, at: Long, acc: Double = 0.5, game: String = "g") =
        InsightResult(skill, game, score, acc, at)

    @Test fun weakestAndStrongestIgnoreUnplayedSkills() {
        val levels = mapOf(Skill.SPEED to 9, Skill.MEMORY to 4, Skill.ATTENTION to 12)
        assertEquals(Skill.MEMORY, Insights.weakest(levels))
        assertEquals(Skill.ATTENTION, Insights.strongest(levels))
        assertNull(Insights.weakest(emptyMap()))
        assertNull(Insights.strongest(emptyMap()))
    }

    @Test fun tiesGoToTheFirstSkill() {
        val levels = mapOf(Skill.SPEED to 5, Skill.MEMORY to 5)
        assertEquals(Skill.MEMORY, Insights.weakest(levels))
        assertEquals(Skill.MEMORY, Insights.strongest(levels))
    }

    @Test fun weeklyChangeComparesTwoWeeks() {
        val results = listOf(
            r(Skill.MEMORY, 100, now - 1 * day), r(Skill.MEMORY, 200, now - 2 * day), // this week: 150
            r(Skill.MEMORY, 100, now - 9 * day),                                      // last week: 100
            r(Skill.SPEED, 80, now - 1 * day),                                        // no last week
        )
        val change = Insights.weeklyChange(results, now)
        assertEquals(50.0, change.getValue(Skill.MEMORY), 1e-9)
        assertEquals(setOf(Skill.MEMORY), change.keys)
    }

    @Test fun weekBoundariesDoNotOverlap() {
        val results = listOf(r(Skill.MEMORY, 10, now - 7 * day), r(Skill.MEMORY, 30, now - 7 * day + 1))
        // Exactly 7 days ago belongs to the previous week; one millisecond later to this week.
        assertEquals(20.0, Insights.weeklyChange(results, now).getValue(Skill.MEMORY), 1e-9)
    }

    @Test fun bestHourNeedsEnoughResultsAndPicksBestAccuracy() {
        val results = buildList {
            repeat(3) { add(r(Skill.MEMORY, 1, 9 * hour + it * day, acc = 0.6)) }   // 09:00 UTC
            repeat(3) { add(r(Skill.MEMORY, 1, 20 * hour + it * day, acc = 0.9)) }  // 20:00 UTC
            add(r(Skill.MEMORY, 1, 14 * hour, acc = 1.0))                           // too few at 14:00
        }
        assertEquals(20, Insights.bestHour(results, ZoneOffset.UTC))
        assertNull(Insights.bestHour(results.take(2), ZoneOffset.UTC))
    }

    @Test fun bestHourRespectsTheTimeZone() {
        val results = List(3) { r(Skill.MEMORY, 1, 20 * hour + it * day, acc = 0.9) }
        assertEquals(22, Insights.bestHour(results, ZoneOffset.ofHours(2)))
    }

    @Test fun personalBestsTakeTheMaxPerGame() {
        val results = listOf(r(Skill.MEMORY, 10, 1, game = "a"), r(Skill.MEMORY, 30, 2, game = "a"), r(Skill.SPEED, 5, 3, game = "b"))
        assertEquals(mapOf("a" to 30, "b" to 5), Insights.personalBests(results))
    }
}
