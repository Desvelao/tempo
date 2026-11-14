package app.trainer.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GameListDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(list: GameListEntity): Long

    @Query("SELECT * FROM game_list ORDER BY createdAt")
    fun observeAll(): Flow<List<GameListEntity>>

    @Query("SELECT * FROM game_list ORDER BY createdAt")
    suspend fun all(): List<GameListEntity>

    @Query("SELECT * FROM game_list WHERE id = :id")
    suspend fun get(id: Long): GameListEntity?

    @Query("DELETE FROM game_list WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM game_list")
    suspend fun clear()

    @Insert
    suspend fun insertAll(lists: List<GameListEntity>)
}
