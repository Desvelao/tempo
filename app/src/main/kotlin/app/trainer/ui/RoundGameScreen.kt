package app.trainer.ui

import app.trainer.audio.Effect
import androidx.compose.ui.res.pluralStringResource
import kotlinx.coroutines.delay
import app.trainer.ui.theme.cappedSp
import app.trainer.ui.theme.LocalReducedMotion
import app.trainer.R as Res
import androidx.compose.ui.res.stringResource
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.trainer.games.RoundPhase
import app.trainer.games.RoundState
import app.trainer.games.nowMillis

/** Shared frame for round-based games: bar, header, countdown, feedback, round history, result screen. */
@Composable
fun <R : Any, A : Any> RoundGameScreen(
    state: RoundState<R>,
    onAnswer: (A) -> Unit,
    title: String,
    primaryLabel: String,
    onPrimary: () -> Unit,
    onHome: () -> Unit,
    content: @Composable (round: R, answer: (A) -> Unit) -> Unit,
) {
    val summary = state.summary
    when {
        state.phase == RoundPhase.Finished && summary != null -> ResultScreen(summary, primaryLabel, onPrimary, onHome)
        state.phase == RoundPhase.Countdown -> Countdown(state.countdown, pluralStringResource(Res.plurals.round_countdown_title, state.total, title, state.total), onHome)
        else -> Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            FeedbackEffect(
                key = state.index.takeIf { state.phase == RoundPhase.Feedback },
                correct = state.lastCorrect.takeIf { state.phase == RoundPhase.Feedback },
            )
            val limit = state.timeLimitMs
            when {
                limit != null && state.phase == RoundPhase.Playing -> RoundTimerBar(limit, state.startedAtMs, state.index)
                limit != null -> FocusBar(0f)
                else -> FocusBar((state.index + 1f) / state.total)
            }
            GameHeader(stringResource(Res.string.round_header, state.index + 1, state.total), onHome)
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                val round = state.round
                if (round != null) {
                    Column(
                        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).heightIn(min = maxHeight),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) { key(state.index) { content(round, onAnswer) } }
                }
            }
            RoundHistory(state.history, state.total)
            FeedbackBanner(state.phase, state.lastCorrect)
        }
    }
}

@Composable
fun Countdown(value: Int, caption: String, onClose: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val audio = rememberAudio()
    LaunchedEffect(value) { audio?.play(Effect.TICK) }
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        FocusBar(0f)
        GameHeader("", onClose)
        Column(
            Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge, color = colors.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.outlineVariant),
            ) {
                Text(caption, style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
            }
            Text(
                stringResource(Res.string.round_get_ready), style = MaterialTheme.typography.titleMedium, color = colors.onSurfaceVariant,
                modifier = Modifier.padding(top = 28.dp),
            )
            Text(
                value.toString(),
                style = MaterialTheme.typography.displayLarge.copy(fontSize = cappedSp(200), lineHeight = cappedSp(210), letterSpacing = (-8).sp),
                color = colors.primary,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 8.dp)) {
                val lit = RoundGameViewModelConstants.COUNTDOWN_FROM - value + 1
                for (i in 1..RoundGameViewModelConstants.COUNTDOWN_FROM) {
                    Surface(
                        color = if (i <= lit) colors.primary else colors.outlineVariant,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(3.dp),
                        modifier = Modifier.size(width = 40.dp, height = 6.dp),
                    ) {}
                }
            }
        }
        Text(
            stringResource(Res.string.round_adapts), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(8.dp),
        )
    }
}

private object RoundGameViewModelConstants {
    const val COUNTDOWN_FROM = app.trainer.games.RoundGameViewModel.COUNTDOWN_FROM
}

@Composable
fun RoundTimerBar(limitMs: Long, startedAtMs: Long, resetKey: Any) {
    // Start from the time actually remaining, so the bar stays correct after a rotation.
    val remaining = (limitMs - (nowMillis() - startedAtMs)).coerceIn(0L, limitMs)
    val progress = remember(resetKey) { Animatable(remaining.toFloat() / limitMs) }
    val reducedMotion = LocalReducedMotion.current
    LaunchedEffect(resetKey, reducedMotion) {
        if (reducedMotion) {
            // One step per second instead of a continuously moving bar.
            var left = (limitMs - (nowMillis() - startedAtMs)).coerceIn(0L, limitMs)
            while (left > 0) {
                progress.snapTo(left.toFloat() / limitMs)
                val step = minOf(1000L, left)
                delay(step)
                left -= step
            }
            progress.snapTo(0f)
        } else {
            progress.animateTo(0f, tween(remaining.toInt(), easing = LinearEasing))
        }
    }
    FocusBar(progress.value)
}

@Composable
fun FeedbackBanner(phase: RoundPhase, correct: Boolean?) {
    Text(
        if (phase == RoundPhase.Feedback) stringResource(if (correct == true) Res.string.feedback_correct else Res.string.feedback_wrong) else " ",
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.fillMaxWidth().padding(8.dp).semantics { liveRegion = LiveRegionMode.Polite },
        color = if (correct == false) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
    )
}

/** Answer buttons laid out two per row. */
@Composable
fun <T> OptionGrid(
    options: List<T>,
    label: (T) -> String,
    onPick: (T) -> Unit,
    buttonHeight: Dp = 76.dp,
    textStyle: TextStyle = MaterialTheme.typography.titleLarge,
    icon: (@Composable (T) -> Unit)? = null,
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        options.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { option ->
                    ChoiceButton(
                        label(option), { onPick(option) }, Modifier.weight(1f), buttonHeight, textStyle,
                        icon = icon?.let { f -> { f(option) } },
                    )
                }
                if (row.size == 1) Box(Modifier.weight(1f))
            }
        }
    }
}
