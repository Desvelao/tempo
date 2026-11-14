package app.trainer.games.visualsearch

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.trainer.R
import app.trainer.core.VisualSearchRound

@Composable
fun VisualSearchContent(round: VisualSearchRound, onAnswer: (Int) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val targetDescription = stringResource(R.string.search_target, round.target)
    val glyphStyle = if (round.size >= 6) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineMedium
    Column(verticalArrangement = Arrangement.spacedBy(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.search_prompt), style = MaterialTheme.typography.titleMedium, color = colors.onSurfaceVariant)
            Surface(
                shape = MaterialTheme.shapes.extraLarge, color = colors.surface,
                border = BorderStroke(1.5.dp, colors.secondary),
                modifier = Modifier.semantics { contentDescription = targetDescription },
            ) {
                Text(
                    round.target, style = MaterialTheme.typography.headlineMedium, color = colors.secondary,
                    modifier = Modifier.padding(horizontal = 22.dp, vertical = 8.dp),
                )
            }
        }
        Column(Modifier.widthIn(max = 360.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            for (row in 0 until round.size) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (col in 0 until round.size) {
                        val index = row * round.size + col
                        Surface(
                            onClick = { onAnswer(index) },
                            color = colors.surfaceContainer,
                            shape = MaterialTheme.shapes.small,
                            border = BorderStroke(1.dp, colors.outlineVariant),
                            modifier = Modifier.weight(1f).aspectRatio(1f),
                        ) {
                            Box(contentAlignment = Alignment.Center) { Text(round.cells[index], style = glyphStyle) }
                        }
                    }
                }
            }
        }
    }
}
