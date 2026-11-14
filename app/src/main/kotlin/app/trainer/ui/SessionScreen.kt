package app.trainer.ui

import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.heightIn
import app.trainer.ui.theme.cappedSp
import app.trainer.R
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.trainer.session.SessionGame
import app.trainer.session.SessionViewModel
import java.text.NumberFormat

@Composable
fun SessionScreen(viewModel: SessionViewModel, onHome: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    when {
        state.complete -> SessionComplete(state.streak, state.days, state.games, onHome)
        state.plan.isEmpty() -> Unit
        else -> GameHost(
            gameId = state.plan[state.index],
            key = "session-${state.sessionId}-${state.index}",
            sessionId = state.sessionId,
            primaryLabel = if (state.isLast) stringResource(R.string.session_finish) else stringResource(R.string.session_next_game, state.index + 2, state.plan.size),
            onPrimary = viewModel::next,
            onHome = onHome,
        )
    }
}

@Composable
private fun SessionComplete(streak: Int, days: Set<java.time.LocalDate>, games: List<SessionGame>, onHome: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val format = NumberFormat.getIntegerInstance(LocalConfiguration.current.locales[0])
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.session_complete), style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant, modifier = Modifier.semantics { heading() })
        Text(
            pluralStringResource(R.plurals.days_count, streak, streak),
            style = MaterialTheme.typography.displayLarge.copy(fontSize = cappedSp(80), lineHeight = cappedSp(84), letterSpacing = (-3).sp),
            color = colors.primary,
            modifier = Modifier.padding(top = 16.dp),
        )
        Text(stringResource(R.string.session_current_streak), style = MaterialTheme.typography.titleMedium, color = colors.onSurfaceVariant)

        Surface(
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
            shape = MaterialTheme.shapes.large, color = colors.surfaceContainer,
            border = BorderStroke(1.dp, colors.outlineVariant),
        ) { WeekStrip(days, Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) }

        if (games.isNotEmpty()) {
            Text(
                stringResource(R.string.session_todays_games), style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(top = 28.dp, bottom = 4.dp),
            )
            Column(Modifier.fillMaxWidth()) {
                games.forEachIndexed { i, g ->
                    Row(Modifier.fillMaxWidth().heightIn(min = 54.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(gameTitle(g.gameId), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                        Text(format.format(g.score), Modifier.widthIn(min = 76.dp), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.End)
                        val (arrow, tint) = when {
                            g.endLevel > g.startLevel -> " ↑" to colors.primary
                            g.endLevel < g.startLevel -> " ↓" to colors.error
                            else -> "" to colors.onSurfaceVariant
                        }
                        Text(
                            stringResource(R.string.session_level_short, g.endLevel) + arrow, Modifier.widthIn(min = 72.dp), textAlign = TextAlign.End,
                            style = MaterialTheme.typography.labelLarge, color = tint,
                        )
                    }
                    if (i < games.lastIndex) HorizontalDivider(color = colors.outlineVariant)
                }
            }
        }

        PrimaryButton(stringResource(R.string.action_home), onHome, Modifier.padding(top = 32.dp))
    }
}
