package app.trainer.ui

import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.trainer.R
import app.trainer.core.Difficulty
import app.trainer.core.Skill
import app.trainer.data.ResultRepository
import java.time.LocalDate
import java.time.format.TextStyle

@Composable
fun HomeScreen(repository: ResultRepository, sessionLength: Int, onSession: () -> Unit, onSettings: () -> Unit) {
    val streak by repository.streak.collectAsStateWithLifecycle(initialValue = 0)
    val levels by repository.levels.collectAsStateWithLifecycle(initialValue = emptyMap())
    val days by repository.sessionDays.collectAsStateWithLifecycle(initialValue = emptySet())
    val colors = MaterialTheme.colorScheme
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.home_title), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
            IconAction(Icons.Filled.Settings, stringResource(R.string.action_settings), onSettings)
        }

        StreakCard(streak, days)

        Surface(
            onClick = onSession,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            color = colors.primary,
            contentColor = colors.onPrimary,
        ) {
            Row(Modifier.padding(22.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.home_start_session), style = MaterialTheme.typography.headlineSmall)
                    Text(pluralStringResource(R.plurals.home_session_summary, sessionLength, sessionLength, sessionLength * 3 / 2), style = MaterialTheme.typography.bodyMedium)
                }
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(34.dp))
            }
        }

        Text(
            stringResource(R.string.home_skill_levels),
            style = MaterialTheme.typography.labelLarge,
            color = colors.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Skill.entries.forEach { skill -> SkillRow(skill, levels[skill] ?: Difficulty.MIN_LEVEL) }
        }
    }
}

@Composable
private fun StreakCard(streak: Int, doneDays: Set<LocalDate>) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = colors.surfaceContainer,
        border = BorderStroke(1.dp, colors.outlineVariant),
    ) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Text(stringResource(R.string.home_streak), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                Text(pluralStringResource(R.plurals.days_count, streak, streak), style = MaterialTheme.typography.headlineSmall, color = colors.primary)
            }
            WeekStrip(doneDays)
        }
    }
}

@Composable
private fun SkillRow(skill: Skill, level: Int) {
    val colors = MaterialTheme.colorScheme
    val filled = (level + 1) / 2 // 10 segments for levels 1..20
    val skillName = skill.label()
    val description = stringResource(R.string.skill_level_description, skillName, level)
    Row(
        Modifier.fillMaxWidth().semantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(skillName, Modifier.widthIn(min = 124.dp, max = 200.dp).padding(end = 8.dp), style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            for (i in 1..10) {
                Box(
                    Modifier.weight(1f).height(10.dp).clip(RoundedCornerShape(2.dp))
                        .background(if (i <= filled) colors.primary else colors.outlineVariant),
                )
            }
        }
        Text("$level", Modifier.widthIn(min = 32.dp), style = MaterialTheme.typography.titleMedium, textAlign = androidx.compose.ui.text.style.TextAlign.End)
    }
}
