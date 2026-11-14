package app.trainer.ui

import app.trainer.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.TextStyle

/** The last seven days, ending today; a filled dot marks a day with a completed session. */
@Composable
fun WeekStrip(doneDays: Set<LocalDate>, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val locale = LocalConfiguration.current.locales[0]
    val today = LocalDate.now()
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        for (offset in 6 downTo 0) {
            val day = today.minusDays(offset.toLong())
            val done = day in doneDays
            val name = day.dayOfWeek.getDisplayName(TextStyle.FULL, locale)
            val description = stringResource(if (done) R.string.week_day_done else R.string.week_day_none, name)
            Column(
                Modifier.semantics { contentDescription = description },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                val dot = Modifier.size(28.dp).clip(CircleShape)
                Box(
                    when {
                        done -> dot.background(colors.primary)
                        offset == 0 -> dot.border(2.dp, colors.outline, CircleShape)
                        else -> dot.border(2.dp, colors.outlineVariant, CircleShape)
                    },
                )
                Text(
                    day.dayOfWeek.getDisplayName(TextStyle.NARROW, locale),
                    style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant,
                )
            }
        }
    }
}
