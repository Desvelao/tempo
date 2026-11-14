package app.trainer.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.trainer.R
import app.trainer.core.InsightResult
import app.trainer.core.Insights
import app.trainer.core.Skill
import app.trainer.data.ResultRepository
import app.trainer.games.GameCatalog
import java.text.NumberFormat
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.abs
import kotlin.math.roundToLong

/** Summaries of the played history: strongest and weakest skill, best time of day, weekly change, personal bests. */
@Composable
fun InsightsScreen(repository: ResultRepository, onBack: () -> Unit) {
    val results by repository.all.collectAsStateWithLifecycle(initialValue = emptyList())
    val levels by repository.levels.collectAsStateWithLifecycle(initialValue = emptyMap())
    val locale = LocalConfiguration.current.locales[0]
    val format = remember(locale) { NumberFormat.getIntegerInstance(locale) }
    val colors = MaterialTheme.colorScheme

    val insightResults = remember(results) {
        results.map { InsightResult(it.skill, it.gameId, it.score, it.accuracy, it.playedAt) }
    }
    val now = remember(results) { System.currentTimeMillis() }
    val weakest = Insights.weakest(levels)
    val strongest = Insights.strongest(levels)
    val bestHour = remember(insightResults) { Insights.bestHour(insightResults, ZoneId.systemDefault()) }
    val change = remember(insightResults, now) { Insights.weeklyChange(insightResults, now) }
    val bests = remember(insightResults) { Insights.personalBests(insightResults) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            IconAction(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back), onBack)
            Text(stringResource(R.string.insights_title), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
        }

        if (results.isEmpty()) {
            Text(stringResource(R.string.insights_empty), style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
            return@Column
        }

        if (strongest != null && weakest != null) {
            InsightCard(stringResource(R.string.insights_strongest)) {
                Text(stringResource(R.string.insights_skill_level, strongest.label(), levels.getValue(strongest)), style = MaterialTheme.typography.titleLarge, color = colors.primary)
            }
            InsightCard(stringResource(R.string.insights_weakest)) {
                Text(stringResource(R.string.insights_skill_level, weakest.label(), levels.getValue(weakest)), style = MaterialTheme.typography.titleLarge)
                Text(stringResource(R.string.insights_tip, weakest.label()), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            }
        }

        InsightCard(stringResource(R.string.insights_best_time)) {
            if (bestHour == null) {
                Text(stringResource(R.string.insights_best_time_empty), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            } else {
                val time = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale).format(LocalTime.of(bestHour, 0))
                Text(stringResource(R.string.insights_best_time_value, time), style = MaterialTheme.typography.titleLarge)
            }
        }

        InsightCard(stringResource(R.string.insights_week)) {
            if (change.isEmpty()) {
                Text(stringResource(R.string.insights_week_empty), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            } else {
                Skill.entries.filter { it in change }.forEach { skill ->
                    val delta = change.getValue(skill).roundToLong()
                    // The arrow and the sign carry the meaning, the colour only reinforces it.
                    val text = when {
                        delta > 0 -> stringResource(R.string.insights_week_up, format.format(delta))
                        delta < 0 -> stringResource(R.string.insights_week_down, format.format(abs(delta)))
                        else -> stringResource(R.string.insights_week_same)
                    }
                    Row(Modifier.fillMaxWidth().heightIn(min = 40.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(skill.label(), style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text, style = MaterialTheme.typography.titleMedium,
                            color = if (delta > 0) colors.primary else if (delta < 0) colors.error else colors.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        InsightCard(stringResource(R.string.insights_bests)) {
            GameCatalog.all.filter { it.id in bests }.forEach { game ->
                val score = format.format(bests.getValue(game.id))
                val description = stringResource(R.string.insights_best_row, stringResource(game.titleRes), score)
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 40.dp).semantics(mergeDescendants = true) { contentDescription = description },
                    horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(game.titleRes), style = MaterialTheme.typography.bodyLarge)
                    Text(score, style = MaterialTheme.typography.titleMedium, color = colors.primary)
                }
            }
        }
    }
}

@Composable
private fun InsightCard(title: String, content: @Composable () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = colors.surfaceContainer,
        border = BorderStroke(1.dp, colors.outlineVariant),
    ) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant, modifier = Modifier.semantics { heading() })
            content()
        }
    }
}
