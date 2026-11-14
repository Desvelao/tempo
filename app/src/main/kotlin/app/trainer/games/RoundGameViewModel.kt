package app.trainer.games

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.trainer.core.Difficulty
import app.trainer.core.Game
import app.trainer.core.RoundResult
import app.trainer.core.Scoring
import app.trainer.data.ResultRepository
import app.trainer.ui.ResultSummary
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.random.Random

/** Monotonic clock shared with the UI so a recreated screen can resume the timer bar. */
fun nowMillis(): Long = System.nanoTime() / 1_000_000

enum class RoundPhase { Countdown, Playing, Feedback, Finished }

data class RoundState<R>(
    val phase: RoundPhase = RoundPhase.Countdown,
    val countdown: Int = RoundGameViewModel.COUNTDOWN_FROM,
    val round: R? = null,
    val index: Int = 0,
    val total: Int = 0,
    val lastCorrect: Boolean? = null,
    val timeLimitMs: Long? = null,
    val startedAtMs: Long = 0,
    val history: List<Boolean> = emptyList(),
    val summary: ResultSummary? = null,
)

/** Shared engine for games made of independent answer rounds (everything except grid recall). */
class RoundGameViewModel<R : Any, A : Any>(
    private val game: Game<R, A>,
    private val repository: ResultRepository,
    private val rounds: Int = DEFAULT_ROUNDS,
    private val startLevel: Int = Difficulty.MIN_LEVEL,
    private val random: Random = Random.Default,
    private val sessionId: Long? = null,
) : ViewModel() {
    private val _state = MutableStateFlow(RoundState<R>(total = rounds))
    val state: StateFlow<RoundState<R>> = _state

    private var pending: CompletableDeferred<A>? = null

    init {
        viewModelScope.launch { run() }
    }

    fun onAnswer(answer: A) {
        if (_state.value.phase == RoundPhase.Playing) pending?.complete(answer)
    }

    private fun nowMs() = nowMillis()

    private suspend fun run() {
        game.reset()
        for (c in COUNTDOWN_FROM downTo 1) {
            _state.update { it.copy(phase = RoundPhase.Countdown, countdown = c) }
            delay(COUNTDOWN_STEP_MS)
        }
        var difficulty = Difficulty(level = startLevel)
        val results = mutableListOf<RoundResult>()
        for (i in 0 until rounds) {
            val round = game.generateRound(difficulty.level, random)
            val limit = game.timeLimitMs(difficulty.level)
            val deferred = CompletableDeferred<A>().also { pending = it }
            val startedAt = nowMs()
            _state.update {
                it.copy(
                    phase = RoundPhase.Playing, round = round, index = i, lastCorrect = null,
                    timeLimitMs = limit, startedAtMs = startedAt,
                )
            }
            val answer: A? = if (limit == null) deferred.await()
            else withTimeoutOrNull(limit) { deferred.await() } ?: game.timeoutAnswer(round)
            val reaction = limit?.let { (nowMs() - startedAt).coerceAtMost(it) } ?: (nowMs() - startedAt)
            val correct = answer != null && game.evaluate(round, answer)
            results += RoundResult(correct, reaction)
            difficulty = difficulty.record(correct)
            _state.update { it.copy(phase = RoundPhase.Feedback, lastCorrect = correct, history = results.map { r -> r.correct }) }
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
        _state.update { it.copy(phase = RoundPhase.Finished, summary = summary) }
    }

    companion object {
        const val DEFAULT_ROUNDS = 10
        const val COUNTDOWN_FROM = 3
        const val COUNTDOWN_STEP_MS = 700L
        const val FEEDBACK_MS = 500L
    }
}
