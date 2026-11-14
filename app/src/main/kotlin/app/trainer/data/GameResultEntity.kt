package app.trainer.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import app.trainer.core.Skill

@Entity(tableName = "game_result")
data class GameResultEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long? = null,
    val gameId: String,
    val skill: Skill,
    val score: Int,
    val accuracy: Double,
    val avgReactionMs: Long,
    val difficulty: Int,
    val playedAt: Long,
)
