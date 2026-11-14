package app.trainer.games.stroop

import app.trainer.ui.theme.LocalColorBlindPalette
import androidx.annotation.StringRes
import app.trainer.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.trainer.core.InkColor
import app.trainer.core.StroopRound
import app.trainer.ui.OptionGrid
import app.trainer.ui.theme.isDarkTheme

@StringRes
fun InkColor.titleRes(): Int = when (this) {
    InkColor.RED -> R.string.ink_red
    InkColor.BLUE -> R.string.ink_blue
    InkColor.GREEN -> R.string.ink_green
    InkColor.YELLOW -> R.string.ink_yellow
    InkColor.ORANGE -> R.string.ink_orange
    InkColor.PURPLE -> R.string.ink_purple
}

// Inks must stay legible on the panel: bright ones on dark, deeper ones on light.
private fun InkColor.color(dark: Boolean, colorBlind: Boolean) = if (colorBlind) colorBlindInk(dark) else standardInk(dark)

private fun InkColor.standardInk(dark: Boolean) = when (this) {
    InkColor.RED -> if (dark) Color(0xFFFF5A5A) else Color(0xFFC62828)
    InkColor.BLUE -> if (dark) Color(0xFF5B9DFF) else Color(0xFF1558B0)
    InkColor.GREEN -> if (dark) Color(0xFF4ADE80) else Color(0xFF1B7A3A)
    InkColor.YELLOW -> if (dark) Color(0xFFFACC15) else Color(0xFF8A6A00)
    InkColor.ORANGE -> if (dark) Color(0xFFFB923C) else Color(0xFFB54708)
    InkColor.PURPLE -> if (dark) Color(0xFFC084FC) else Color(0xFF7B2FBE)
}

/**
 * Inks chosen to stay apart under red-green colour blindness (Okabe-Ito based on dark panels, deeper tones on
 * light ones). The names still match: the word "Red" is a vermillion, "Orange" a true orange, and so on.
 */
internal fun InkColor.colorBlindInk(dark: Boolean) = when (this) {
    InkColor.RED -> if (dark) Color(0xFFD55E00) else Color(0xFFB01E00)
    InkColor.BLUE -> if (dark) Color(0xFF56B4E9) else Color(0xFF1F5FBF)
    InkColor.GREEN -> if (dark) Color(0xFF009E73) else Color(0xFF00785A)
    InkColor.YELLOW -> if (dark) Color(0xFFF0E442) else Color(0xFF6F6200)
    InkColor.ORANGE -> if (dark) Color(0xFFE69F00) else Color(0xFFD26A00)
    InkColor.PURPLE -> if (dark) Color(0xFFCC79A7) else Color(0xFF8E3F9C)
}

@Composable
fun StroopContent(round: StroopRound, onAnswer: (InkColor) -> Unit) {
    val labels = round.options.associateWith { stringResource(it.titleRes()) }
    Column(verticalArrangement = Arrangement.spacedBy(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(stringResource(R.string.stroop_prompt), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Surface(
            modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(stringResource(round.word.titleRes()), color = round.ink.color(isDarkTheme(), LocalColorBlindPalette.current), style = MaterialTheme.typography.displayLarge)
            }
        }
        OptionGrid(round.options, { labels.getValue(it) }, onAnswer)
    }
}
