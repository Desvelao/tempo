package app.trainer.data.backup

import app.trainer.settings.AppPrefs
import app.trainer.core.SessionPlanner
import app.trainer.core.SessionMode
import app.trainer.core.Skill
import app.trainer.settings.ThemeMode
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/** Which part of the data an export or import covers. */
enum class BackupScope {
    SETTINGS, LISTS, PROGRESS, ALL;

    fun includes(part: BackupScope) = this == ALL || this == part
}

/** Why a file was rejected. The UI maps each reason to a localised message. */
enum class BackupError { NOT_A_BACKUP, NEWER_VERSION, DAMAGED, UNKNOWN_VALUES }

class BackupException(val error: BackupError) : Exception(error.name)

data class BackupSettings(
    val sound: Boolean, val haptics: Boolean, val theme: ThemeMode, val sessionMode: SessionMode,
    val reminderEnabled: Boolean, val reminderHour: Int, val reminderMinute: Int,
    // Added after version 1 files already existed, so files without them still import.
    val sessionLength: Int = AppPrefs.DEFAULT_SESSION_LENGTH, val focusSkills: Set<Skill> = emptySet(),
    val reducedMotion: Boolean = false, val colorBlindPalette: Boolean = false,
    val moreSounds: Boolean = false, val music: Boolean = false,
)

data class BackupList(val name: String, val gameIds: List<String>, val shuffle: Boolean, val createdAt: Long)
data class BackupSession(val id: Long, val startedAt: Long, val finishedAt: Long?)
data class BackupResult(
    val sessionId: Long?, val gameId: String, val skill: Skill, val score: Int, val accuracy: Double,
    val avgReactionMs: Long, val difficulty: Int, val playedAt: Long,
)
data class BackupRating(val skill: Skill, val level: Int, val updatedAt: Long)
data class BackupProgress(val sessions: List<BackupSession>, val results: List<BackupResult>, val ratings: List<BackupRating>)

/** A parsed backup. A part that is null is not contained in the file. */
data class BackupData(
    val exportedAt: Long,
    val settings: BackupSettings? = null,
    val lists: List<BackupList>? = null,
    val progress: BackupProgress? = null,
) {
    fun has(scope: BackupScope) = when (scope) {
        BackupScope.SETTINGS -> settings != null
        BackupScope.LISTS -> lists != null
        BackupScope.PROGRESS -> progress != null
        BackupScope.ALL -> settings != null || lists != null || progress != null
    }
}

/** JSON (de)serialisation of a backup file. Pure: no Android classes except the bundled `org.json`. */
object BackupFormat {
    const val FORMAT = "trainer-backup"
    const val VERSION = 1

    fun encode(data: BackupData): String {
        val root = JSONObject().put("format", FORMAT).put("version", VERSION).put("exportedAt", data.exportedAt)
        data.settings?.let {
            root.put(
                "settings",
                JSONObject().put("sound", it.sound).put("haptics", it.haptics).put("theme", it.theme.name)
                    .put("sessionMode", it.sessionMode.name).put("reminderEnabled", it.reminderEnabled)
                    .put("reminderHour", it.reminderHour).put("reminderMinute", it.reminderMinute)
                    .put("sessionLength", it.sessionLength).put("focusSkills", JSONArray(it.focusSkills.map { s -> s.name }))
                    .put("reducedMotion", it.reducedMotion).put("colorBlindPalette", it.colorBlindPalette)
                    .put("moreSounds", it.moreSounds).put("music", it.music),
            )
        }
        data.lists?.let { lists ->
            root.put(
                "lists",
                JSONArray(
                    lists.map {
                        JSONObject().put("name", it.name).put("gameIds", JSONArray(it.gameIds))
                            .put("shuffle", it.shuffle).put("createdAt", it.createdAt)
                    },
                ),
            )
        }
        data.progress?.let { p ->
            root.put(
                "progress",
                JSONObject()
                    .put("sessions", JSONArray(p.sessions.map {
                        JSONObject().put("id", it.id).put("startedAt", it.startedAt).put("finishedAt", it.finishedAt ?: JSONObject.NULL)
                    }))
                    .put("results", JSONArray(p.results.map {
                        JSONObject().put("sessionId", it.sessionId ?: JSONObject.NULL).put("gameId", it.gameId)
                            .put("skill", it.skill.name).put("score", it.score).put("accuracy", it.accuracy)
                            .put("avgReactionMs", it.avgReactionMs).put("difficulty", it.difficulty).put("playedAt", it.playedAt)
                    }))
                    .put("ratings", JSONArray(p.ratings.map {
                        JSONObject().put("skill", it.skill.name).put("level", it.level).put("updatedAt", it.updatedAt)
                    })),
            )
        }
        return root.toString(2)
    }

