package app.trainer.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "game_list")
data class GameListEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** Game ids joined by commas, in play order. */
    val gameIds: String,
    val shuffle: Boolean,
    val createdAt: Long,
)
