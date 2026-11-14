package app.trainer.games.trailalternate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.trainer.core.Difficulty
import app.trainer.core.RoundResult
import app.trainer.core.Scoring
import app.trainer.core.TrailAlternateGame
import app.trainer.core.TrailRound
import app.trainer.data.ResultRepository
import app.trainer.games.GameCatalog
import app.trainer.games.nowMillis
import app.trainer.ui.ResultSummary
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

enum class TrailPhase { Playing, Feedback, Finished }

data class TrailState(
    val phase: TrailPhase = TrailPhase.Playing,
    val round: TrailRound? = null,
    /** Index of the next step to tap; steps below it are done. */
    val next: Int = 0,
    val errors: Int = 0,
    val wrongCell: Int? = null,
    val trailIndex: Int = 0,
    val total: Int = TrailAlternateViewModel.TRAILS,
    val lastCorrect: Boolean? = null,
    val history: List<Boolean> = emptyList(),
    val summary: ResultSummary? = null,
)

class TrailAlternateViewModel(
    private val repository: ResultRepository,
    private val startLevel: Int = Difficulty.MIN_LEVEL,
    private val random: Random = Random.Default,
    private val sessionId: Long? = null,
) : ViewModel() {
    private val game = TrailAlternateGame()
    private val _state = MutableStateFlow(TrailState())
    val state: StateFlow<TrailState> = _state

    private var pending: CompletableDeferred<Int>? = null

    init {
        viewModelScope.launch { run() }
    }

    fun onCellTap(cell: Int) {
        val s = _state.value
        val round = s.round ?: return
        if (s.phase != TrailPhase.Playing) return
        val step = round.cellOfStep.indexOf(cell)
        if (step < 0 || step < s.next) return
        if (step == s.next) {
            _state.update { it.copy(next = step + 1, wrongCell = null) }
            if (step + 1 == round.labels.size) pending?.complete(_state.value.errors)
        } else {
            _state.update { it.copy(errors = it.errors + 1, wrongCell = cell) }
            viewModelScope.launch {
                delay(WRONG_FLASH_MS)
                _state.update { if (it.wrongCell == cell) it.copy(wrongCell = null) else it }
            }
        }
    }

    private suspend fun run() {
        var difficulty = Difficulty(level = startLevel)
        val results = mutableListOf<RoundResult>()
        for (i in 0 until TRAILS) {
            val round = game.generateRound(difficulty.level, random)
            val deferred = CompletableDeferred<Int>().also { pending = it }
            _state.update {
                it.copy(
                    phase = TrailPhase.Playing, round = round, next = 0, errors = 0, wrongCell = null,
                    trailIndex = i, lastCorrect = null,
                )
            }
            val startedAt = nowMillis()
            val errors = deferred.await()
            val correct = game.evaluate(round, errors)
            results += RoundResult(correct, nowMillis() - startedAt)
            difficulty = difficulty.record(correct)
            _state.update { it.copy(phase = TrailPhase.Feedback, lastCorrect = correct, history = results.map { r -> r.correct }) }
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
        _state.update { it.copy(phase = TrailPhase.Finished, summary = summary) }
    }

    companion object {
        const val TRAILS = 3
        const val WRONG_FLASH_MS = 400L
        const val FEEDBACK_MS = 900L
    }
}
