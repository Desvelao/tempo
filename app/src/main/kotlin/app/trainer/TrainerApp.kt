package app.trainer

import android.app.Application
import androidx.room.withTransaction
import app.trainer.data.AppDatabase
import app.trainer.data.GameListRepository
import app.trainer.data.ResultRepository
import app.trainer.core.SessionMode
import app.trainer.settings.SettingsRepository
import kotlinx.coroutines.flow.first

class TrainerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        audio // start observing the sound preferences
    }

    val database by lazy { AppDatabase.create(this) }
    val results by lazy {
        ResultRepository(database.gameResultDao(), database.skillRatingDao(), database.sessionDao())
    }
    val gameLists by lazy { GameListRepository(database.gameListDao()) }
    val settings by lazy { SettingsRepository(this) }
    val audio by lazy { app.trainer.audio.AudioController(this, settings.prefs) }
    val backup by lazy { app.trainer.data.backup.BackupRepository(database, settings) }

    /** Permanently deletes the chosen data. */
    suspend fun clearData(scope: ClearScope) {
        if (scope == ClearScope.SETTINGS || scope == ClearScope.ALL) settings.resetSettings()
        if (scope == ClearScope.PROGRESS || scope == ClearScope.ALL) database.withTransaction { results.clearProgress() }
        if (scope == ClearScope.LISTS || scope == ClearScope.ALL) {
            gameLists.clear()
            // A list-based session mode would now point at nothing.
            if (settings.prefs.first().sessionMode != SessionMode.RANDOM) settings.setSessionMode(SessionMode.RANDOM)
        }
    }
}

enum class ClearScope { SETTINGS, LISTS, PROGRESS, ALL }
