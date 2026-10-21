package io.github.conorryan.weeklies.ui.settings

import android.text.format.DateUtils
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.conorryan.weeklies.AppGraph
import io.github.conorryan.weeklies.data.model.AppColorScheme
import io.github.conorryan.weeklies.ui.TabHeader
import io.github.conorryan.weeklies.ui.ToastBus
import io.github.conorryan.weeklies.ui.theme.HeaderColor
import io.github.conorryan.weeklies.ui.theme.headerColorFor

private const val PORTIONS_INFO_TEXT =
    "Used to scale recipe amounts on your shopping list. Will only work if numeric quantities are included in ingredients."

private const val MISTRAL_KEY_INFO_TEXT =
    "Your Mistral API key is stored only on this device and is not included in backup export or import. Get a key at console.mistral.ai."

private const val BACKUP_FOLDER_INFO_TEXT =
    "Backups are written to a folder you choose — pick one from Google Drive, Nextcloud, or similar and your provider syncs it off this phone. The newest 3 backups are kept."

private const val AUTO_BACKUP_INFO_TEXT =
    "A backup is written automatically shortly after your recipes, plan, or shopping list change."

@Composable
fun SectionHeading(text: String, first: Boolean = false) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = if (first) 16.dp else 20.dp, bottom = 2.dp),
    )
}

@Composable
fun SettingsScreen(
    graph: AppGraph,
    toastBus: ToastBus,
    onOpenHelp: () -> Unit,
) {
    val vm: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory(graph, toastBus))
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    var portionsText by remember { mutableStateOf(state.settings.portionsPerDay.toString()) }
    var keyText by remember { mutableStateOf("") }
    var portionsInfoOpen by remember { mutableStateOf(false) }
    var keyInfoOpen by remember { mutableStateOf(false) }

    // Keep inputs in sync when values change elsewhere (e.g. after backup import).
    LaunchedEffect(state.settings.portionsPerDay) { portionsText = state.settings.portionsPerDay.toString() }
    LaunchedEffect(state.apiKey) { keyText = state.apiKey }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        when (uri) {
            null -> toastBus.info("Export canceled")
            else -> vm.exportBackup {
                context.contentResolver.openOutputStream(uri)
                    ?: error("Could not open file for writing")
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        when (uri) {
            null -> toastBus.info("Import canceled")
            else -> vm.importBackup {
                context.contentResolver.openInputStream(uri)
                    ?: error("Could not read that file")
            }
        }
    }

    val folderPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        when (uri) {
            null -> toastBus.info("Folder selection canceled")
            else -> vm.chooseBackupFolder(uri)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        TabHeader("Settings", Icons.Filled.Settings, headerColorFor(HeaderColor.SETTINGS))

        SectionHeading("Appearance", first = true)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Dark mode", style = MaterialTheme.typography.bodyLarge)
            Switch(
                checked = state.settings.colorScheme == AppColorScheme.DARK,
                onCheckedChange = { on -> vm.setColorScheme(on) },
            )
        }

        SectionHeading("Planning")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Portions per day",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = { portionsInfoOpen = true }) { Text("Info") }
            OutlinedTextField(
                value = portionsText,
                onValueChange = { portionsText = it },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (!vm.applyPortions(portionsText)) {
                            portionsText = state.settings.portionsPerDay.toString()
                        }
                        focusManager.clearFocus()
                    },
                ),
                singleLine = true,
                modifier = Modifier.width(88.dp),
            )
        }

        SectionHeading("Recipe import")
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Mistral API key",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { keyInfoOpen = true }) { Text("Info") }
            }
            OutlinedTextField(
                value = keyText,
                onValueChange = { keyText = it },
                placeholder = { Text("Paste API key") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
            )
            TextButton(onClick = { vm.saveApiKey(keyText) }) { Text("Save key") }
            if (state.apiKey.trim().isNotEmpty()) {
                TextButton(onClick = {
                    vm.clearApiKey()
                    keyText = ""
                }) { Text("Clear key") }
            }
        }

        SectionHeading("Data")
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            TextButton(
                onClick = { exportLauncher.launch(graph.backupManager.defaultBackupFilename()) },
                enabled = !state.backupBusy,
            ) { Text("Export…") }
            TextButton(
                onClick = { importLauncher.launch(arrayOf("application/json", "text/plain")) },
                enabled = !state.backupBusy,
            ) {
                Text("Import… (WARNING: replaces all data)")
            }
        }

        SectionHeading("Backup folder")
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            if (state.backupFolder.configured) {
                Text(
                    "Backing up to: ${state.backupFolder.name ?: "configured folder"}",
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                Text(
                    BACKUP_FOLDER_INFO_TEXT,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                )
            }
            TextButton(onClick = { folderPicker.launch(null) }) {
                Text(if (state.backupFolder.configured) "Change folder…" else "Choose folder…")
            }
            if (state.backupFolder.configured) {
                TextButton(onClick = { vm.clearBackupFolder() }) { Text("Remove folder") }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Back up automatically", style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked = state.backupFolder.autoBackup,
                        onCheckedChange = { on -> vm.setAutoBackup(on) },
                    )
                }
                Text(
                    AUTO_BACKUP_INFO_TEXT,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                )
                TextButton(
                    onClick = { vm.backupNowToFolder() },
                    enabled = !state.backupBusy,
                ) { Text("Back up now") }
            }
            state.backupFolder.lastBackupAt?.let { at ->
                Text(
                    "Last backup: ${DateUtils.getRelativeTimeSpanString(at.toEpochMilli())}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                )
            }
        }

        SectionHeading("Support")
        TextButton(onClick = onOpenHelp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text("Help")
        }
    }

    if (portionsInfoOpen) {
        AlertDialog(
            onDismissRequest = { portionsInfoOpen = false },
            title = { Text("Portions per day") },
            text = { Text(PORTIONS_INFO_TEXT) },
            confirmButton = {
                OutlinedButton(onClick = { portionsInfoOpen = false }) { Text("Close") }
            },
        )
    }
    if (keyInfoOpen) {
        AlertDialog(
            onDismissRequest = { keyInfoOpen = false },
            title = { Text("Mistral API key") },
            text = { Text(MISTRAL_KEY_INFO_TEXT) },
            confirmButton = {
                OutlinedButton(onClick = { keyInfoOpen = false }) { Text("Close") }
            },
        )
    }
}
