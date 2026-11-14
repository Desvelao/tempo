package app.trainer.ui

import androidx.compose.ui.res.pluralStringResource
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.trainer.R
import app.trainer.core.ChartMath
import app.trainer.core.Skill
import app.trainer.data.GameResultEntity
import app.trainer.data.ResultRepository
import app.trainer.games.GameCatalog
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Date

private const val CHART_POINTS = 30

@Composable
fun Skill.label(): String = stringResource(
    when (this) {
        Skill.MEMORY -> R.string.skill_memory
        Skill.ATTENTION -> R.string.skill_attention
        Skill.SPEED -> R.string.skill_speed
        Skill.WORKING_MEMORY -> R.string.skill_working_memory
        Skill.FLEXIBILITY -> R.string.skill_flexibility
    },
)

@Composable
fun ProgressScreen(repository: ResultRepository, onInsights: () -> Unit) {
    val results by repository.all.collectAsStateWithLifecycle(initialValue = emptyList())
    var showAccuracy by rememberSaveable { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme

    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Text(stringResource(R.string.progress_title), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() }) }
        item { SecondaryButton(stringResource(R.string.progress_insights), onInsights) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for ((label, accuracy) in listOf(stringResource(R.string.progress_score) to false, stringResource(R.string.progress_accuracy) to true)) {
                    FilterChip(
                        selected = showAccuracy == accuracy,
                        onClick = { showAccuracy = accuracy },
                        label = { Text(label, style = MaterialTheme.typography.labelLarge) },
                        shape = RoundedCornerShape(12.dp),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true, selected = showAccuracy == accuracy,
                            borderColor = colors.outlineVariant, selectedBorderColor = colors.primary,
                        ),
                    )
                }
            }
        }
        items(Skill.entries.toList()) { skill ->
            SkillTrend(skill, results.filter { it.skill == skill }, showAccuracy)
        }
        item { Text(stringResource(R.string.progress_history), style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant, modifier = Modifier.padding(top = 10.dp)) }
        if (results.isEmpty()) item { Text(stringResource(R.string.progress_no_results), color = colors.onSurfaceVariant) }
        items(results, key = { it.id }) { ResultRow(it) }
    }
}

@Composable
private fun SkillTrend(skill: Skill, skillResults: List<GameResultEntity>, showAccuracy: Boolean) {
    val colors = MaterialTheme.colorScheme
    val format = NumberFormat.getIntegerInstance(LocalConfiguration.current.locales[0])
    val recent = skillResults.sortedBy { it.playedAt }.takeLast(CHART_POINTS)
    val values = recent.map { if (showAccuracy) it.accuracy * 100 else it.score.toDouble() }
    val max = if (showAccuracy) 100.0 else ChartMath.axisMax(values)
    val latest = values.lastOrNull()?.let { if (showAccuracy) "${it.toInt()}%" else format.format(it.toLong()) }
    val skillName = skill.label()
    val description = if (latest == null) {
        stringResource(if (showAccuracy) R.string.progress_trend_empty_accuracy else R.string.progress_trend_empty_score, skillName)
    } else {
        pluralStringResource(if (showAccuracy) R.plurals.progress_trend_accuracy else R.plurals.progress_trend_score, values.size, skillName, values.size, latest)
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = colors.surfaceContainer,
        border = BorderStroke(1.dp, colors.outlineVariant),
    ) {
        androidx.compose.foundation.layout.Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Text(skill.label(), style = MaterialTheme.typography.titleMedium)
                if (latest == null) Text(stringResource(R.string.progress_no_results_short), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                else Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(R.string.progress_latest), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                    Text(latest, style = MaterialTheme.typography.titleMedium, color = colors.primary)
                }
            }
            TrendChart(values, max, description, Modifier.padding(top = 6.dp), height = 80.dp, emptyMessage = null)
        }
    }
}

@Composable
private fun ResultRow(r: GameResultEntity) {
    val date = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(r.playedAt))
    Text(
        stringResource(R.string.progress_result_row, gameTitle(r.gameId), r.score, (r.accuracy * 100).toInt(), date),
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(vertical = 2.dp),
    )
}
