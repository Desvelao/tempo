package app.trainer.games.gridrecall

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.trainer.core.Difficulty
import app.trainer.core.GridRecallGame
import app.trainer.core.GridRecallRound
import app.trainer.core.RoundResult
import app.trainer.core.Scoring
import app.trainer.data.ResultRepository
import app.trainer.games.GameCatalog
import app.trainer.ui.ResultSummary
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

enum class Phase { Showing, Input, Feedback, Finished }

data class GridRecallState(
    val phase: Phase = Phase.Showing,
    val round: GridRecallRound? = null,
    val selected: Set<Int> = emptySet(),
    val roundIndex: Int = 0,
    val total: Int = GridRecallViewModel.ROUNDS,
    val lastCorrect: Boolean? = null,
    val summary: ResultSummary? = null,
    val history: List<Boolean> = emptyList(),
)

class GridRecallViewModel(
    private val repository: ResultRepository,
    startLevel: Int = Difficulty.MIN_LEVEL,
    private val random: Random = Random.Default,
    private val sessionId: Long? = null,
) : ViewModel() {
    private val game = GridRecallGame()
    private val startLevel = startLevel
    private val _state = MutableStateFlow(GridRecallState())
    val state: StateFlow<GridRecallState> = _state

    private var pending: CompletableDeferred<Set<Int>>? = null

    init {
        viewModelScope.launch { run() }
    }

    fun onTileTap(index: Int) {
        val s = _state.value
        val round = s.round ?: return
        if (s.phase != Phase.Input) return
        val selected = if (index in s.selected) s.selected - index else s.selected + index
        _state.update { it.copy(selected = selected) }
        if (selected.size == round.tiles.size) pending?.complete(selected)
    }

    private suspend fun run() {
        var difficulty = Difficulty(level = startLevel)
        val results = mutableListOf<RoundResult>()
        for (i in 0 until ROUNDS) {
            val round = game.generateRound(difficulty.level, random)
            _state.update {
                it.copy(phase = Phase.Showing, round = round, selected = emptySet(), roundIndex = i, lastCorrect = null)
            }
            delay(FLASH_MS + 150L * round.tiles.size)
            val deferred = CompletableDeferred<Set<Int>>().also { pending = it }
            _state.update { it.copy(phase = Phase.Input) }
            val startedAt = SystemClock.elapsedRealtime()
            val answer = deferred.await()
            val reaction = SystemClock.elapsedRealtime() - startedAt
            val correct = game.evaluate(round, answer)
            results += RoundResult(correct, reaction)
            difficulty = difficulty.record(correct)
            _state.update { it.copy(phase = Phase.Feedback, lastCorrect = correct, history = results.map { r -> r.correct }) }
            delay(FEEDBACK_MS)
        }
        val base = ResultSummary(
            score = game.score(results),
            accuracy = Scoring.accuracy(results),
            avgReactionMs = Scoring.avgReactionMs(results),
            correct = results.count { it.correct },
            total = results.size,
            difficulty = startLevel,
            finalLevel = difficulty.level,
            gameId = game.id,
            history = results.map { it.correct },
        )
        val summary = base.copy(scoreDelta = repository.lastScore(game.id)?.let { base.score - it })
        repository.saveGame(game, summary, startLevel, sessionId)
        _state.update { it.copy(phase = Phase.Finished, summary = summary) }
    }

    companion object {
        const val ROUNDS = 8
        const val FLASH_MS = 800L
        const val FEEDBACK_MS = 900L
    }
}
