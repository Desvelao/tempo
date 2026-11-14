package app.trainer.ui

import app.trainer.core.Skill
import app.trainer.core.SessionPlanner
import android.Manifest
import app.trainer.audio.Effect
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import app.trainer.BuildConfig
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import android.widget.Toast
import app.trainer.ClearScope
import app.trainer.R
import app.trainer.data.backup.BackupData
import app.trainer.data.backup.BackupError
import app.trainer.data.backup.BackupException
import app.trainer.data.backup.BackupFormat
import app.trainer.data.backup.BackupScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.trainer.TrainerApp
import app.trainer.core.SessionMode
import app.trainer.settings.AppPrefs
import app.trainer.settings.ReminderSettings
import app.trainer.settings.ThemeMode
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as TrainerApp
    val scope = rememberCoroutineScope()
    val colors = MaterialTheme.colorScheme
    val reminder by app.settings.reminder.collectAsStateWithLifecycle(initialValue = ReminderSettings())
    val prefs by app.settings.prefs.collectAsStateWithLifecycle(initialValue = AppPrefs())

    var chooser by rememberSaveable { mutableStateOf<BackupMode?>(null) }
    var exportScope by rememberSaveable { mutableStateOf(BackupScope.ALL) }
    var importScope by rememberSaveable { mutableStateOf(BackupScope.ALL) }
    var pendingImport by remember { mutableStateOf<BackupData?>(null) }
    fun toast(@StringRes text: Int) = Toast.makeText(context, text, Toast.LENGTH_LONG).show()
    val scopeLabels = BackupScope.entries.associateWith { stringResource(it.labelRes()) }
    val noDataMessages = BackupScope.entries.associateWith { stringResource(R.string.backup_no_data, scopeLabels.getValue(it)) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            try {
                val text = app.backup.export(exportScope)
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(text.toByteArray()) }
                        ?: error("Cannot open the file")
                }
                toast(R.string.backup_exported)
            } catch (e: Exception) {
                toast(R.string.backup_export_failed)
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            try {
                val text = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) } ?: error("Cannot open the file")
                }
                val data = BackupFormat.decode(text)
                if (data.has(importScope)) pendingImport = data
                else Toast.makeText(context, noDataMessages.getValue(importScope), Toast.LENGTH_LONG).show()
            } catch (e: BackupException) {
                toast(e.error.messageRes())
            } catch (e: Exception) {
                toast(R.string.backup_unreadable)
            }
        }
    }

    var showAbout by rememberSaveable { mutableStateOf(false) }
    var pendingClear by rememberSaveable { mutableStateOf<ClearScope?>(null) }

    fun save(settings: ReminderSettings) = scope.launch { app.settings.setReminder(settings) }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        // Without the permission the reminder could never be shown, so keep it off.
        if (granted) save(reminder.copy(enabled = true))
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            IconAction(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back), onBack)
            Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
        }

        SectionLabel(stringResource(R.string.settings_section_reminder))
        Panel {
            SettingRow(
                title = stringResource(R.string.settings_reminder_title), subtitle = stringResource(R.string.settings_reminder_subtitle),
                checked = reminder.enabled,
            ) { on ->
                val needsPermission = on && Build.VERSION.SDK_INT >= 33 &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                if (needsPermission) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                else save(reminder.copy(enabled = on))
            }
            HorizontalDivider(color = colors.outlineVariant)
            Surface(
                onClick = {
                    TimePickerDialog(
                        context,
                        { _, h, m -> save(reminder.copy(hour = h, minute = m)) },
                        reminder.hour, reminder.minute, true,
                    ).show()
                },
                color = colors.surfaceContainer,
            ) {
                Row(Modifier.fillMaxWidth().heightIn(min = 64.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.settings_reminder_time), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                    Text("%02d:%02d".format(reminder.hour, reminder.minute), style = MaterialTheme.typography.titleLarge, color = colors.primary)
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = colors.onSurfaceVariant)
                }
            }
        }

        SectionLabel(stringResource(R.string.settings_section_session))
        val savedLists by app.gameLists.lists.collectAsStateWithLifecycle(initialValue = emptyList())
        val hasLists = savedLists.any { it.gameIds.isNotEmpty() }
        Panel {
            ModeRow(stringResource(R.string.settings_mode_random), stringResource(R.string.settings_mode_random_subtitle), prefs.sessionMode == SessionMode.RANDOM, true) {
                scope.launch { app.settings.setSessionMode(SessionMode.RANDOM) }
            }
            HorizontalDivider(color = colors.outlineVariant)
            ModeRow(stringResource(R.string.settings_mode_adaptive), stringResource(R.string.settings_mode_adaptive_subtitle), prefs.sessionMode == SessionMode.ADAPTIVE, true) {
                scope.launch { app.settings.setSessionMode(SessionMode.ADAPTIVE) }
            }
            HorizontalDivider(color = colors.outlineVariant)
            ModeRow(stringResource(R.string.settings_mode_any_list), if (hasLists) stringResource(R.string.settings_mode_any_list_subtitle) else stringResource(R.string.settings_create_list_first), prefs.sessionMode == SessionMode.ANY_LIST, hasLists) {
                scope.launch { app.settings.setSessionMode(SessionMode.ANY_LIST) }
            }
            HorizontalDivider(color = colors.outlineVariant)
            ModeRow(stringResource(R.string.settings_mode_one_list), if (hasLists) stringResource(R.string.settings_mode_one_list_subtitle) else stringResource(R.string.settings_create_list_first), prefs.sessionMode == SessionMode.LIST, hasLists) {
                scope.launch { app.settings.setSessionMode(SessionMode.LIST, prefs.sessionListId.takeIf { id -> savedLists.any { it.id == id } } ?: savedLists.first { it.gameIds.isNotEmpty() }.id) }
            }
        }
        if (prefs.sessionMode == SessionMode.RANDOM || prefs.sessionMode == SessionMode.ADAPTIVE) {
            SectionLabel(stringResource(R.string.settings_session_length))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (count in SessionPlanner.MIN_GAMES..SessionPlanner.MAX_GAMES) {
                    ChoiceChip(
                        text = stringResource(R.string.settings_session_length_option, count),
                        selected = prefs.sessionLength == count,
                        modifier = Modifier.weight(1f),
                    ) { scope.launch { app.settings.setSessionLength(count) } }
                }
            }
        }
        if (prefs.sessionMode == SessionMode.ADAPTIVE) {
            SectionLabel(stringResource(R.string.settings_focus_skills))
            Text(stringResource(R.string.settings_focus_skills_hint), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, modifier = Modifier.padding(bottom = 8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Skill.entries.forEach { skill ->
                    val on = skill in prefs.focusSkills
                    ChoiceChip(text = skill.label(), selected = on, modifier = Modifier.fillMaxWidth(), role = Role.Checkbox) {
                        scope.launch { app.settings.setFocusSkills(if (on) prefs.focusSkills - skill else prefs.focusSkills + skill) }
                    }
                }
            }
        }
        if (prefs.sessionMode == SessionMode.LIST && hasLists) {
            Column(Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                savedLists.filter { it.gameIds.isNotEmpty() }.forEach { list ->
                    val selected = list.id == prefs.sessionListId
                    Surface(
                        onClick = { scope.launch { app.settings.setSessionMode(SessionMode.LIST, list.id) } },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = if (selected) colors.secondaryContainer else colors.background,
                        border = BorderStroke(1.5.dp, if (selected) colors.primary else colors.outlineVariant),
                    ) {
                        Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(list.name, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                            if (selected) Text(stringResource(R.string.settings_selected), style = MaterialTheme.typography.labelLarge, color = colors.primary)
                        }
                    }
                }
            }
        }

        SectionLabel(stringResource(R.string.settings_section_feedback))
        Panel {
            SettingRow(stringResource(R.string.settings_sound), null, prefs.sound) { on -> scope.launch { app.settings.setSound(on) } }
            HorizontalDivider(color = colors.outlineVariant)
            SettingRow(stringResource(R.string.settings_more_sounds), stringResource(R.string.settings_more_sounds_subtitle), prefs.moreSounds) { on -> scope.launch { app.settings.setMoreSounds(on) }; if (on) app.audio.preview(Effect.DONE) }
            HorizontalDivider(color = colors.outlineVariant)
            SettingRow(stringResource(R.string.settings_music), stringResource(R.string.settings_music_subtitle), prefs.music) { on -> scope.launch { app.settings.setMusic(on) } }
            HorizontalDivider(color = colors.outlineVariant)
            SettingRow(stringResource(R.string.settings_haptics), null, prefs.haptics) { on -> scope.launch { app.settings.setHaptics(on) } }
        }

        SectionLabel(stringResource(R.string.settings_section_accessibility))
        Panel {
            SettingRow(stringResource(R.string.settings_reduced_motion), stringResource(R.string.settings_reduced_motion_subtitle), prefs.reducedMotion) { on ->
                scope.launch { app.settings.setReducedMotion(on) }
            }
            HorizontalDivider(color = colors.outlineVariant)
            SettingRow(stringResource(R.string.settings_color_blind), stringResource(R.string.settings_color_blind_subtitle), prefs.colorBlindPalette) { on ->
                scope.launch { app.settings.setColorBlindPalette(on) }
            }
        }

        SectionLabel(stringResource(R.string.settings_section_theme))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemeMode.entries.forEach { mode ->
                val selected = prefs.theme == mode
                Surface(
                    onClick = { scope.launch { app.settings.setTheme(mode) } },
                    modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = if (selected) colors.secondaryContainer else colors.background,
                    border = BorderStroke(1.5.dp, if (selected) colors.primary else colors.outlineVariant),
                ) {
                    androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                        Text(
                            stringResource(when (mode) { ThemeMode.SYSTEM -> R.string.theme_system; ThemeMode.LIGHT -> R.string.theme_light; ThemeMode.DARK -> R.string.theme_dark }),
                            style = MaterialTheme.typography.titleSmall,
                            color = if (selected) colors.primary else colors.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        SectionLabel(stringResource(R.string.settings_section_backup))
        Panel {
            ClearRow(stringResource(R.string.settings_export), stringResource(R.string.settings_export_subtitle)) { chooser = BackupMode.EXPORT }
            HorizontalDivider(color = colors.outlineVariant)
            ClearRow(stringResource(R.string.settings_import), stringResource(R.string.settings_import_subtitle)) { chooser = BackupMode.IMPORT }
        }

        SectionLabel(stringResource(R.string.settings_section_data))
        Panel {
            ClearRow(stringResource(R.string.settings_reset), stringResource(R.string.settings_reset_subtitle)) { pendingClear = ClearScope.SETTINGS }
            HorizontalDivider(color = colors.outlineVariant)
            ClearRow(stringResource(R.string.settings_delete_lists), stringResource(R.string.settings_delete_lists_subtitle)) { pendingClear = ClearScope.LISTS }
            HorizontalDivider(color = colors.outlineVariant)
            ClearRow(stringResource(R.string.settings_delete_progress), stringResource(R.string.settings_delete_progress_subtitle)) { pendingClear = ClearScope.PROGRESS }
            HorizontalDivider(color = colors.outlineVariant)
            ClearRow(stringResource(R.string.settings_delete_all), stringResource(R.string.settings_delete_all_subtitle), danger = true) { pendingClear = ClearScope.ALL }
        }

        chooser?.let { mode ->
            AlertDialog(
                onDismissRequest = { chooser = null },
                title = { Text(stringResource(if (mode == BackupMode.EXPORT) R.string.backup_export_which else R.string.backup_import_which)) },
                text = {
                    Column {
                        BackupScope.entries.forEach { option ->
                            TextButton(
                                onClick = {
                                    chooser = null
                                    if (mode == BackupMode.EXPORT) {
                                        exportScope = option
                                        exportLauncher.launch("trainer-${option.name.lowercase()}-${LocalDate.now()}.json")
                                    } else {
                                        importScope = option
                                        importLauncher.launch(arrayOf("application/json", "*/*"))
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                            ) { Text(scopeLabels.getValue(option), Modifier.fillMaxWidth(), style = MaterialTheme.typography.titleMedium) }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = { TextButton(onClick = { chooser = null }) { Text(stringResource(R.string.action_cancel)) } },
            )
        }

        pendingImport?.let { data ->
            val parts = buildList {
                if (importScope.includes(BackupScope.SETTINGS) && data.settings != null) add(stringResource(R.string.backup_part_settings))
                if (importScope.includes(BackupScope.LISTS) && data.lists != null) add(pluralStringResource(R.plurals.backup_part_lists, data.lists!!.size, data.lists!!.size))
                if (importScope.includes(BackupScope.PROGRESS) && data.progress != null) add(pluralStringResource(R.plurals.backup_part_progress, data.progress!!.results.size, data.progress!!.results.size))
            }
            AlertDialog(
                onDismissRequest = { pendingImport = null },
                title = { Text(stringResource(R.string.backup_replace_title)) },
                text = { Text(stringResource(R.string.backup_replace_body, parts.joinToString(", "))) },
                confirmButton = {
                    TextButton(onClick = {
                        pendingImport = null
                        scope.launch {
                            try {
                                app.backup.import(data, importScope)
                                toast(R.string.backup_imported)
                            } catch (e: Exception) {
                                toast(R.string.backup_import_failed)
                            }
                        }
                    }) { Text(stringResource(R.string.backup_replace_action), color = colors.error) }
                },
                dismissButton = { TextButton(onClick = { pendingImport = null }) { Text(stringResource(R.string.action_cancel)) } },
            )
        }

        pendingClear?.let { target ->
            AlertDialog(
                onDismissRequest = { pendingClear = null },
                title = { Text(clearTitle(target)) },
                text = { Text(stringResource(R.string.clear_confirm_body, clearBody(target))) },
                confirmButton = {
                    TextButton(onClick = {
                        pendingClear = null
                        scope.launch {
                            app.clearData(target)
                            Toast.makeText(context, R.string.clear_done, Toast.LENGTH_SHORT).show()
                        }
                    }) { Text(stringResource(R.string.clear_delete_action), color = colors.error) }
                },
                dismissButton = { TextButton(onClick = { pendingClear = null }) { Text(stringResource(R.string.action_cancel)) } },
            )
        }

        Surface(
            modifier = Modifier.fillMaxWidth().padding(top = 28.dp),
            shape = RoundedCornerShape(18.dp), color = colors.background,
            border = BorderStroke(1.dp, colors.outlineVariant),
        ) {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(AppIcons.Lock, contentDescription = null, tint = colors.primary)
                Text(stringResource(R.string.settings_offline_note), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            }
        }

        val versionLabel = stringResource(R.string.settings_version, stringResource(R.string.app_name), BuildConfig.VERSION_NAME)
        TextButton(onClick = { showAbout = true }, modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 12.dp).heightIn(min = 48.dp)) {
            Text(versionLabel, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        }
        if (showAbout) {
            val clipboard = LocalClipboardManager.current
            AlertDialog(
                onDismissRequest = { showAbout = false },
                title = { Text(stringResource(R.string.about_title)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.about_version, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE), style = MaterialTheme.typography.bodyMedium)
                        Text(stringResource(R.string.settings_offline_note), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                        Text(BuildConfig.APP_URL, style = MaterialTheme.typography.bodyMedium, color = colors.primary)
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        showAbout = false
                        try {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(BuildConfig.APP_URL)))
                        } catch (e: ActivityNotFoundException) {
                            toast(R.string.about_no_browser)
                        }
                    }) { Text(stringResource(R.string.about_open)) }
                },
                dismissButton = {
                    Row {
                        TextButton(onClick = { clipboard.setText(AnnotatedString(BuildConfig.APP_URL)); showAbout = false }) { Text(stringResource(R.string.about_copy)) }
                        TextButton(onClick = { showAbout = false }) { Text(stringResource(R.string.action_cancel)) }
                    }
                },
            )
        }
    }
}

@Composable
private fun ModeRow(title: String, subtitle: String, selected: Boolean, enabled: Boolean, onSelect: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp)
            .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onSelect)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = if (enabled) Color.Unspecified else MaterialTheme.colorScheme.onSurfaceVariant)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        RadioButton(selected = selected, onClick = null, enabled = enabled)
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 28.dp, bottom = 10.dp),
    )
}

