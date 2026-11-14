package app.trainer.audio

import android.app.Activity
import android.app.Application
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.SoundPool
import android.os.Bundle
import app.trainer.settings.AppPrefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private val attributes = AudioAttributes.Builder()
    .setUsage(AudioAttributes.USAGE_GAME)
    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
    .build()

/** How long the app may stay hidden (rotation, a dialog, a quick app switch) before the music pauses. */
private const val HIDE_GRACE_MS = 1_500L
private const val DUCKED_VOLUME = 0.3f

/**
 * Owns the optional extra sound effects and the ambient music. Both are off unless the user enables them.
 * Effects go through a [SoundPool] (overlapping, no replay races). The music is one looping [AudioTrack]
 * that is paused, not rebuilt, when the app is hidden or another app briefly takes the audio focus.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AudioController(private val app: Application, prefs: Flow<AppPrefs>) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val audioManager = app.getSystemService(AudioManager::class.java)

    @Volatile private var moreSounds = false
    private val visible = MutableStateFlow(false)

    // Effects
    private var pool: SoundPool? = null
    private val soundIds = HashMap<Effect, Int>()
    private val loaded = HashSet<Int>()

    // Music
    private var loopPcm: ShortArray? = null
    private var music: AudioTrack? = null
    private var focus: AudioFocusRequest? = null
    private var pausedByFocus = false

    init {
        var started = 0
        app.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityStarted(activity: Activity) { started++; visible.value = true }
            override fun onActivityStopped(activity: Activity) { started--; visible.value = started > 0 }
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
        scope.launch {
            prefs.map { it.moreSounds }.distinctUntilChanged().collect {
                moreSounds = it
                if (it) loadEffects() else releaseEffects()
            }
        }
        scope.launch {
            // collectLatest cancels a pending pause when the app comes back within the grace period.
            combine(prefs.map { it.music }.distinctUntilChanged(), visible) { on, shown -> on to shown }
                .distinctUntilChanged()
                .collectLatest { (on, shown) ->
                    when {
                        !on -> releaseMusic()
                        shown -> resumeMusic()
                        else -> { delay(HIDE_GRACE_MS); pauseMusic() }
                    }
                }
        }
    }

    /** Plays an extra effect when "more sound effects" is on. Cheap to call; does nothing otherwise. */
    fun play(effect: Effect) {
        if (moreSounds) preview(effect)
    }

    /** Plays an effect regardless of the setting, to confirm a switch the user just turned on. */
    fun preview(effect: Effect) {
        val pool = pool
        val id = soundIds[effect]
        if (pool == null || id == null || id !in loaded) {
            // Not ready yet (first use right after enabling): load now, this one is skipped.
            if (pool == null) scope.launch { loadEffects() }
            return
        }
        pool.play(id, 1f, 1f, 1, 0, 1f)
    }

    private suspend fun loadEffects() {
        if (pool != null) return
        val sp = SoundPool.Builder().setMaxStreams(4).setAudioAttributes(attributes).build()
        pool = sp
        sp.setOnLoadCompleteListener { _, id, status -> if (status == 0) loaded.add(id) }
        val files = withContext(Dispatchers.IO) {
            Effect.entries.map { e ->
                val f = File(app.cacheDir, "fx_${e.name.lowercase()}.wav")
                f.writeBytes(SoundSynth.wav(SoundSynth.effect(e)))
                e to f
            }
        }
        files.forEach { (e, f) -> soundIds[e] = sp.load(f.absolutePath, 1) }
    }

    private fun releaseEffects() {
        pool?.release()
        pool = null
        soundIds.clear()
        loaded.clear()
    }

    private suspend fun ensureMusic(): AudioTrack? {
        music?.let { return it }
        val pcm = loopPcm ?: withContext(Dispatchers.Default) { SoundSynth.ambientLoop() }.also { loopPcm = it }
        music?.let { return it } // built by a concurrent call while the loop was being generated
        return runCatching {
            AudioTrack.Builder()
                .setAudioAttributes(attributes)
                .setAudioFormat(
                    AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SoundSynth.SAMPLE_RATE).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build(),
                )
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(pcm.size * 2)
                .build().also {
                    it.write(pcm, 0, pcm.size)
                    it.setLoopPoints(0, pcm.size, -1)
                    music = it
                }
        }.getOrNull()
    }

    private suspend fun resumeMusic() {
        val track = ensureMusic() ?: return
        pausedByFocus = false
        requestFocus()
        runCatching { track.setVolume(1f); track.play() }
    }

    private fun pauseMusic() {
        runCatching { music?.pause() }
        abandonFocus()
    }

    private fun releaseMusic() {
        music?.let { runCatching { it.stop(); it.release() } }
        music = null
        abandonFocus()
    }

    private fun requestFocus() {
        if (focus != null) return
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(attributes)
            .setOnAudioFocusChangeListener { change ->
                val track = music ?: return@setOnAudioFocusChangeListener
                when (change) {
                    AudioManager.AUDIOFOCUS_GAIN -> runCatching {
                        track.setVolume(1f)
                        if (pausedByFocus) { pausedByFocus = false; track.play() }
                    }
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> runCatching { track.setVolume(DUCKED_VOLUME) }
                    // A transient loss resumes on GAIN; a permanent one resumes when the app is shown again.
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT, AudioManager.AUDIOFOCUS_LOSS -> {
                        pausedByFocus = change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT
                        runCatching { track.pause() }
                        if (change == AudioManager.AUDIOFOCUS_LOSS) focus = null
                    }
                }
            }
            .build()
        focus = request
        audioManager.requestAudioFocus(request)
    }

    private fun abandonFocus() {
        focus?.let { audioManager.abandonAudioFocusRequest(it) }
        focus = null
    }
}
