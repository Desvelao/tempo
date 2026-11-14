package app.trainer.games.gridrecall

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

@Composable
fun GridRecallScreen(
    viewModel: GridRecallViewModel,
    primaryLabel: String,
    onPrimary: () -> Unit,
    onHome: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val summary = state.summary
    if (state.phase == Phase.Finished && summary != null) {
        ResultScreen(summary, primaryLabel, onPrimary, onHome)
        return
    }
    val round = state.round ?: return
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp)) {
        FeedbackEffect(
            key = state.roundIndex.takeIf { state.phase == Phase.Feedback },
            correct = state.lastCorrect.takeIf { state.phase == Phase.Feedback },
        )
        FocusBar((state.roundIndex + 1f) / state.total)
        Column(Modifier.padding(top = 12.dp)) { GameHeader(stringResource(R.string.round_header, state.roundIndex + 1, state.total), onHome) }
        Column(
            Modifier.fillMaxWidth().padding(top = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                when (state.phase) {
                    Phase.Showing -> stringResource(R.string.grid_remember)
                    Phase.Input -> stringResource(R.string.grid_tap_back)
                    Phase.Feedback -> stringResource(if (state.lastCorrect == true) R.string.feedback_correct else R.string.feedback_wrong)
                    Phase.Finished -> ""
                },
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                color = when {
                    state.phase == Phase.Feedback && state.lastCorrect == false -> MaterialTheme.colorScheme.error
                    state.phase == Phase.Feedback -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurface
                },
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                Text(
                    if (state.phase == Phase.Showing) "${round.tiles.size}" else "${state.selected.size}",
                    style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    if (state.phase == Phase.Showing) pluralStringResource(R.plurals.grid_tiles_to_remember, round.tiles.size) else pluralStringResource(R.plurals.grid_selected_of, round.tiles.size, round.tiles.size),
                    style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
            }
            val gap = if (round.gridSize >= 5) 4.dp else 10.dp
            Column(Modifier.widthIn(max = 360.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(gap)) {
                for (row in 0 until round.gridSize) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                        for (col in 0 until round.gridSize) {
                            val index = row * round.gridSize + col
                            val lit = when (state.phase) {
                                Phase.Showing -> index in round.tiles
                                Phase.Input -> index in state.selected
                                else -> index in round.tiles
                            }
                            val wrong = state.phase == Phase.Feedback && index in state.selected && index !in round.tiles
                            Tile(
                                lit = lit, wrong = wrong,
                                description = stringResource(
                                    when {
                                        wrong -> R.string.grid_tile_wrong
                                        state.phase == Phase.Feedback && lit -> R.string.grid_tile_correct
                                        lit -> R.string.grid_tile_selected
                                        else -> R.string.grid_tile
                                    },
                                    index + 1,
                                ),
                                modifier = Modifier.weight(1f),
                                onClick = { viewModel.onTileTap(index) },
                            )
                        }
                    }
                }
            }
            app.trainer.ui.RoundHistory(state.history, state.total, Modifier.padding(top = 8.dp))
            Text(
                stringResource(R.string.grid_undo_hint),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Tile(lit: Boolean, wrong: Boolean, description: String, modifier: Modifier, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        color = when {
            wrong -> colors.error
            lit -> colors.primary
            else -> colors.surfaceContainer
        },
        shape = MaterialTheme.shapes.medium,
        border = if (lit || wrong) BorderStroke(3.dp, colors.onSurface) else BorderStroke(1.dp, colors.outlineVariant),
        modifier = modifier.aspectRatio(1f).semantics { contentDescription = description },
    ) {
        // A mark in addition to the colour, so a wrong tile is not signalled by colour alone.
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (wrong) Text("✕", style = MaterialTheme.typography.titleLarge, color = colors.onError)
        }
    }
}
