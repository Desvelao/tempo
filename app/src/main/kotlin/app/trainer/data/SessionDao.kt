package app.trainer.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Insert
    suspend fun insert(session: SessionEntity): Long

    @Query("UPDATE session SET finishedAt = :finishedAt WHERE id = :id")
    suspend fun finish(id: Long, finishedAt: Long)

    @Query("SELECT finishedAt FROM session WHERE finishedAt IS NOT NULL")
    fun observeFinishedAt(): Flow<List<Long>>

    @Query("DELETE FROM session")
    suspend fun clear()

    @Query("SELECT * FROM session ORDER BY id")
    suspend fun all(): List<SessionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(sessions: List<SessionEntity>)
}
