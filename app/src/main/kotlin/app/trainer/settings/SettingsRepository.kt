package app.trainer.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.trainer.core.SessionMode
import app.trainer.core.SessionPlanner
import app.trainer.core.Skill
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

data class ReminderSettings(val enabled: Boolean = false, val hour: Int = 20, val minute: Int = 0)

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class AppPrefs(
    val onboarded: Boolean = false,
    val sound: Boolean = true,
    val haptics: Boolean = true,
    /** Extra effects (taps, countdown, fanfare). Off by default. */
    val moreSounds: Boolean = false,
    /** Ambient background music. Off by default. */
    val music: Boolean = false,
    val theme: ThemeMode = ThemeMode.DARK,
    val sessionMode: SessionMode = SessionMode.RANDOM,
    val sessionListId: Long = -1,
    /** Games per daily session in the random and adaptive modes (3..5). */
    val sessionLength: Int = DEFAULT_SESSION_LENGTH,
    /** Skills the adaptive mode favours; empty means no preference. */
    val focusSkills: Set<Skill> = emptySet(),
    /** Replaces decorative animation (the round timer bar) with stepped visuals. */
    val reducedMotion: Boolean = false,
    /** Blue and orange instead of lime and coral, and a colour-blind safe set of Stroop inks. */
    val colorBlindPalette: Boolean = false,
) {
    companion object { const val DEFAULT_SESSION_LENGTH = 4 }
}

class SettingsRepository(private val context: Context) {
    private val enabledKey = booleanPreferencesKey("reminder_enabled")
    private val hourKey = intPreferencesKey("reminder_hour")
    private val minuteKey = intPreferencesKey("reminder_minute")

    private val onboardedKey = booleanPreferencesKey("onboarded")
    private val soundKey = booleanPreferencesKey("sound")
    private val hapticsKey = booleanPreferencesKey("haptics")
    private val moreSoundsKey = booleanPreferencesKey("more_sounds")
    private val musicKey = booleanPreferencesKey("ambient_music")
    private val themeKey = stringPreferencesKey("theme")
    private val sessionModeKey = stringPreferencesKey("session_mode")
    private val sessionListKey = longPreferencesKey("session_list_id")
    private val sessionLengthKey = intPreferencesKey("session_length")
    private val focusSkillsKey = stringSetPreferencesKey("focus_skills")
    private val reducedMotionKey = booleanPreferencesKey("reduced_motion")
    private val colorBlindKey = booleanPreferencesKey("color_blind_palette")

    val prefs: Flow<AppPrefs> = context.dataStore.data.map {
        AppPrefs(
            onboarded = it[onboardedKey] ?: false,
            sound = it[soundKey] ?: true,
            haptics = it[hapticsKey] ?: true,
            moreSounds = it[moreSoundsKey] ?: false,
            music = it[musicKey] ?: false,
            theme = it[themeKey]?.let { name -> ThemeMode.entries.firstOrNull { m -> m.name == name } } ?: ThemeMode.DARK,
            sessionMode = it[sessionModeKey]?.let { n -> SessionMode.entries.firstOrNull { m -> m.name == n } } ?: SessionMode.RANDOM,
            sessionListId = it[sessionListKey] ?: -1,
            sessionLength = (it[sessionLengthKey] ?: AppPrefs.DEFAULT_SESSION_LENGTH).coerceIn(SessionPlanner.MIN_GAMES, SessionPlanner.MAX_GAMES),
            reducedMotion = it[reducedMotionKey] ?: false,
            colorBlindPalette = it[colorBlindKey] ?: false,
            focusSkills = it[focusSkillsKey].orEmpty().mapNotNull { n -> Skill.entries.firstOrNull { s -> s.name == n } }.toSet(),
        )
    }

    suspend fun setOnboarded() { context.dataStore.edit { it[onboardedKey] = true } }
    suspend fun setSound(on: Boolean) { context.dataStore.edit { it[soundKey] = on } }
    suspend fun setHaptics(on: Boolean) { context.dataStore.edit { it[hapticsKey] = on } }
    suspend fun setMoreSounds(on: Boolean) { context.dataStore.edit { it[moreSoundsKey] = on } }
    suspend fun setMusic(on: Boolean) { context.dataStore.edit { it[musicKey] = on } }
    suspend fun setTheme(mode: ThemeMode) { context.dataStore.edit { it[themeKey] = mode.name } }

    suspend fun setReducedMotion(on: Boolean) { context.dataStore.edit { it[reducedMotionKey] = on } }
    suspend fun setColorBlindPalette(on: Boolean) { context.dataStore.edit { it[colorBlindKey] = on } }

    suspend fun setSessionLength(count: Int) {
        context.dataStore.edit { it[sessionLengthKey] = count.coerceIn(SessionPlanner.MIN_GAMES, SessionPlanner.MAX_GAMES) }
    }

    suspend fun setFocusSkills(skills: Set<Skill>) {
        context.dataStore.edit { it[focusSkillsKey] = skills.map { s -> s.name }.toSet() }
    }

    suspend fun setSessionMode(mode: SessionMode, listId: Long = -1) {
        context.dataStore.edit { it[sessionModeKey] = mode.name; it[sessionListKey] = listId }
    }

    /** Restores every preference to its default and cancels the reminder. Keeps the onboarded flag. */
    suspend fun resetSettings() {
        context.dataStore.edit {
            val onboarded = it[onboardedKey]
            it.clear()
            if (onboarded != null) it[onboardedKey] = onboarded
        }
        ReminderScheduler.apply(context, ReminderSettings())
    }

    /** Applies imported settings. A specific list can't be restored (ids change), so that mode falls back to random. */
    suspend fun applyBackup(b: app.trainer.data.backup.BackupSettings) {
        context.dataStore.edit {
            it[soundKey] = b.sound
            it[hapticsKey] = b.haptics
            it[moreSoundsKey] = b.moreSounds
            it[musicKey] = b.music
            it[themeKey] = b.theme.name
            it[sessionModeKey] = (if (b.sessionMode == SessionMode.LIST) SessionMode.RANDOM else b.sessionMode).name
            it[sessionListKey] = -1
            it[sessionLengthKey] = b.sessionLength
            it[reducedMotionKey] = b.reducedMotion
            it[colorBlindKey] = b.colorBlindPalette
            it[focusSkillsKey] = b.focusSkills.map { s -> s.name }.toSet()
            it[enabledKey] = b.reminderEnabled
            it[hourKey] = b.reminderHour
            it[minuteKey] = b.reminderMinute
        }
        ReminderScheduler.apply(context, ReminderSettings(b.reminderEnabled, b.reminderHour, b.reminderMinute))
    }

    val reminder: Flow<ReminderSettings> = context.dataStore.data.map {
        ReminderSettings(it[enabledKey] ?: false, it[hourKey] ?: 20, it[minuteKey] ?: 0)
    }

    /** Persists the reminder and (re)schedules or cancels it. */
    suspend fun setReminder(settings: ReminderSettings) {
        context.dataStore.edit {
            it[enabledKey] = settings.enabled
            it[hourKey] = settings.hour
            it[minuteKey] = settings.minute
        }
        ReminderScheduler.apply(context, settings)
    }
}
