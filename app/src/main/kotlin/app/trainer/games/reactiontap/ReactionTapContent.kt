package app.trainer.games.reactiontap

import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import app.trainer.core.ReactionRound
import app.trainer.core.ReactionTapGame
import app.trainer.games.nowMillis

@Composable
fun ReactionTapContent(round: ReactionRound, onAnswer: (Long) -> Unit) {
    val colors = MaterialTheme.colorScheme
    var goAt by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(Unit) {
        delay(round.delayMs)
        goAt = nowMillis()
        delay(ReactionTapGame.GRACE_MS)
        onAnswer(ReactionTapGame.NO_RESPONSE)
    }

    val go = goAt != null
    Column(verticalArrangement = Arrangement.spacedBy(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            stringResource(R.string.reaction_prompt), style = MaterialTheme.typography.titleMedium,
            color = colors.onSurfaceVariant,
        )
        Surface(
            onClick = { onAnswer(goAt?.let { nowMillis() - it } ?: ReactionTapGame.EARLY) },
            modifier = Modifier.fillMaxWidth().heightIn(min = 280.dp),
            shape = MaterialTheme.shapes.extraLarge,
            color = if (go) colors.primary else colors.surfaceContainer,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    stringResource(if (go) R.string.reaction_go else R.string.reaction_wait),
                    style = MaterialTheme.typography.displayMedium,
                    color = if (go) colors.onPrimary else colors.onSurface,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive },
                )
            }
        }
        Text(stringResource(R.string.reaction_early_hint), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
    }
}
