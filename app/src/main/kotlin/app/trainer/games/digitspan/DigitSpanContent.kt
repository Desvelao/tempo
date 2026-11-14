package app.trainer.games.digitspan

import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.trainer.R
import app.trainer.core.DigitSpanAnswer
import app.trainer.core.DigitSpanRound
import app.trainer.ui.ChoiceButton
import app.trainer.ui.DigitPad
import kotlinx.coroutines.delay

private const val SHOW_MS = 800L
private const val GAP_MS = 250L

private enum class Phase { Show, Check, Recall }

@Composable
fun DigitSpanContent(round: DigitSpanRound, onAnswer: (DigitSpanAnswer) -> Unit) {
    val colors = MaterialTheme.colorScheme
    var phase by remember { mutableStateOf(Phase.Show) }
    var shown by remember { mutableIntStateOf(-1) }
    var verdict by remember { mutableStateOf<Boolean?>(null) }
    val entered: SnapshotStateList<Int> = remember { mutableStateListOf() }

    LaunchedEffect(Unit) {
        delay(300)
        for (i in round.digits.indices) {
            shown = i; delay(SHOW_MS)
            shown = -1; delay(GAP_MS)
        }
        phase = Phase.Check
    }

    Column(verticalArrangement = Arrangement.spacedBy(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            stringResource(
                when (phase) {
                    Phase.Show -> R.string.reverse_remember
                    Phase.Check -> R.string.span_check
                    Phase.Recall -> R.string.span_recall
                },
            ),
            style = MaterialTheme.typography.titleMedium, color = colors.onSurfaceVariant,
        )
        Surface(
            modifier = Modifier.fillMaxWidth().heightIn(min = 140.dp),
            shape = MaterialTheme.shapes.extraLarge, color = colors.surfaceContainer,
        ) {
            Box(contentAlignment = Alignment.Center) {
                val text = when (phase) {
                    Phase.Show -> if (shown >= 0) round.digits[shown].toString() else " "
                    Phase.Check -> stringResource(R.string.span_sum, round.a, round.b, round.shownSum)
                    Phase.Recall -> entered.joinToString(" ").ifEmpty { "–" }
                }
                Text(
                    text, style = if (phase == Phase.Check) MaterialTheme.typography.displayMedium else MaterialTheme.typography.displayLarge,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
        }
        when (phase) {
            Phase.Show -> Unit
            Phase.Check -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ChoiceButton(stringResource(R.string.span_sum_right), { verdict = true; phase = Phase.Recall }, Modifier.weight(1f), height = 84.dp)
                ChoiceButton(stringResource(R.string.span_sum_wrong), { verdict = false; phase = Phase.Recall }, Modifier.weight(1f), height = 84.dp)
            }
            Phase.Recall -> DigitPad(
                onDigit = { d ->
                    if (entered.size < round.digits.size) {
                        entered += d
                        if (entered.size == round.digits.size) onAnswer(DigitSpanAnswer(verdict == true, entered.toList()))
                    }
                },
                onUndo = { if (entered.isNotEmpty()) entered.removeAt(entered.lastIndex) },
            )
        }
    }
}
