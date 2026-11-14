package app.trainer.games.nback

import app.trainer.R
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.trainer.core.NBackRound
import app.trainer.ui.PrimaryButton

@Composable
fun NBackContent(round: NBackRound, onAnswer: (Boolean) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge, color = colors.surface,
            border = BorderStroke(1.5.dp, colors.secondary),
        ) {
            Text(stringResource(R.string.nback_title, round.n), style = MaterialTheme.typography.titleLarge, color = colors.secondary, modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp))
        }
        Text(
            pluralStringResource(R.plurals.nback_question, round.n, round.n),
            style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center,
        )
        Column(Modifier.widthIn(max = 316.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            for (row in 0 until 3) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    for (col in 0 until 3) {
                        val index = row * 3 + col
                        val lit = index == round.position
                        val cellDescription = stringResource(if (lit) R.string.nback_cell_highlighted else R.string.nback_cell, index + 1)
                        Surface(
                            color = if (lit) colors.secondary else colors.surfaceContainer,
                            shape = MaterialTheme.shapes.large,
                            border = if (lit) BorderStroke(4.dp, colors.onSurface) else BorderStroke(1.dp, colors.outlineVariant),
                            modifier = Modifier.weight(1f).aspectRatio(1f)
                                .semantics { contentDescription = cellDescription },
                        ) {}
                    }
                }
            }
        }
        PrimaryButton(stringResource(R.string.nback_match), { onAnswer(true) }, height = 84.dp, textStyle = MaterialTheme.typography.headlineSmall)
        Text(stringResource(R.string.nback_hint), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
    }
}
