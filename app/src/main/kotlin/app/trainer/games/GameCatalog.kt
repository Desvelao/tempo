package app.trainer.games

import app.trainer.R
import androidx.annotation.StringRes
import app.trainer.core.Skill
import kotlin.random.Random

data class GameInfo(val id: String, @StringRes val titleRes: Int, val skill: Skill)

/** All playable games, in the order shown on the home screen. */
object GameCatalog {
    const val GRID_RECALL = "grid_recall"
    const val STROOP = "stroop"
    const val QUICK_MATH = "quick_math"
    const val N_BACK = "n_back"
    const val RULE_SWITCH = "rule_switch"
    const val ODD_ONE_OUT = "odd_one_out"
    const val REVERSE_SEQUENCE = "reverse_sequence"
    const val REACTION_TAP = "reaction_tap"
    const val PAIR_MATCH = "pair_match"
    const val TRAIL_ALTERNATE = "trail_alternate"
    const val MENTAL_ROTATION = "mental_rotation"
    const val VISUAL_SEARCH = "visual_search"
    const val DIGIT_SPAN = "digit_span"

    val all = listOf(
        GameInfo(GRID_RECALL, R.string.game_grid_recall, Skill.MEMORY),
        GameInfo(STROOP, R.string.game_stroop, Skill.ATTENTION),
        GameInfo(QUICK_MATH, R.string.game_quick_math, Skill.SPEED),
        GameInfo(N_BACK, R.string.game_n_back, Skill.WORKING_MEMORY),
        GameInfo(RULE_SWITCH, R.string.game_rule_switch, Skill.FLEXIBILITY),
        GameInfo(ODD_ONE_OUT, R.string.game_odd_one_out, Skill.ATTENTION),
        GameInfo(REVERSE_SEQUENCE, R.string.game_reverse_sequence, Skill.WORKING_MEMORY),
        GameInfo(REACTION_TAP, R.string.game_reaction_tap, Skill.SPEED),
        GameInfo(PAIR_MATCH, R.string.game_pair_match, Skill.MEMORY),
        GameInfo(TRAIL_ALTERNATE, R.string.game_trail_alternate, Skill.FLEXIBILITY),
        GameInfo(MENTAL_ROTATION, R.string.game_mental_rotation, Skill.ATTENTION),
        GameInfo(VISUAL_SEARCH, R.string.game_visual_search, Skill.SPEED),
        GameInfo(DIGIT_SPAN, R.string.game_digit_span, Skill.WORKING_MEMORY),
    )

    /** A random game for [skill]; ratings are per skill, so every game of a skill shares one level. */
    fun forSkill(skill: Skill, random: Random = Random.Default): GameInfo =
        all.filter { it.skill == skill }.random(random)

    fun skillOf(id: String): Skill = all.first { it.id == id }.skill

    @StringRes
    fun titleRes(id: String): Int? = all.firstOrNull { it.id == id }?.titleRes
}
