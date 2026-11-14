package app.trainer.games.oddoneout

import app.trainer.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.trainer.core.OddOneOutRound

@Composable
fun OddOneOutContent(round: OddOneOutRound, onAnswer: (Int) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(stringResource(R.string.odd_prompt), style = MaterialTheme.typography.titleMedium, color = colors.onSurfaceVariant)
        Column(Modifier.widthIn(max = 360.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            for (row in 0 until round.size) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (col in 0 until round.size) {
                        val index = row * round.size + col
                        Surface(
                            onClick = { onAnswer(index) },
                            color = colors.surfaceContainer,
                            shape = MaterialTheme.shapes.medium,
                            border = BorderStroke(1.dp, colors.outlineVariant),
                            modifier = Modifier.weight(1f).aspectRatio(1f),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    if (index == round.oddIndex) round.odd else round.base,
                                    style = MaterialTheme.typography.headlineMedium,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
