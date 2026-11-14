package app.trainer.ui

import app.trainer.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.trainer.data.GameListRepository
import app.trainer.games.GameCatalog
import kotlinx.coroutines.launch

/** Create ([listId] = 0) or edit a saved game list. */
@Composable
fun ListEditorScreen(lists: GameListRepository, listId: Long, onDone: () -> Unit) {
    val scope = rememberCoroutineScope()
    val colors = MaterialTheme.colorScheme
    var name by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf(listOf<String>()) }
    var shuffle by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(listId) {
        if (listId != 0L) lists.get(listId)?.let { name = it.name; selected = it.gameIds; shuffle = it.shuffle }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            IconAction(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back), onDone)
            Text(
                stringResource(if (listId == 0L) R.string.list_new else R.string.list_edit),
                style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() },
            )
        }
        OutlinedTextField(
            value = name, onValueChange = { name = it.take(30) }, singleLine = true,
            label = { Text(stringResource(R.string.list_name)) }, modifier = Modifier.fillMaxWidth(),
        )
        Text(stringResource(R.string.list_games), style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
        GameCatalog.all.forEach { game ->
            val checked = game.id in selected
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = colors.surfaceContainer,
                border = BorderStroke(1.5.dp, if (checked) colors.primary else colors.outlineVariant),
            ) {
                Row(
                    Modifier.heightIn(min = 56.dp).toggleable(
                        value = checked, role = Role.Checkbox,
                        onValueChange = { on -> selected = if (on) selected + game.id else selected - game.id },
                    ).padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(game.titleRes), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                    Checkbox(checked = checked, onCheckedChange = null)
                }
            }
        }
        Row(Modifier.fillMaxWidth().heightIn(min = 64.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.list_shuffle), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.list_shuffle_subtitle), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
            Switch(checked = shuffle, onCheckedChange = { shuffle = it })
        }
        if (selected.isEmpty()) {
            Text(stringResource(R.string.list_pick_one), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        }
        PrimaryButton(stringResource(R.string.list_save), {
            if (selected.isNotEmpty()) scope.launch {
                // Keep catalog order for the saved order.
                val ordered = GameCatalog.all.map { it.id }.filter { it in selected }
                lists.save(listId, name, ordered, shuffle)
                onDone()
            }
        }, height = 56.dp)
        if (listId != 0L) SecondaryButton(stringResource(R.string.list_delete), { confirmDelete = true })
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.list_delete_title)) },
            text = { Text(stringResource(R.string.list_delete_body)) },
            confirmButton = { TextButton({ scope.launch { lists.delete(listId); onDone() } }) { Text(stringResource(R.string.action_delete)) } },
            dismissButton = { TextButton({ confirmDelete = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}