@Composable
private fun Panel(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) { Column(Modifier.padding(horizontal = 18.dp)) { content() } }
}

@Composable
private fun SettingRow(title: String, subtitle: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun clearTitle(scope: ClearScope) = stringResource(
    when (scope) {
        ClearScope.SETTINGS -> R.string.clear_title_settings
        ClearScope.LISTS -> R.string.clear_title_lists
        ClearScope.PROGRESS -> R.string.clear_title_progress
        ClearScope.ALL -> R.string.clear_title_all
    },
)

@Composable
private fun clearBody(scope: ClearScope) = stringResource(
    when (scope) {
        ClearScope.SETTINGS -> R.string.clear_body_settings
        ClearScope.LISTS -> R.string.clear_body_lists
        ClearScope.PROGRESS -> R.string.clear_body_progress
        ClearScope.ALL -> R.string.clear_body_all
    },
)

@StringRes
private fun BackupScope.labelRes() = when (this) {
    BackupScope.SETTINGS -> R.string.scope_settings
    BackupScope.LISTS -> R.string.scope_lists
    BackupScope.PROGRESS -> R.string.scope_progress
    BackupScope.ALL -> R.string.scope_all
}

@StringRes
private fun BackupError.messageRes() = when (this) {
    BackupError.NOT_A_BACKUP -> R.string.backup_error_not_a_backup
    BackupError.NEWER_VERSION -> R.string.backup_error_newer_version
    BackupError.DAMAGED -> R.string.backup_error_damaged
    BackupError.UNKNOWN_VALUES -> R.string.backup_error_unknown_values
}

@Composable
private fun ClearRow(title: String, subtitle: String, danger: Boolean = false, onClick: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().heightIn(min = 64.dp)
            .selectable(selected = false, role = Role.Button, onClick = onClick)
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = if (danger) MaterialTheme.colorScheme.error else Color.Unspecified)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private enum class BackupMode { EXPORT, IMPORT }

/** A selectable pill, used for the session length and the focus skills. */
@Composable
private fun ChoiceChip(text: String, selected: Boolean, modifier: Modifier = Modifier, role: Role = Role.RadioButton, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.heightIn(min = 52.dp).selectable(selected = selected, role = role, onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = if (selected) colors.secondaryContainer else colors.background,
        border = BorderStroke(1.5.dp, if (selected) colors.primary else colors.outlineVariant),
    ) {
        androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
            Text(
                text, style = MaterialTheme.typography.titleSmall,
                color = if (selected) colors.primary else colors.onSurfaceVariant,
            )
        }
    }
}
