package app.trainer.games.trailalternate

import app.trainer.R
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.trainer.ui.FeedbackEffect
import app.trainer.ui.FocusBar
import app.trainer.ui.GameHeader
import app.trainer.ui.ResultScreen
import app.trainer.ui.RoundHistory

@Composable
fun TrailAlternateScreen(viewModel: TrailAlternateViewModel, primaryLabel: String, onPrimary: () -> Unit, onHome: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val summary = state.summary
    if (state.phase == TrailPhase.Finished && summary != null) {
        ResultScreen(summary, primaryLabel, onPrimary, onHome)
        return
    }
    val round = state.round ?: return
    val colors = MaterialTheme.colorScheme
    val feedback = state.phase == TrailPhase.Feedback
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp)) {
        FeedbackEffect(key = state.trailIndex.takeIf { feedback }, correct = state.lastCorrect.takeIf { feedback })
        FocusBar((state.trailIndex + 1f) / state.total)
        Column(Modifier.padding(top = 12.dp)) { GameHeader(stringResource(R.string.trail_header, state.trailIndex + 1, state.total), onHome) }
        Column(
            Modifier.fillMaxWidth().padding(top = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                stringResource(if (feedback) (if (state.lastCorrect == true) R.string.feedback_correct else R.string.trail_too_many) else R.string.trail_instruction),
                style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center,
                color = if (feedback && state.lastCorrect == false) colors.error else if (feedback) colors.primary else colors.onSurface,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
            val nextLabel = round.labels.getOrNull(state.next)
            Text(
                if (nextLabel != null) pluralStringResource(R.plurals.trail_status_next, state.errors, nextLabel, state.errors) else pluralStringResource(R.plurals.trail_status, state.errors, state.errors),
                style = MaterialTheme.typography.titleMedium, color = colors.onSurfaceVariant,
            )
            Column(Modifier.widthIn(max = 360.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                (0 until round.cells).chunked(round.columns).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { cell ->
                            val step = round.cellOfStep.indexOf(cell)
                            if (step < 0) {
                                Box(Modifier.weight(1f).aspectRatio(1f))
                            } else {
                                val done = step < state.next
                                val wrong = state.wrongCell == cell
                                val label = round.labels[step]
                                val cellDescription = when {
                                    done -> stringResource(R.string.trail_cell_done, label)
                                    wrong -> stringResource(R.string.trail_cell_wrong, label)
                                    else -> label
                                }
                                Surface(
                                    onClick = { viewModel.onCellTap(cell) },
                                    color = when {
                                        wrong -> colors.error
                                        done -> colors.secondaryContainer
                                        else -> colors.surfaceContainer
                                    },
                                    shape = MaterialTheme.shapes.medium,
                                    border = BorderStroke(if (done || wrong) 3.dp else 1.dp, if (wrong) colors.onSurface else if (done) colors.primary else colors.outlineVariant),
                                    modifier = Modifier.weight(1f).aspectRatio(1f).semantics {
                                        contentDescription = cellDescription
                                    },
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        // A mark in addition to the colour for the wrong tap.
                                        Text(
                                            if (wrong) "✕ $label" else label,
                                            style = MaterialTheme.typography.headlineMedium,
                                            color = if (wrong) colors.onError else if (done) colors.onSurfaceVariant else colors.onSurface,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            RoundHistory(state.history, state.total, Modifier.padding(top = 8.dp))
        }
    }
}
