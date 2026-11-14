package app.trainer.ui

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.trainer.settings.AppPrefs
import app.trainer.core.SessionPlanner
import app.trainer.core.SessionMode
import androidx.annotation.StringRes
import app.trainer.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.trainer.TrainerApp
import app.trainer.core.ListPlanner
import app.trainer.session.SessionViewModel
import kotlinx.coroutines.flow.first

private const val HOME = "home"
private const val PROGRESS = "progress"
private const val GAMES = "games"
private const val GAME = "game/{id}"
private const val SESSION = "session?list={list}"
private const val LIST_EDITOR = "list/{id}"
private const val SETTINGS = "settings"
private const val INSIGHTS = "insights"
private fun gameRoute(id: String) = "game/$id"
private fun sessionRoute(listId: Long = 0) = "session?list=$listId"
private fun listRoute(id: Long) = "list/$id"

private data class Tab(val route: String, @StringRes val label: Int, val icon: ImageVector)

private val tabs = listOf(
    Tab(HOME, R.string.tab_home, Icons.Filled.Home),
    Tab(PROGRESS, R.string.tab_progress, AppIcons.Progress),
    Tab(GAMES, R.string.tab_games, AppIcons.Games),
)

@Composable
fun AppNav() {
    val app = LocalContext.current.applicationContext as TrainerApp
    val nav = rememberNavController()
    val route = nav.currentBackStackEntryAsState().value?.destination?.route
    val prefs by app.settings.prefs.collectAsStateWithLifecycle(initialValue = AppPrefs())
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (tabs.any { it.route == route }) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = route == tab.route,
                            onClick = { switchTab(nav, tab.route) },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(stringResource(tab.label)) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = Color.Transparent,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(nav, startDestination = HOME, modifier = Modifier.padding(padding)) {
            composable(HOME) {
                HomeScreen(app.results, sessionLength = prefs.sessionLength, onSession = { nav.navigate(sessionRoute()) }, onSettings = { nav.navigate(SETTINGS) })
            }
            composable(PROGRESS) { ProgressScreen(app.results, onInsights = { nav.navigate(INSIGHTS) }) }
            composable(INSIGHTS) { InsightsScreen(app.results, onBack = { nav.popBackStack() }) }
            composable(GAMES) {
                GamesScreen(
                    app.results, app.gameLists,
                    onPlay = { nav.navigate(gameRoute(it)) },
                    onPlayList = { nav.navigate(sessionRoute(it)) },
                    onEditList = { nav.navigate(listRoute(it)) },
                )
            }
            composable(LIST_EDITOR, arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
                ListEditorScreen(app.gameLists, entry.arguments?.getLong("id") ?: 0L, onDone = { nav.popBackStack() })
            }
            composable(GAME) { entry ->
                val id = entry.arguments?.getString("id") ?: return@composable
                GameHost(
                    gameId = id, key = "free-$id", sessionId = null, primaryLabel = stringResource(R.string.action_play_again),
                    onPrimary = { nav.popBackStack(); nav.navigate(gameRoute(id)) },
                    onHome = { nav.popBackStack() },
                )
            }
            composable(SESSION, arguments = listOf(navArgument("list") { type = NavType.LongType; defaultValue = 0L })) { entry ->
                val explicit = entry.arguments?.getLong("list") ?: 0L
                val vm: SessionViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer {
                            SessionViewModel(
                                app.results,
                                resolveList = { random ->
                                    if (explicit != 0L) app.gameLists.get(explicit)
                                    else {
                                        val prefs = app.settings.prefs.first()
                                        ListPlanner.pick(prefs.sessionMode, app.gameLists.all(), prefs.sessionListId, random)
                                    }
                                },
                                planSkills = { random ->
                                    val prefs = app.settings.prefs.first()
                                    if (prefs.sessionMode == SessionMode.ADAPTIVE) {
                                        SessionPlanner.planAdaptive(
                                            levels = app.results.levels.first(),
                                            lastPlayed = app.results.lastPlayedBySkill(),
                                            now = System.currentTimeMillis(),
                                            focus = prefs.focusSkills,
                                            random = random,
                                            count = prefs.sessionLength,
                                        )
                                    } else {
                                        SessionPlanner.plan(random, prefs.sessionLength)
                                    }
                                },
                            )
                        }
                    },
                )
                SessionScreen(vm, onHome = { goHome(nav) })
            }
            composable(SETTINGS) { SettingsScreen(onBack = { nav.popBackStack() }) }
        }
    }
}

private fun switchTab(nav: NavController, route: String) {
    nav.navigate(route) {
        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

private fun goHome(nav: NavController) { nav.popBackStack(HOME, inclusive = false) }
