package app.trainer.ui

import app.trainer.ui.theme.LocalAccentColors
import app.trainer.R
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.trainer.core.Difficulty
import app.trainer.data.GameListRepository
import app.trainer.data.ResultRepository
import app.trainer.games.GameCatalog
import app.trainer.ui.theme.isDarkTheme

/** Free play: pick any single game. */
@Composable
fun GamesScreen(
    repository: ResultRepository,
    lists: GameListRepository,
    onPlay: (String) -> Unit,
    onPlayList: (Long) -> Unit,
    onEditList: (Long) -> Unit,
) {
    val savedLists by lists.lists.collectAsStateWithLifecycle(initialValue = emptyList())
    val levels by repository.levels.collectAsStateWithLifecycle(initialValue = emptyMap())
    val colors = MaterialTheme.colorScheme
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.tab_games), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
        Text(stringResource(R.string.games_subtitle), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        Text(stringResource(R.string.games_my_lists), style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
        savedLists.forEach { list ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = colors.surfaceContainer,
                border = BorderStroke(1.5.dp, colors.outlineVariant),
            ) {
                Row(Modifier.padding(start = 18.dp, top = 8.dp, bottom = 8.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text(list.name, style = MaterialTheme.typography.titleLarge)
                        Text(
                            pluralStringResource(if (list.shuffle) R.plurals.games_list_count_shuffled else R.plurals.games_list_count, list.gameIds.size, list.gameIds.size),
                            style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant,
                        )
                    }
                    IconAction(Icons.Filled.Edit, stringResource(R.string.games_edit_list, list.name), { onEditList(list.id) })
                    IconAction(Icons.Filled.PlayArrow, stringResource(R.string.games_play_list, list.name), { if (list.gameIds.isNotEmpty()) onPlayList(list.id) })
                }
            }
        }
        SecondaryButton(stringResource(R.string.games_new_list), { onEditList(0) })
        Text(stringResource(R.string.games_single), style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
        GameCatalog.all.forEach { game ->
            Surface(
                onClick = { onPlay(game.id) },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = colors.surfaceContainer,
                border = BorderStroke(1.5.dp, colors.outlineVariant),
            ) {
                Row(Modifier.padding(horizontal = 18.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    GameIcon(game.id)
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(game.titleRes), style = MaterialTheme.typography.titleLarge)
                        Text(
                            stringResource(R.string.games_skill_level, game.skill.label(), levels[game.skill] ?: Difficulty.MIN_LEVEL),
                            style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant,
                        )
                    }
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = colors.onSurfaceVariant)
                }
            }
        }
    }
}

/** Small symbolic icon per game, drawn from shapes so no image assets are needed. */
@Composable
private fun GameIcon(gameId: String) {
    val colors = MaterialTheme.colorScheme
    val accents = LocalAccentColors.current
    Surface(
        shape = RoundedCornerShape(16.dp), color = colors.background,
        border = BorderStroke(1.dp, colors.outlineVariant), modifier = Modifier.size(56.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            fun cell(color: Color, size: Int = 14, round: Int = 4) =
                Modifier.size(size.dp).background(color, RoundedCornerShape(round.dp))
            when (gameId) {
                GameCatalog.GRID_RECALL -> Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { Box(cell(colors.primary)); Box(cell(colors.outlineVariant)) }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { Box(cell(colors.outlineVariant)); Box(cell(colors.primary)) }
                }
                GameCatalog.STROOP -> Text("Aa", style = MaterialTheme.typography.titleLarge, color = accents.warm)
                GameCatalog.QUICK_MATH -> Text("7×4", style = MaterialTheme.typography.titleMedium, color = colors.primary)
                GameCatalog.N_BACK -> Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (r in 0 until 3) Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (c in 0 until 3) Box(cell(if (r == 1 && c == 1) colors.secondary else colors.outlineVariant, 11, 3))
                    }
                }
                GameCatalog.ODD_ONE_OUT -> Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (r in 0 until 2) Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (c in 0 until 2) Box(cell(if (r == 1 && c == 1) colors.primary else colors.outlineVariant, 14, 7))
                    }
                }
                GameCatalog.REVERSE_SEQUENCE -> Text("3 2 1", style = MaterialTheme.typography.titleMedium, color = colors.secondary)
                GameCatalog.PAIR_MATCH -> Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(cell(colors.primary, 16, 5)); Box(cell(colors.primary, 16, 5))
                }
                GameCatalog.TRAIL_ALTERNATE -> Text("1·A", style = MaterialTheme.typography.titleMedium, color = colors.primary)
                GameCatalog.MENTAL_ROTATION -> Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) { Box(cell(colors.primary, 11, 3)); Box(cell(colors.outlineVariant, 11, 3)) }
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) { Box(cell(colors.primary, 11, 3)); Box(cell(colors.outlineVariant, 11, 3)) }
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) { Box(cell(colors.primary, 11, 3)); Box(cell(colors.primary, 11, 3)) }
                }
                GameCatalog.VISUAL_SEARCH -> Text("OQO", style = MaterialTheme.typography.titleMedium, color = colors.secondary)
                GameCatalog.DIGIT_SPAN -> Text("4 7 2", style = MaterialTheme.typography.titleMedium, color = colors.primary)
                GameCatalog.REACTION_TAP -> Box(Modifier.size(26.dp).background(colors.primary, CircleShape))
                else -> Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(16.dp).background(accents.warm, CircleShape))
                    Box(cell(accents.cool, 16, 3))
                }
            }
        }
    }
}
