package app.trainer.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SkillRatingDao {
    @Query("SELECT * FROM skill_rating")
    fun observeAll(): Flow<List<SkillRatingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(rating: SkillRatingEntity)

    @Query("DELETE FROM skill_rating")
    suspend fun clear()

    @Query("SELECT * FROM skill_rating")
    suspend fun all(): List<SkillRatingEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(ratings: List<SkillRatingEntity>)
}
