package app.trainer.games.quickmath

import app.trainer.ui.theme.cappedSp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.trainer.core.MathRound
import app.trainer.ui.OptionGrid

@Composable
fun QuickMathContent(round: MathRound, onAnswer: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("${round.a} ${round.op.symbol} ${round.b}", style = MaterialTheme.typography.displayLarge.copy(fontSize = cappedSp(72), lineHeight = cappedSp(76)))
        Text("= ?", style = MaterialTheme.typography.displayMedium, color = MaterialTheme.colorScheme.primary)
        OptionGrid(round.options, { it.toString() }, onAnswer, buttonHeight = 96.dp, textStyle = MaterialTheme.typography.displaySmall)
    }
}
