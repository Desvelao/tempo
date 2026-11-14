package app.trainer.data

import androidx.room.Dao
import app.trainer.core.Skill
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** The most recent play time of one skill. */
data class SkillLastPlayed(val skill: Skill, val lastPlayedAt: Long)

@Dao
interface GameResultDao {
    @Insert
    suspend fun insert(result: GameResultEntity): Long

    @Query("SELECT score FROM game_result WHERE gameId = :gameId ORDER BY playedAt DESC LIMIT 1")
    suspend fun lastScore(gameId: String): Int?

    @Query("SELECT * FROM game_result WHERE sessionId = :sessionId ORDER BY playedAt ASC")
    suspend fun forSession(sessionId: Long): List<GameResultEntity>

    @Query("SELECT * FROM game_result ORDER BY playedAt DESC")
    fun observeAll(): Flow<List<GameResultEntity>>

    @Query("SELECT skill, MAX(playedAt) AS lastPlayedAt FROM game_result GROUP BY skill")
    suspend fun lastPlayedBySkill(): List<SkillLastPlayed>

    @Query("DELETE FROM game_result")
    suspend fun clear()

    @Query("SELECT * FROM game_result ORDER BY playedAt ASC")
    suspend fun all(): List<GameResultEntity>

    @Insert
    suspend fun insertAll(results: List<GameResultEntity>)
}
