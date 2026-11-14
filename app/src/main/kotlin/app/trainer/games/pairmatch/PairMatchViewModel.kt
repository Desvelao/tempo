package app.trainer.games.pairmatch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.trainer.core.Difficulty
import app.trainer.core.PairMatchGame
import app.trainer.core.PairMatchRound
import app.trainer.core.RoundResult
import app.trainer.core.Scoring
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

enum class PairPhase { Playing, Feedback, Finished }

data class PairMatchState(
    val phase: PairPhase = PairPhase.Playing,
    val round: PairMatchRound? = null,
    val faceUp: Set<Int> = emptySet(),
    val matched: Set<Int> = emptySet(),
    val flips: Int = 0,
    val boardIndex: Int = 0,
    val total: Int = PairMatchViewModel.BOARDS,
    val lastCorrect: Boolean? = null,
    val history: List<Boolean> = emptyList(),
    val summary: ResultSummary? = null,
)

class PairMatchViewModel(
    private val repository: ResultRepository,
    private val startLevel: Int = Difficulty.MIN_LEVEL,
    private val random: Random = Random.Default,
    private val sessionId: Long? = null,
) : ViewModel() {
    private val game = PairMatchGame()
    private val _state = MutableStateFlow(PairMatchState())
    val state: StateFlow<PairMatchState> = _state

    private var pending: CompletableDeferred<Int>? = null
    private var locked = false

    init {
        viewModelScope.launch { run() }
    }

    fun onCardTap(index: Int) {
        val s = _state.value
        val round = s.round ?: return
        if (s.phase != PairPhase.Playing || locked || index in s.matched || index in s.faceUp) return
        val faceUp = s.faceUp + index
        val flips = s.flips + 1
        _state.update { it.copy(faceUp = faceUp, flips = flips) }
        if (faceUp.size < 2) return
        locked = true
        viewModelScope.launch {
            val (a, b) = faceUp.toList()
            val match = game.isMatch(round, a, b)
            delay(if (match) MATCH_MS else MISMATCH_MS)
            val matched = if (match) _state.value.matched + faceUp else _state.value.matched
            _state.update { it.copy(faceUp = emptySet(), matched = matched) }
            locked = false
            if (matched.size == round.cards.size) pending?.complete(_state.value.flips)
        }
    }

    private suspend fun run() {
        var difficulty = Difficulty(level = startLevel)
        val results = mutableListOf<RoundResult>()
        for (i in 0 until BOARDS) {
            val round = game.generateRound(difficulty.level, random)
            val deferred = CompletableDeferred<Int>().also { pending = it }
            _state.update {
                it.copy(
                    phase = PairPhase.Playing, round = round, faceUp = emptySet(), matched = emptySet(),
                    flips = 0, boardIndex = i, lastCorrect = null,
                )
            }
            val startedAt = nowMillis()
            val flips = deferred.await()
            val correct = game.evaluate(round, flips)
            results += RoundResult(correct, nowMillis() - startedAt)
            difficulty = difficulty.record(correct)
            _state.update { it.copy(phase = PairPhase.Feedback, lastCorrect = correct, history = results.map { r -> r.correct }) }
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
        _state.update { it.copy(phase = PairPhase.Finished, summary = summary) }
    }

    companion object {
        const val BOARDS = 3
        const val MATCH_MS = 350L
        const val MISMATCH_MS = 750L
        const val FEEDBACK_MS = 900L
    }
}
