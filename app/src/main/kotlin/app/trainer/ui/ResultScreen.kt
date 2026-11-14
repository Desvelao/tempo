package app.trainer.ui

import app.trainer.audio.Effect
import androidx.compose.foundation.layout.heightIn
import app.trainer.ui.theme.cappedSp
import app.trainer.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import kotlin.math.abs

data class ResultSummary(
    val score: Int,
    val accuracy: Double,
    val avgReactionMs: Long,
    val correct: Int,
    val total: Int,
    val difficulty: Int,
    val finalLevel: Int = difficulty,
    val gameId: String = "",
    val history: List<Boolean> = emptyList(),
    /** Score minus the previous score of the same game; null on the first play. */
    val scoreDelta: Int? = null,
)

@Composable
fun ResultScreen(summary: ResultSummary, primaryLabel: String, onPrimary: () -> Unit, onHome: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val format = NumberFormat.getIntegerInstance(LocalConfiguration.current.locales[0])
    val audio = rememberAudio()
    androidx.compose.runtime.LaunchedEffect(Unit) { audio?.play(Effect.DONE) }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            if (summary.gameId.isBlank()) stringResource(R.string.result_title) else stringResource(R.string.result_title_game, gameTitle(summary.gameId).uppercase()),
            style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            format.format(summary.score),
            style = MaterialTheme.typography.displayLarge.copy(fontSize = cappedSp(88), lineHeight = cappedSp(92), letterSpacing = (-3).sp),
            color = colors.primary,
            modifier = Modifier.padding(top = 16.dp),
        )
        Text(stringResource(R.string.result_points), style = MaterialTheme.typography.titleMedium, color = colors.onSurfaceVariant)

        summary.scoreDelta?.let { delta ->
            Surface(
                shape = MaterialTheme.shapes.extraLarge, color = colors.surface,
                border = BorderStroke(1.dp, colors.outlineVariant),
                modifier = Modifier.padding(top = 14.dp),
            ) {
                val text = when {
                    delta > 0 -> stringResource(R.string.result_delta_up, format.format(delta))
                    delta < 0 -> stringResource(R.string.result_delta_down, format.format(abs(delta)))
                    else -> stringResource(R.string.result_delta_same)
                }
                Text(
                    text, style = MaterialTheme.typography.bodyMedium,
                    color = if (delta > 0) colors.primary else if (delta < 0) colors.error else colors.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                )
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth().padding(top = 28.dp),
            shape = MaterialTheme.shapes.large,
            color = colors.surfaceContainer,
            border = BorderStroke(1.dp, colors.outlineVariant),
        ) {
            Column(Modifier.padding(horizontal = 20.dp)) {
                StatRow(stringResource(R.string.result_correct), stringResource(R.string.result_fraction, summary.correct, summary.total))
                HorizontalDivider(color = colors.outlineVariant)
                StatRow(stringResource(R.string.result_accuracy), stringResource(R.string.result_percent, (summary.accuracy * 100).toInt()))
                HorizontalDivider(color = colors.outlineVariant)
                StatRow(stringResource(R.string.result_avg_reaction), stringResource(R.string.result_ms, format.format(summary.avgReactionMs)))
                HorizontalDivider(color = colors.outlineVariant)
                StatRow(stringResource(R.string.result_level), "${summary.difficulty}", trailing = if (summary.finalLevel != summary.difficulty) "→ ${summary.finalLevel}" else null)
            }
        }

        if (summary.history.isNotEmpty()) RoundHistory(summary.history, summary.total, Modifier.padding(top = 22.dp))

        Column(Modifier.padding(top = 28.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PrimaryButton(primaryLabel, onPrimary)
            SecondaryButton(stringResource(R.string.action_home), onHome)
        }
    }
}

@Composable
private fun StatRow(label: String, value: String, trailing: String? = null) {
    Row(Modifier.fillMaxWidth().heightIn(min = 52.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(value, style = MaterialTheme.typography.titleMedium)
            if (trailing != null) Text(trailing, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        }
    }
}
