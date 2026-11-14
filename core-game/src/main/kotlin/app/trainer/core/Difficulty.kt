package app.trainer.core

/**
 * Simple staircase: [upAfter] consecutive correct answers raise the level,
 * [downAfter] consecutive errors lower it. Immutable.
 */
data class Difficulty(
    val level: Int = MIN_LEVEL,
    val streak: Int = 0, // >0 consecutive correct, <0 consecutive wrong
    val upAfter: Int = 3,
    val downAfter: Int = 1,
    val minLevel: Int = MIN_LEVEL,
    val maxLevel: Int = MAX_LEVEL,
) {
    init {
        require(upAfter > 0 && downAfter > 0) { "thresholds must be positive" }
        require(minLevel <= maxLevel) { "minLevel > maxLevel" }
        require(level in minLevel..maxLevel) { "level out of range" }
    }

    fun record(correct: Boolean): Difficulty {
        val s = if (correct) (if (streak > 0) streak + 1 else 1) else (if (streak < 0) streak - 1 else -1)
        return when {
            s >= upAfter -> copy(level = (level + 1).coerceAtMost(maxLevel), streak = 0)
            -s >= downAfter -> copy(level = (level - 1).coerceAtLeast(minLevel), streak = 0)
            else -> copy(streak = s)
        }
    }

    companion object {
        const val MIN_LEVEL = 1
        const val MAX_LEVEL = 20
    }
}

/** Per-skill difficulty levels. */
data class SkillLevels(val levels: Map<Skill, Difficulty> = emptyMap()) {
    operator fun get(skill: Skill): Difficulty = levels[skill] ?: Difficulty()

    fun record(skill: Skill, correct: Boolean): SkillLevels =
        SkillLevels(levels + (skill to get(skill).record(correct)))
}
