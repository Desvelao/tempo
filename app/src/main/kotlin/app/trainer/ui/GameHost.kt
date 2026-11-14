package app.trainer.ui

import app.trainer.games.digitspan.DigitSpanContent
import app.trainer.games.visualsearch.VisualSearchContent
import app.trainer.games.mentalrotation.MentalRotationContent
import app.trainer.core.DigitSpanGame
import app.trainer.core.VisualSearchGame
import app.trainer.core.MentalRotationGame
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.trainer.TrainerApp
import app.trainer.core.Difficulty
import app.trainer.core.Game
import app.trainer.core.NBackGame
import app.trainer.core.OddOneOutGame
import app.trainer.core.ReactionTapGame
import app.trainer.core.ReverseSequenceGame
import app.trainer.core.QuickMathGame
import app.trainer.core.RuleSwitchGame
import app.trainer.core.StroopGame
import app.trainer.games.GameCatalog
import app.trainer.games.RoundGameViewModel
import app.trainer.games.gridrecall.GridRecallScreen
import app.trainer.games.gridrecall.GridRecallViewModel
import app.trainer.games.nback.NBackContent
import app.trainer.games.pairmatch.PairMatchScreen
import app.trainer.games.pairmatch.PairMatchViewModel
import app.trainer.games.trailalternate.TrailAlternateScreen
import app.trainer.games.trailalternate.TrailAlternateViewModel
import app.trainer.games.oddoneout.OddOneOutContent
import app.trainer.games.reactiontap.ReactionTapContent
import app.trainer.games.reversesequence.ReverseSequenceContent
import app.trainer.games.quickmath.QuickMathContent
import app.trainer.games.ruleswitch.RuleSwitchContent
import app.trainer.games.stroop.StroopContent

/**
 * Plays one game, starting at the skill's stored rating. [key] scopes the ViewModel so that
 * consecutive games in a session (or a replay) each get a fresh one.
 */
@Composable
fun GameHost(
    gameId: String,
    key: String,
    sessionId: Long?,
    primaryLabel: String,
    onPrimary: () -> Unit,
    onHome: () -> Unit,
) {
    val app = LocalContext.current.applicationContext as TrainerApp
    val levels by app.results.levels.collectAsStateWithLifecycle(initialValue = null)
    val known = levels ?: return // wait for the stored ratings before starting
    val start = known[GameCatalog.skillOf(gameId)] ?: Difficulty.MIN_LEVEL
    when (gameId) {
        GameCatalog.GRID_RECALL -> {
            val vm: GridRecallViewModel = viewModel(
                key = key,
                factory = viewModelFactory { initializer { GridRecallViewModel(app.results, start, sessionId = sessionId) } },
            )
            GridRecallScreen(vm, primaryLabel, onPrimary, onHome)
        }
        GameCatalog.PAIR_MATCH -> {
            val vm: PairMatchViewModel = viewModel(
                key = key,
                factory = viewModelFactory { initializer { PairMatchViewModel(app.results, start, sessionId = sessionId) } },
            )
            PairMatchScreen(vm, primaryLabel, onPrimary, onHome)
        }
        GameCatalog.TRAIL_ALTERNATE -> {
            val vm: TrailAlternateViewModel = viewModel(
                key = key,
                factory = viewModelFactory { initializer { TrailAlternateViewModel(app.results, start, sessionId = sessionId) } },
            )
            TrailAlternateScreen(vm, primaryLabel, onPrimary, onHome)
        }
        GameCatalog.STROOP -> RoundHost(app, key, gameId, start, sessionId, primaryLabel, onPrimary, onHome, ::StroopGame) { r, a -> StroopContent(r, a) }
        GameCatalog.QUICK_MATH -> RoundHost(app, key, gameId, start, sessionId, primaryLabel, onPrimary, onHome, ::QuickMathGame) { r, a -> QuickMathContent(r, a) }
        GameCatalog.N_BACK -> RoundHost(app, key, gameId, start, sessionId, primaryLabel, onPrimary, onHome, ::NBackGame) { r, a -> NBackContent(r, a) }
        GameCatalog.ODD_ONE_OUT -> RoundHost(app, key, gameId, start, sessionId, primaryLabel, onPrimary, onHome, ::OddOneOutGame) { r, a -> OddOneOutContent(r, a) }
        GameCatalog.REVERSE_SEQUENCE -> RoundHost(app, key, gameId, start, sessionId, primaryLabel, onPrimary, onHome, ::ReverseSequenceGame) { r, a -> ReverseSequenceContent(r, a) }
        GameCatalog.REACTION_TAP -> RoundHost(app, key, gameId, start, sessionId, primaryLabel, onPrimary, onHome, ::ReactionTapGame) { r, a -> ReactionTapContent(r, a) }
        GameCatalog.MENTAL_ROTATION -> RoundHost(app, key, gameId, start, sessionId, primaryLabel, onPrimary, onHome, ::MentalRotationGame) { r, a -> MentalRotationContent(r, a) }
        GameCatalog.VISUAL_SEARCH -> RoundHost(app, key, gameId, start, sessionId, primaryLabel, onPrimary, onHome, ::VisualSearchGame) { r, a -> VisualSearchContent(r, a) }
        GameCatalog.DIGIT_SPAN -> RoundHost(app, key, gameId, start, sessionId, primaryLabel, onPrimary, onHome, ::DigitSpanGame) { r, a -> DigitSpanContent(r, a) }
        GameCatalog.RULE_SWITCH -> RoundHost(app, key, gameId, start, sessionId, primaryLabel, onPrimary, onHome, ::RuleSwitchGame) { r, a -> RuleSwitchContent(r, a) }
    }
}

@Suppress("UNCHECKED_CAST")
@Composable
private fun <R : Any, A : Any> RoundHost(
    app: TrainerApp,
    key: String,
    gameId: String,
    start: Int,
    sessionId: Long?,
    primaryLabel: String,
    onPrimary: () -> Unit,
    onHome: () -> Unit,
    newGame: () -> Game<R, A>,
    content: @Composable (R, (A) -> Unit) -> Unit,
) {
    val vm = viewModel(
        modelClass = RoundGameViewModel::class.java as Class<RoundGameViewModel<R, A>>,
        key = key,
        factory = viewModelFactory {
            initializer { RoundGameViewModel(newGame(), app.results, startLevel = start, sessionId = sessionId) }
        },
    )
    val state by vm.state.collectAsStateWithLifecycle()
    RoundGameScreen(state, vm::onAnswer, gameTitle(gameId), primaryLabel, onPrimary, onHome, content)
}
