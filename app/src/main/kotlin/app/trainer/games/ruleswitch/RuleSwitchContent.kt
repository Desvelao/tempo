package app.trainer.games.ruleswitch

import androidx.compose.ui.platform.LocalConfiguration
import app.trainer.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Icon
import app.trainer.ui.AppIcons
import app.trainer.ui.theme.isDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.trainer.core.CardColor
import app.trainer.core.CardShape
import app.trainer.core.Rule
import app.trainer.core.RuleSwitchRound
import app.trainer.ui.OptionGrid

@Composable
fun RuleSwitchContent(round: RuleSwitchRound, onAnswer: (Int) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val dark = isDarkTheme()
    val red = if (dark) Color(0xFFFF6B6B) else Color(0xFFE03E3E)
    val blue = if (dark) Color(0xFF7AA2FF) else Color(0xFF2F5BD1)
    val ruleLabel = stringResource(if (round.rule == Rule.COLOR) R.string.rule_colour else R.string.rule_shape)
    val options = if (round.rule == Rule.COLOR) {
        listOf(stringResource(R.string.card_red), stringResource(R.string.card_blue))
    } else {
        listOf(stringResource(R.string.card_circle), stringResource(R.string.card_square))
    }
    val cardDescription = stringResource(
        R.string.card_description,
        stringResource(if (round.color == CardColor.RED) R.string.card_red else R.string.card_blue),
        stringResource(if (round.shape == CardShape.CIRCLE) R.string.card_circle else R.string.card_square),
    ).lowercase(LocalConfiguration.current.locales[0])
    Column(verticalArrangement = Arrangement.spacedBy(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge, color = colors.surface,
            border = BorderStroke(1.5.dp, colors.secondary),
        ) {
            Row(
                Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically,
            ) {
                if (round.isSwitch) Icon(AppIcons.Swap, contentDescription = null, tint = colors.secondary)
                Text(
                    stringResource(if (round.isSwitch) R.string.rule_changed else R.string.rule_sort_by, ruleLabel),
                    style = MaterialTheme.typography.titleLarge, color = colors.secondary,
                )
            }
        }
        Surface(
            modifier = Modifier.size(220.dp),
            shape = MaterialTheme.shapes.extraLarge,
            color = colors.surfaceContainer,
            border = BorderStroke(1.dp, colors.outlineVariant),
        ) {
            Box(contentAlignment = Alignment.Center) {
                val color = if (round.color == CardColor.RED) red else blue
                val shape = if (round.shape == CardShape.CIRCLE) CircleShape else RoundedCornerShape(10.dp)
                Box(
                    Modifier.size(120.dp).clip(shape).background(color)
                        .semantics { contentDescription = cardDescription },
                )
            }
        }
        Text(
            stringResource(if (round.rule == Rule.COLOR) R.string.rule_which_colour else R.string.rule_which_shape),
            style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant,
        )
        OptionGrid(
            options.indices.toList(), { options[it] }, onAnswer, buttonHeight = 112.dp,
            icon = { index ->
                val swatch = Modifier.size(34.dp)
                when {
                    round.rule == Rule.COLOR && index == 0 -> Box(swatch.clip(CircleShape).background(red))
                    round.rule == Rule.COLOR -> Box(swatch.clip(CircleShape).background(blue))
                    index == 0 -> Box(swatch.clip(CircleShape).background(colors.onSurface))
                    else -> Box(swatch.clip(RoundedCornerShape(6.dp)).background(colors.onSurface))
                }
            },
        )
    }
}
