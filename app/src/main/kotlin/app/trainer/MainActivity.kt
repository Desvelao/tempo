package app.trainer

import androidx.compose.ui.platform.LocalContext
import android.provider.Settings
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import app.trainer.ui.theme.TrainerTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.trainer.settings.ThemeMode
import app.trainer.ui.AppNav
import app.trainer.ui.OnboardingScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as TrainerApp
        setContent {
            val prefs by app.settings.prefs.collectAsStateWithLifecycle(initialValue = null)
            val scope = rememberCoroutineScope()
            val current = prefs
            val dark = when (current?.theme ?: ThemeMode.DARK) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            DisposableEffect(dark) {
                enableEdgeToEdge(
                    statusBarStyle = if (dark) SystemBarStyle.dark(Color.TRANSPARENT)
                    else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
                    navigationBarStyle = if (dark) SystemBarStyle.dark(Color.TRANSPARENT)
                    else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
                )
                onDispose {}
            }
            val context = LocalContext.current
            // The system "remove animations" setting counts as asking for reduced motion too.
            val systemReducedMotion = remember {
                Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
            }
            TrainerTheme(
                dark = dark,
                colorBlind = current?.colorBlindPalette ?: false,
                reducedMotion = (current?.reducedMotion ?: false) || systemReducedMotion,
            ) {
                // The surface fills the whole window (including behind the system bars); only its content is inset.
                Surface(Modifier.fillMaxSize()) {
                    Box(Modifier.fillMaxSize().safeDrawingPadding()) {
                        when {
                            current == null -> Unit // stored preferences still loading
                            !current.onboarded -> OnboardingScreen(onDone = { scope.launch { app.settings.setOnboarded() } })
                            else -> AppNav()
                        }
                    }
                }
            }
        }
    }
}
