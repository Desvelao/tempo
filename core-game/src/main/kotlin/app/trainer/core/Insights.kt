package app.trainer.core

import java.time.Instant
import java.time.ZoneId

/** The part of a stored result the insights need; keeps this file free of database types. */
data class InsightResult(
    val skill: Skill,
    val gameId: String,
    val score: Int,
    val accuracy: Double,
    val playedAt: Long,
)

/** Pure summaries over the played history. Nothing here reads the clock: callers pass `now`. */
object Insights {
    private const val WEEK_MS = 7L * 24 * 60 * 60 * 1000
    const val MIN_RESULTS_PER_HOUR = 3

    /** The skill with the lowest level (ties go to the first in [Skill] order), or null when nothing was played. */
    fun weakest(levels: Map<Skill, Int>): Skill? = Skill.entries.filter { it in levels }.minByOrNull { levels.getValue(it) }

    /** The skill with the highest level (ties go to the first in [Skill] order), or null when nothing was played. */
    fun strongest(levels: Map<Skill, Int>): Skill? = Skill.entries.filter { it in levels }.maxByOrNull { levels.getValue(it) }

    /**
     * Mean score of the last 7 days minus the mean of the 7 days before, per skill. A skill is absent when either
     * week has no result, because the difference would be meaningless.
     */
    fun weeklyChange(results: List<InsightResult>, now: Long): Map<Skill, Double> =
        Skill.entries.mapNotNull { skill ->
            val mine = results.filter { it.skill == skill }
            val thisWeek = mine.filter { it.playedAt in (now - WEEK_MS + 1)..now }
            val lastWeek = mine.filter { it.playedAt in (now - 2 * WEEK_MS + 1)..(now - WEEK_MS) }
            if (thisWeek.isEmpty() || lastWeek.isEmpty()) null
            else skill to thisWeek.map { it.score }.average() - lastWeek.map { it.score }.average()
        }.toMap()

    /**
     * The hour of day (0..23, in [zone]) with the best mean accuracy, considering only hours with at least
     * [MIN_RESULTS_PER_HOUR] results. Null when no hour has enough data.
     */
    fun bestHour(results: List<InsightResult>, zone: ZoneId): Int? =
        results.groupBy { Instant.ofEpochMilli(it.playedAt).atZone(zone).hour }
            .filterValues { it.size >= MIN_RESULTS_PER_HOUR }
            .mapValues { (_, v) -> v.map { it.accuracy }.average() }
            .entries.sortedWith(compareByDescending<Map.Entry<Int, Double>> { it.value }.thenBy { it.key })
            .firstOrNull()?.key

    /** Highest score per game id. */
    fun personalBests(results: List<InsightResult>): Map<String, Int> =
        results.groupBy { it.gameId }.mapValues { (_, v) -> v.maxOf { it.score } }
}
