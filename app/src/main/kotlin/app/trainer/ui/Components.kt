package app.trainer.ui

import app.trainer.audio.Effect
import androidx.compose.ui.res.pluralStringResource
import app.trainer.games.GameCatalog
import app.trainer.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.graphics.vector.ImageVector.Builder
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val BigShape = RoundedCornerShape(22.dp)

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, height: Dp = 64.dp, textStyle: TextStyle = MaterialTheme.typography.titleLarge) {
    val audio = rememberAudio()
    Button(onClick = { audio?.play(Effect.TAP); onClick() }, modifier = modifier.fillMaxWidth().heightIn(min = height), shape = BigShape) {
        Text(text, style = textStyle)
    }
}

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, height: Dp = 56.dp) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().heightIn(min = height),
        shape = BigShape,
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant),
    ) { Text(text, style = MaterialTheme.typography.titleMedium) }
}

/** Dark panel button used for answer options. */
@Composable
fun ChoiceButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 76.dp,
    textStyle: TextStyle = MaterialTheme.typography.titleLarge,
    icon: (@Composable () -> Unit)? = null,
) {
    val audio = rememberAudio()
    Surface(
        onClick = { audio?.play(Effect.TAP); onClick() },
        modifier = modifier.heightIn(min = height),
        shape = BigShape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Box(Modifier.heightIn(min = height), contentAlignment = Alignment.Center) {
            if (icon == null) Text(text, style = textStyle)
            else Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                icon()
                Text(text, style = textStyle)
            }
        }
    }
}

/** 48dp square icon button with the panel look. */
@Composable
fun IconAction(icon: ImageVector, description: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier.size(48.dp),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Box(contentAlignment = Alignment.Center) { Icon(icon, contentDescription = description, tint = MaterialTheme.colorScheme.onSurface) }
    }
}

@Composable
fun FocusBar(progress: Float, modifier: Modifier = Modifier) {
    LinearProgressIndicator(
        progress = { progress.coerceIn(0f, 1f) },
        modifier = modifier.fillMaxWidth().heightIn(min = 12.dp),
        color = MaterialTheme.colorScheme.primary,
        trackColor = MaterialTheme.colorScheme.outlineVariant,
        strokeCap = StrokeCap.Round,
        gapSize = 0.dp,
        drawStopIndicator = {},
    )
}

/** Close button + "Round i / n" line shared by every game screen. */
@Composable
fun GameHeader(label: String, onClose: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        IconAction(Icons.Filled.Close, stringResource(R.string.action_close_game), onClose)
        Text(label, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Box(Modifier.size(48.dp))
    }
}

/** One short bar per round: correct, wrong or still to play. */
@Composable
fun RoundHistory(history: List<Boolean>, total: Int, modifier: Modifier = Modifier) {
    val ok = MaterialTheme.colorScheme.primary
    val bad = MaterialTheme.colorScheme.error
    val todo = MaterialTheme.colorScheme.outlineVariant
    val correct = history.count { it }
    val wrong = history.count { !it }
    val left = total - history.size
    val summary = listOf(
        pluralStringResource(R.plurals.history_correct, correct, correct),
        pluralStringResource(R.plurals.history_wrong, wrong, wrong),
        pluralStringResource(R.plurals.history_to_go, left, left),
    ).joinToString(", ")
    Row(
        modifier.fillMaxWidth().semantics { contentDescription = summary },
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
    ) {
        for (i in 0 until total) {
            val color = history.getOrNull(i)?.let { if (it) ok else bad } ?: todo
            Surface(color = color, shape = RoundedCornerShape(3.dp), modifier = Modifier.size(width = 22.dp, height = 6.dp)) {}
        }
    }
}

object AppIcons {
    private fun stroke(name: String, path: String): ImageVector =
        Builder(name, 24.dp, 24.dp, 24f, 24f).addPath(
            pathData = PathParser().parsePathString(path).toNodes(),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ).build()

    val Progress: ImageVector by lazy { stroke("progress", "M4 20V10M10 20V4M16 20v-8M22 20H2") }
    val Swap: ImageVector by lazy { stroke("swap", "M4 8h14M14 4l4 4-4 4M20 16H6M10 12l-4 4 4 4") }
    val Target: ImageVector by lazy { stroke("target", "M21 12a9 9 0 1 1-18 0a9 9 0 0 1 18 0zM16 12a4 4 0 1 1-8 0a4 4 0 0 1 8 0z") }
    val Lock: ImageVector by lazy { stroke("lock", "M5 11h14v10H5zM8 11V8a4 4 0 0 1 8 0v3") }
    val Games: ImageVector by lazy { stroke("games", "M3 3h7v7H3zM14 3h7v7h-7zM3 14h7v7H3zM14 14h7v7h-7z") }
}

/** The localised title of a game; falls back to the id for ids this version does not know. */
@Composable
fun gameTitle(id: String): String = GameCatalog.titleRes(id)?.let { stringResource(it) } ?: id

/** Digits 1-9 in a 3 x 3 pad plus an undo button, shared by the digit games. */
@Composable
fun DigitPad(onDigit: (Int) -> Unit, onUndo: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        (1..9).chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { d -> ChoiceButton(d.toString(), { onDigit(d) }, Modifier.weight(1f), height = 64.dp) }
            }
        }
        ChoiceButton(stringResource(R.string.reverse_undo), onUndo, Modifier.fillMaxWidth(), height = 56.dp)
    }
}