    /** Parses and validates the whole file; throws [BackupException] before anything is applied. */
    fun decode(text: String): BackupData = try {
        val root = JSONObject(text)
        if (root.optString("format") != FORMAT) throw BackupException(BackupError.NOT_A_BACKUP)
        if (root.optInt("version", 0) !in 1..VERSION) throw BackupException(BackupError.NEWER_VERSION)
        BackupData(
            exportedAt = root.optLong("exportedAt"),
            settings = root.optJSONObject("settings")?.let(::settings),
            lists = root.optJSONArray("lists")?.let(::lists),
            progress = root.optJSONObject("progress")?.let(::progress),
        )
    } catch (e: JSONException) {
        throw BackupException(BackupError.DAMAGED)
    } catch (e: IllegalArgumentException) {
        throw BackupException(BackupError.UNKNOWN_VALUES)
    }

    private fun settings(o: JSONObject) = BackupSettings(
        sound = o.getBoolean("sound"), haptics = o.getBoolean("haptics"),
        theme = ThemeMode.valueOf(o.getString("theme")), sessionMode = SessionMode.valueOf(o.getString("sessionMode")),
        reminderEnabled = o.getBoolean("reminderEnabled"),
        reminderHour = o.getInt("reminderHour").also { require(it in 0..23) },
        reminderMinute = o.getInt("reminderMinute").also { require(it in 0..59) },
        sessionLength = o.optInt("sessionLength", AppPrefs.DEFAULT_SESSION_LENGTH)
            .also { require(it in SessionPlanner.MIN_GAMES..SessionPlanner.MAX_GAMES) },
        focusSkills = o.optJSONArray("focusSkills")?.let { a -> (0 until a.length()).map { Skill.valueOf(a.getString(it)) }.toSet() }.orEmpty(),
        reducedMotion = o.optBoolean("reducedMotion", false),
        colorBlindPalette = o.optBoolean("colorBlindPalette", false),
        moreSounds = o.optBoolean("moreSounds", false),
        music = o.optBoolean("music", false),
    )

    private fun lists(a: JSONArray) = (0 until a.length()).map {
        val o = a.getJSONObject(it)
        val ids = o.getJSONArray("gameIds")
        BackupList(o.getString("name"), (0 until ids.length()).map(ids::getString), o.getBoolean("shuffle"), o.getLong("createdAt"))
    }

    private fun progress(o: JSONObject): BackupProgress {
        fun <T> JSONArray.map(f: (JSONObject) -> T) = (0 until length()).map { f(getJSONObject(it)) }
        return BackupProgress(
            sessions = o.getJSONArray("sessions").map {
                BackupSession(it.getLong("id"), it.getLong("startedAt"), if (it.isNull("finishedAt")) null else it.getLong("finishedAt"))
            },
            results = o.getJSONArray("results").map {
                BackupResult(
                    if (it.isNull("sessionId")) null else it.getLong("sessionId"), it.getString("gameId"),
                    Skill.valueOf(it.getString("skill")), it.getInt("score"), it.getDouble("accuracy"),
                    it.getLong("avgReactionMs"), it.getInt("difficulty"), it.getLong("playedAt"),
                )
            },
            ratings = o.getJSONArray("ratings").map {
                BackupRating(Skill.valueOf(it.getString("skill")), it.getInt("level"), it.getLong("updatedAt"))
            },
        )
    }
}
