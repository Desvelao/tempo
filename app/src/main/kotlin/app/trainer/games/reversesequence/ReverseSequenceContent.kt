package app.trainer.games.reversesequence

import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import app.trainer.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import app.trainer.core.ReverseSequenceRound
import app.trainer.ui.DigitPad

private const val SHOW_MS = 800L
private const val GAP_MS = 250L

@Composable
fun ReverseSequenceContent(round: ReverseSequenceRound, onAnswer: (List<Int>) -> Unit) {
    val colors = MaterialTheme.colorScheme
    var showing by remember { mutableStateOf(true) }
    var shown by remember { mutableIntStateOf(-1) }
    val entered: SnapshotStateList<Int> = remember { mutableStateListOf() }

    LaunchedEffect(Unit) {
        delay(300)
        for (i in round.digits.indices) {
            shown = i; delay(SHOW_MS)
            shown = -1; delay(GAP_MS)
        }
        showing = false
    }

    Column(verticalArrangement = Arrangement.spacedBy(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            stringResource(if (showing) R.string.reverse_remember else R.string.reverse_enter),
            style = MaterialTheme.typography.titleMedium, color = colors.onSurfaceVariant,
        )
        Surface(
            modifier = Modifier.fillMaxWidth().heightIn(min = 140.dp),
            shape = MaterialTheme.shapes.extraLarge, color = colors.surfaceContainer,
        ) {
            Box(contentAlignment = Alignment.Center) {
                val text = when {
                    showing -> if (shown >= 0) round.digits[shown].toString() else " "
                    else -> entered.joinToString(" ").ifEmpty { "–" }
                }
                Text(text, style = MaterialTheme.typography.displayLarge, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            }
        }
        if (!showing) {
            DigitPad(
                onDigit = { d ->
                    if (entered.size < round.digits.size) {
                        entered += d
                        if (entered.size == round.digits.size) onAnswer(entered.toList())
                    }
                },
                onUndo = { if (entered.isNotEmpty()) entered.removeAt(entered.lastIndex) },
            )
        }
    }
}
