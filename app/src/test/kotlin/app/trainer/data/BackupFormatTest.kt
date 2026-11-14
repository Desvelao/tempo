package app.trainer.data

import app.trainer.core.SessionMode
import app.trainer.core.Skill
import app.trainer.data.backup.BackupData
import app.trainer.data.backup.BackupException
import app.trainer.data.backup.BackupFormat
import app.trainer.data.backup.BackupList
import app.trainer.data.backup.BackupProgress
import app.trainer.data.backup.BackupRating
import app.trainer.data.backup.BackupResult
import app.trainer.data.backup.BackupScope
import app.trainer.data.backup.BackupSession
import app.trainer.data.backup.BackupSettings
import app.trainer.settings.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class BackupFormatTest {
    private val settings = BackupSettings(false, true, ThemeMode.LIGHT, SessionMode.ANY_LIST, true, 7, 30)
    private val lists = listOf(BackupList("Morning", listOf("stroop", "n_back"), true, 11L), BackupList("Empty", emptyList(), false, 12L))
    private val progress = BackupProgress(
        sessions = listOf(BackupSession(1, 100, 200), BackupSession(2, 300, null)),
        results = listOf(
            BackupResult(1, "stroop", Skill.ATTENTION, 420, 0.8, 910, 5, 150),
            BackupResult(null, "future_game", Skill.SPEED, 10, 0.5, 0, 1, 160),
        ),
        ratings = listOf(BackupRating(Skill.ATTENTION, 6, 150)),
    )

    private fun roundTrip(d: BackupData) = BackupFormat.decode(BackupFormat.encode(d))

    @Test fun everythingRoundTrips() {
        val all = BackupData(5L, settings, lists, progress)
        assertEquals(all, roundTrip(all))
    }

    @Test fun eachPartRoundTripsAlone() {
        val s = roundTrip(BackupData(1L, settings = settings))
        assertEquals(settings, s.settings); assertNull(s.lists); assertNull(s.progress)
        assertTrue(s.has(BackupScope.SETTINGS)); assertFalse(s.has(BackupScope.LISTS)); assertFalse(s.has(BackupScope.PROGRESS))
        assertEquals(lists, roundTrip(BackupData(1L, lists = lists)).lists)
        assertEquals(progress, roundTrip(BackupData(1L, progress = progress)).progress)
    }

    @Test fun emptyPartsRoundTrip() {
        val d = BackupData(1L, lists = emptyList(), progress = BackupProgress(emptyList(), emptyList(), emptyList()))
        assertEquals(d, roundTrip(d))
        assertTrue(roundTrip(d).has(BackupScope.LISTS))
    }

    @Test fun sessionLengthAndFocusSkillsRoundTrip() {
        val custom = settings.copy(sessionMode = SessionMode.ADAPTIVE, sessionLength = 5, focusSkills = setOf(Skill.SPEED, Skill.MEMORY))
        assertEquals(custom, roundTrip(BackupData(1L, settings = custom)).settings)
    }

    @Test fun accessibilitySettingsRoundTrip() {
        val custom = settings.copy(reducedMotion = true, colorBlindPalette = true)
        assertEquals(custom, roundTrip(BackupData(1L, settings = custom)).settings)
    }

    @Test fun oldSettingsWithoutNewFieldsStillImportWithDefaults() {
        val old = """{"format":"trainer-backup","version":1,"exportedAt":1,"settings":{"sound":true,"haptics":false,
            "theme":"DARK","sessionMode":"RANDOM","reminderEnabled":false,"reminderHour":20,"reminderMinute":0}}"""
        val s = BackupFormat.decode(old).settings!!
        assertEquals(4, s.sessionLength)
        assertEquals(emptySet<Skill>(), s.focusSkills)
        assertFalse(s.reducedMotion)
        assertFalse(s.colorBlindPalette)
        assertFalse(s.moreSounds)
        assertFalse(s.music)
    }

    @Test fun soundOptionsRoundTrip() {
        val custom = settings.copy(moreSounds = true, music = true)
        assertEquals(custom, roundTrip(BackupData(1L, settings = custom)).settings)
    }

    @Test fun rejectsOutOfRangeSessionLength() {
        val bad = """{"format":"trainer-backup","version":1,"exportedAt":1,"settings":{"sound":true,"haptics":false,
            "theme":"DARK","sessionMode":"RANDOM","reminderEnabled":false,"reminderHour":20,"reminderMinute":0,"sessionLength":9}}"""
        assertRejected(bad)
    }

    private fun assertRejected(text: String) {
        try { BackupFormat.decode(text); fail("should be rejected: $text") } catch (_: BackupException) {}
    }

    @Test fun rejectsForeignDamagedAndNewerFiles() {
        assertRejected("not json")
        assertRejected("{}")
        assertRejected("""{"format":"other","version":1}""")
        assertRejected("""{"format":"trainer-backup","version":99}""")
        assertRejected("""{"format":"trainer-backup","version":0}""")
    }

    @Test fun rejectsUnknownValues() {
        val good = BackupFormat.encode(BackupData(1L, settings, lists, progress))
        assertRejected(good.replace("ATTENTION", "TELEPATHY"))
        assertRejected(good.replace("LIGHT", "NEON"))
        assertRejected(good.replace("ANY_LIST", "SOMETIMES"))
        assertRejected(good.replace("\"reminderHour\": 7", "\"reminderHour\": 77"))
    }
}
