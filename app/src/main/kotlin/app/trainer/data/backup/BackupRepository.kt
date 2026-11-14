package app.trainer.data.backup

import androidx.room.withTransaction
import app.trainer.core.SessionMode
import app.trainer.data.AppDatabase
import app.trainer.data.GameListEntity
import app.trainer.data.GameResultEntity
import app.trainer.data.SessionEntity
import app.trainer.data.SkillRatingEntity
import app.trainer.settings.SettingsRepository
import kotlinx.coroutines.flow.first

/** Reads the app's data into a [BackupData] and writes a parsed backup back, replacing the chosen parts. */
class BackupRepository(private val db: AppDatabase, private val settings: SettingsRepository) {

    suspend fun export(scope: BackupScope, now: Long = System.currentTimeMillis()): String {
        val data = BackupData(
            exportedAt = now,
            settings = if (scope.includes(BackupScope.SETTINGS)) {
                val p = settings.prefs.first(); val r = settings.reminder.first()
                BackupSettings(p.sound, p.haptics, p.theme, p.sessionMode, r.enabled, r.hour, r.minute, p.sessionLength, p.focusSkills, p.reducedMotion, p.colorBlindPalette, p.moreSounds, p.music)
            } else null,
            lists = if (scope.includes(BackupScope.LISTS)) {
                db.gameListDao().all().map { BackupList(it.name, it.gameIds.split(',').filter(String::isNotEmpty), it.shuffle, it.createdAt) }
            } else null,
            progress = if (scope.includes(BackupScope.PROGRESS)) {
                BackupProgress(
                    sessions = db.sessionDao().all().map { BackupSession(it.id, it.startedAt, it.finishedAt) },
                    results = db.gameResultDao().all().map {
                        BackupResult(it.sessionId, it.gameId, it.skill, it.score, it.accuracy, it.avgReactionMs, it.difficulty, it.playedAt)
                    },
                    ratings = db.skillRatingDao().all().map { BackupRating(it.skill, it.level, it.updatedAt) },
                )
            } else null,
        )
        return BackupFormat.encode(data)
    }

    /** Replaces the parts of [scope] with those in [data]. The caller checks [BackupData.has] first. */
    suspend fun import(data: BackupData, scope: BackupScope) {
        val lists = data.lists.takeIf { scope.includes(BackupScope.LISTS) }
        val progress = data.progress.takeIf { scope.includes(BackupScope.PROGRESS) }
        db.withTransaction {
            if (lists != null) {
                db.gameListDao().clear()
                db.gameListDao().insertAll(lists.map { GameListEntity(0, it.name, it.gameIds.joinToString(","), it.shuffle, it.createdAt) })
            }
            if (progress != null) {
                db.gameResultDao().clear(); db.sessionDao().clear(); db.skillRatingDao().clear()
                db.sessionDao().insertAll(progress.sessions.map { SessionEntity(it.id, it.startedAt, it.finishedAt) })
                db.gameResultDao().insertAll(progress.results.map {
                    GameResultEntity(0, it.sessionId, it.gameId, it.skill, it.score, it.accuracy, it.avgReactionMs, it.difficulty, it.playedAt)
                })
                db.skillRatingDao().insertAll(progress.ratings.map { SkillRatingEntity(it.skill, it.level, it.updatedAt) })
            }
        }
        val s = data.settings.takeIf { scope.includes(BackupScope.SETTINGS) }
        if (s != null) settings.applyBackup(s)
        // A saved list id would now point at a different (or no) list.
        else if (lists != null && settings.prefs.first().sessionMode == SessionMode.LIST) settings.setSessionMode(SessionMode.RANDOM)
    }
}
