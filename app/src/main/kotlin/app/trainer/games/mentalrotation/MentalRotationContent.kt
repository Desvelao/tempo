package app.trainer.games.mentalrotation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.trainer.R
import app.trainer.core.MentalRotationRound

@Composable
fun MentalRotationContent(round: MentalRotationRound, onAnswer: (Int) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val targetDescription = stringResource(R.string.rotation_target)
    Column(verticalArrangement = Arrangement.spacedBy(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            stringResource(R.string.rotation_prompt), style = MaterialTheme.typography.titleMedium,
            color = colors.onSurfaceVariant,
        )
        Surface(
            shape = MaterialTheme.shapes.extraLarge, color = colors.surfaceContainer,
            border = BorderStroke(1.5.dp, colors.secondary),
            modifier = Modifier.semantics { contentDescription = targetDescription },
        ) {
            Box(Modifier.padding(18.dp)) { PatternGrid(round.target, round.size, 120.dp / round.size, colors.secondary) }
        }
        Column(Modifier.widthIn(max = 360.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            round.options.indices.chunked(2).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { index ->
                        val description = stringResource(R.string.rotation_option, index + 1)
                        Surface(
                            onClick = { onAnswer(index) },
                            color = colors.surfaceContainer,
                            shape = MaterialTheme.shapes.large,
                            border = BorderStroke(1.dp, colors.outlineVariant),
                            modifier = Modifier.weight(1f).aspectRatio(1f).semantics { contentDescription = description },
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                PatternGrid(round.options[index], round.size, 96.dp / round.size, colors.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** A [size] x [size] pattern drawn from boxes, so no image assets are needed. */
@Composable
private fun PatternGrid(pattern: List<Boolean>, size: Int, cell: Dp, onColor: Color) {
    val off = MaterialTheme.colorScheme.outlineVariant
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        for (row in 0 until size) {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                for (col in 0 until size) {
                    val on = pattern[row * size + col]
                    Box(Modifier.size(cell).background(if (on) onColor else off.copy(alpha = 0.35f), RoundedCornerShape(3.dp)))
                }
            }
        }
    }
}
