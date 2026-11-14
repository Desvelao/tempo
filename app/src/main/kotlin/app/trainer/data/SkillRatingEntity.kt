package app.trainer.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import app.trainer.core.Skill

@Entity(tableName = "skill_rating")
data class SkillRatingEntity(
    @PrimaryKey val skill: Skill,
    val level: Int,
    val updatedAt: Long,
)
