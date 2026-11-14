package app.trainer.data

import app.trainer.core.Game
import app.trainer.core.Skill
import app.trainer.core.Streak
import app.trainer.ui.ResultSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class ResultRepository(
    private val results: GameResultDao,
    private val ratings: SkillRatingDao,
    private val sessions: SessionDao,
) {
    val all: Flow<List<GameResultEntity>> = results.observeAll()

    /** Deletes every result, session and skill rating (callers wrap this in a transaction). */
    suspend fun clearProgress() {
        results.clear()
        sessions.clear()
        ratings.clear()
    }

    /** Stored difficulty level per skill (skills never played are absent). */
    val levels: Flow<Map<Skill, Int>> = ratings.observeAll().map { list -> list.associate { it.skill to it.level } }

    /** Consecutive days with a completed session. */
    val streak: Flow<Int> = sessions.observeFinishedAt().map { computeStreak(it) }

    /** Days (local time) on which at least one session was completed. */
    val sessionDays: Flow<Set<LocalDate>> = sessions.observeFinishedAt().map { list ->
        val zone = ZoneId.systemDefault()
        list.map { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }.toSet()
    }

    /** When each skill was last played (skills never played are absent). */
    suspend fun lastPlayedBySkill(): Map<Skill, Long> = results.lastPlayedBySkill().associate { it.skill to it.lastPlayedAt }

    suspend fun lastScore(gameId: String): Int? = results.lastScore(gameId)

    suspend fun sessionResults(sessionId: Long): List<GameResultEntity> = results.forSession(sessionId)

    suspend fun save(result: GameResultEntity) = results.insert(result)

    /** Stores the result, and moves the skill's rating to the level the staircase ended on. */
    suspend fun saveGame(
        game: Game<*, *>,
        summary: ResultSummary,
        level: Int,
        sessionId: Long? = null,
        playedAt: Long = System.currentTimeMillis(),
    ) {
        save(
            GameResultEntity(
                sessionId = sessionId, gameId = game.id, skill = game.skill, score = summary.score,
                accuracy = summary.accuracy, avgReactionMs = summary.avgReactionMs,
                difficulty = level, playedAt = playedAt,
            ),
        )
        ratings.upsert(SkillRatingEntity(game.skill, summary.finalLevel, playedAt))
    }

    suspend fun startSession(now: Long = System.currentTimeMillis()): Long = sessions.insert(SessionEntity(startedAt = now))

    suspend fun finishSession(id: Long, now: Long = System.currentTimeMillis()) = sessions.finish(id, now)

    suspend fun currentStreak(): Int = streak.first()

    private fun computeStreak(finishedAt: List<Long>): Int {
        val zone = ZoneId.systemDefault()
        val days = finishedAt.map { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }
        return Streak.compute(days, LocalDate.now(zone))
    }
}
