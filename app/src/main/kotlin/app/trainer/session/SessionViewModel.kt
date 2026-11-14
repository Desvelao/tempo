package app.trainer.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.trainer.core.GameList
import app.trainer.core.ListPlanner
import app.trainer.core.SessionPlanner
import app.trainer.core.Skill
import app.trainer.data.ResultRepository
import app.trainer.games.GameCatalog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

data class SessionGame(val gameId: String, val score: Int, val startLevel: Int, val endLevel: Int)

data class SessionState(
    val sessionId: Long? = null,
    val plan: List<String> = emptyList(),
    val index: Int = 0,
    val complete: Boolean = false,
    val streak: Int = 0,
    val games: List<SessionGame> = emptyList(),
    val days: Set<LocalDate> = emptySet(),
) {
    val isLast get() = index == plan.lastIndex
}

/**
 * A session played back to back. By default it is 3-5 random games across different skills; [resolveList] can
 * return a saved list to play instead (null falls back to random games).
 */
class SessionViewModel(
    private val repository: ResultRepository,
    random: Random = Random.Default,
    count: Int = 4,
    private val resolveList: suspend (Random) -> GameList? = { null },
    /** Chooses the skills when no list is played; the default is [SessionPlanner.plan] with [count] games. */
    private val planSkills: suspend (Random) -> List<Skill> = { SessionPlanner.plan(it, count) },
) : ViewModel() {
    private val _state = MutableStateFlow(SessionState())
    val state: StateFlow<SessionState> = _state

    init {
        viewModelScope.launch {
            val list = resolveList(random)
            val plan = list?.let { ListPlanner.plan(it.gameIds, it.shuffle, random, GameCatalog.all.map { g -> g.id }.toSet()) }?.takeIf { it.isNotEmpty() }
                ?: planSkills(random).map { GameCatalog.forSkill(it, random).id }
            val id = repository.startSession()
            _state.update { it.copy(sessionId = id, plan = plan) }
        }
    }

    /** Called when the current game's result screen is dismissed with "Next game"/"Finish". */
    fun next() {
        val s = _state.value
        val id = s.sessionId ?: return
        if (s.complete || s.plan.isEmpty()) return
        if (!s.isLast) {
            _state.update { it.copy(index = it.index + 1) }
        } else {
            _state.update { it.copy(complete = true) } // guards double taps
            viewModelScope.launch {
                repository.finishSession(id)
                val levels = repository.levels.first()
                val games = repository.sessionResults(id).map {
                    SessionGame(it.gameId, it.score, it.difficulty, levels[it.skill] ?: it.difficulty)
                }
                _state.update {
                    it.copy(streak = repository.currentStreak(), games = games, days = repository.sessionDays.first())
                }
            }
        }
    }
}
