package app.trainer.ui

import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.trainer.TrainerApp
import app.trainer.settings.AppPrefs

/**
 * Plays a short tone and/or haptic tick (per the user's settings) once whenever [key] becomes
 * non-null, i.e. when a round's feedback is shown.
 */
@Composable
fun FeedbackEffect(key: Any?, correct: Boolean?) {
    val app = LocalContext.current.applicationContext as TrainerApp
    val prefs by app.settings.prefs.collectAsStateWithLifecycle(initialValue = AppPrefs())
    val view = LocalView.current
    val tone = remember { runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, 50) }.getOrNull() }
    DisposableEffect(Unit) { onDispose { tone?.release() } }
    LaunchedEffect(key) {
        if (key == null || correct == null) return@LaunchedEffect
        if (prefs.sound) {
            tone?.startTone(if (correct) ToneGenerator.TONE_PROP_ACK else ToneGenerator.TONE_PROP_NACK, 150)
        }
        if (prefs.haptics) {
            val type = when {
                Build.VERSION.SDK_INT >= 30 -> if (correct) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.REJECT
                else -> HapticFeedbackConstants.VIRTUAL_KEY
            }
            view.performHapticFeedback(type)
        }
    }
}

/** The app-wide audio controller, or null where there is no [TrainerApp] (previews). */
@Composable
fun rememberAudio(): app.trainer.audio.AudioController? =
    (LocalContext.current.applicationContext as? TrainerApp)?.audio
