package app.trainer.core

import kotlin.random.Random

object SessionPlanner {
    const val MIN_GAMES = 3
    const val MAX_GAMES = 5

    /** [count] (3..5) distinct skills in random order, so a session spans different skills. */
    fun plan(random: Random, count: Int = 4): List<Skill> {
        require(count in MIN_GAMES..MAX_GAMES) { "count must be $MIN_GAMES..$MAX_GAMES" }
        return Skill.entries.shuffled(random).take(count)
    }

    private const val DAY_MS = 24L * 60 * 60 * 1000
    private const val STALE_CAP_DAYS = 14
    private const val FOCUS_FACTOR = 2.0

    /**
     * Relative chance that [skill] is picked. Low levels and skills not played for a while weigh more, and a skill
     * in [focus] counts double. Always positive, so every skill stays reachable.
     */
    fun weight(skill: Skill, levels: Map<Skill, Int>, lastPlayed: Map<Skill, Long>, now: Long, focus: Set<Skill>): Double {
        val level = (levels[skill] ?: Difficulty.MIN_LEVEL).coerceIn(Difficulty.MIN_LEVEL, Difficulty.MAX_LEVEL)
        val lowLevel = (Difficulty.MAX_LEVEL - level + 1).toDouble()
        val days = lastPlayed[skill]?.let { ((now - it) / DAY_MS).toInt().coerceIn(0, STALE_CAP_DAYS) } ?: STALE_CAP_DAYS
        val base = lowLevel + days * 0.5
        return if (skill in focus) base * FOCUS_FACTOR else base
    }

    /** [count] (3..5) distinct skills drawn without replacement, weighted by [weight]. Order is the draw order. */
    fun planAdaptive(
        levels: Map<Skill, Int>,
        lastPlayed: Map<Skill, Long>,
        now: Long,
        focus: Set<Skill>,
        random: Random,
        count: Int = 4,
    ): List<Skill> {
        require(count in MIN_GAMES..MAX_GAMES) { "count must be $MIN_GAMES..$MAX_GAMES" }
        val pool = Skill.entries.toMutableList()
        val picked = mutableListOf<Skill>()
        repeat(count) {
            val weights = pool.map { weight(it, levels, lastPlayed, now, focus) }
            var roll = random.nextDouble() * weights.sum()
            var index = weights.indexOfFirst { w -> roll -= w; roll < 0 }
            if (index < 0) index = pool.lastIndex
            picked += pool.removeAt(index)
        }
        return picked
    }
}

/** A saved, ordered list of game ids. [shuffle] randomises the order on every run. */
data class GameList(val id: Long, val name: String, val gameIds: List<String>, val shuffle: Boolean)

/** How the daily session chooses its games. */
enum class SessionMode { RANDOM, LIST, ANY_LIST, ADAPTIVE }

object ListPlanner {
    /**
     * Distinct [gameIds] in saved order, or shuffled when [shuffle] is on. When [known] is given, ids outside it are
     * dropped, so a list restored from a newer version of the app cannot ask for a game this one does not have.
     */
    fun plan(gameIds: List<String>, shuffle: Boolean, random: Random, known: Set<String>? = null): List<String> {
        val distinct = gameIds.filter { known == null || it in known }.distinct()
        return if (shuffle) distinct.shuffled(random) else distinct
    }

    /** Picks the list a session should use, or null when the session should fall back to random games. */
    fun pick(mode: SessionMode, lists: List<GameList>, selectedId: Long, random: Random): GameList? {
        val usable = lists.filter { it.gameIds.isNotEmpty() }
        return when (mode) {
            SessionMode.RANDOM, SessionMode.ADAPTIVE -> null
            SessionMode.LIST -> usable.firstOrNull { it.id == selectedId }
            SessionMode.ANY_LIST -> usable.randomOrNull(random)
        }
    }
}
