package app.trainer.games.pairmatch

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
import app.trainer.core.PairMatchGame
import app.trainer.ui.FeedbackEffect
import app.trainer.ui.FocusBar
import app.trainer.ui.GameHeader
import app.trainer.ui.ResultScreen
import app.trainer.ui.RoundHistory

@Composable
fun PairMatchScreen(viewModel: PairMatchViewModel, primaryLabel: String, onPrimary: () -> Unit, onHome: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val summary = state.summary
    if (state.phase == PairPhase.Finished && summary != null) {
        ResultScreen(summary, primaryLabel, onPrimary, onHome)
        return
    }
    val round = state.round ?: return
    val colors = MaterialTheme.colorScheme
    val feedback = state.phase == PairPhase.Feedback
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp)) {
        FeedbackEffect(key = state.boardIndex.takeIf { feedback }, correct = state.lastCorrect.takeIf { feedback })
        FocusBar((state.boardIndex + 1f) / state.total)
        Column(Modifier.padding(top = 12.dp)) { GameHeader(stringResource(R.string.pair_board, state.boardIndex + 1, state.total), onHome) }
        Column(
            Modifier.fillMaxWidth().padding(top = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                stringResource(if (feedback) (if (state.lastCorrect == true) R.string.feedback_correct else R.string.pair_too_many_flips) else R.string.pair_find),
                style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center,
                color = if (feedback && state.lastCorrect == false) colors.error else if (feedback) colors.primary else colors.onSurface,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
            Text(
                pluralStringResource(R.plurals.pair_status, state.flips, state.flips, state.matched.size / 2, round.pairCount),
                style = MaterialTheme.typography.titleMedium, color = colors.onSurfaceVariant,
            )
            Column(Modifier.widthIn(max = 360.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                round.cards.indices.chunked(round.columns).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { index ->
                            val matched = index in state.matched
                            val up = matched || index in state.faceUp
                            val glyph = PairMatchGame.SYMBOLS[round.cards[index]]
                            val cardDescription = when {
                                matched -> stringResource(R.string.pair_card_matched, index + 1, glyph)
                                up -> stringResource(R.string.pair_card_up, index + 1, glyph)
                                else -> stringResource(R.string.pair_card, index + 1)
                            }
                            Surface(
                                onClick = { viewModel.onCardTap(index) },
                                color = if (matched) colors.secondaryContainer else if (up) colors.surface else colors.surfaceContainer,
                                shape = MaterialTheme.shapes.medium,
                                border = if (matched) BorderStroke(3.dp, colors.primary) else BorderStroke(1.dp, colors.outlineVariant),
                                modifier = Modifier.weight(1f).aspectRatio(1f).semantics {
                                    contentDescription = cardDescription
                                },
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (up) Text(glyph, style = MaterialTheme.typography.headlineMedium)
                                }
                            }
                        }
                        repeat(round.columns - row.size) { Box(Modifier.weight(1f)) }
                    }
                }
            }
            RoundHistory(state.history, state.total, Modifier.padding(top = 8.dp))
        }
    }
}
