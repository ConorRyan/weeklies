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
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.conorryan.weeklies.AppGraph
import io.github.conorryan.weeklies.R
import io.github.conorryan.weeklies.data.model.AppColorScheme
import io.github.conorryan.weeklies.ui.InfoDialog
import io.github.conorryan.weeklies.ui.TabHeader
import io.github.conorryan.weeklies.ui.ToastBus
import io.github.conorryan.weeklies.ui.theme.HeaderColor
import io.github.conorryan.weeklies.ui.theme.headerColorFor

@Composable
private fun SectionHeader(text: String, first: Boolean = false) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = if (first) 24.dp else 28.dp, bottom = 4.dp),
    )
}

@Composable
private fun SupportingText(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp),
    )
}

/** One settings row: label, optional info icon, and an optional trailing control. */
@Composable
private fun SettingRow(
    label: String,
    modifier: Modifier = Modifier,
    infoContentDescription: String? = null,
    onInfo: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        if (infoContentDescription != null && onInfo != null) {
            IconButton(onClick = onInfo) {
                Icon(Icons.Outlined.Info, contentDescription = infoContentDescription)
            }
        }
        trailing?.invoke()
    }
}

@Composable
fun SettingsScreen(
    graph: AppGraph,
    toastBus: ToastBus,
    onOpenHelp: () -> Unit,
    onOpenPrivacy: () -> Unit,
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
            null -> toastBus.info(R.string.export_canceled_toast)
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
            null -> toastBus.info(R.string.import_canceled_toast)
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
            null -> toastBus.info(R.string.folder_selection_canceled)
            else -> vm.chooseBackupFolder(uri)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        TabHeader(stringResource(R.string.settings), Icons.Filled.Settings, headerColorFor(HeaderColor.SETTINGS))

        SectionHeader(stringResource(R.string.settings_section_appearance), first = true)
        SettingRow(
            label = stringResource(R.string.settings_dark_mode),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            trailing = {
                Switch(
                    checked = state.settings.colorScheme == AppColorScheme.DARK,
                    onCheckedChange = { on -> vm.setColorScheme(on) },
                )
            },
        )

        SectionHeader(stringResource(R.string.settings_section_planning))
        SettingRow(
            label = stringResource(R.string.settings_portions_per_day),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            infoContentDescription = stringResource(R.string.settings_about_portions),
            onInfo = { portionsInfoOpen = true },
            trailing = {
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
            },
        )

        SectionHeader(stringResource(R.string.settings_section_recipe_import))
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            SettingRow(
                label = stringResource(R.string.settings_mistral_api_key),
                infoContentDescription = stringResource(R.string.settings_about_mistral_key),
                onInfo = { keyInfoOpen = true },
            )
            OutlinedTextField(
                value = keyText,
                onValueChange = { keyText = it },
                placeholder = { Text(stringResource(R.string.settings_paste_api_key)) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { vm.saveApiKey(keyText) },
                    modifier = Modifier.weight(1f),
                ) { Text(stringResource(R.string.settings_save_key)) }
                if (state.apiKey.trim().isNotEmpty()) {
                    TextButton(
                        onClick = {
                            vm.clearApiKey()
                            keyText = ""
                        },
                        modifier = Modifier.weight(1f),
                    ) { Text(stringResource(R.string.settings_clear_key)) }
                }
            }
        }

        SectionHeader(stringResource(R.string.settings_section_data))
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            OutlinedButton(
                onClick = { exportLauncher.launch(graph.backupManager.defaultBackupFilename()) },
                enabled = !state.backupBusy,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.settings_export_backup)) }
            OutlinedButton(
                onClick = { importLauncher.launch(arrayOf("application/json", "text/plain")) },
                enabled = !state.backupBusy,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.settings_import_backup)) }
            SupportingText(stringResource(R.string.settings_import_replaces_data))
        }

        SectionHeader(stringResource(R.string.settings_section_backup_folder))
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(vertical = 8.dp),
        ) {
            if (state.backupFolder.configured) {
                val folderName = state.backupFolder.name?.takeIf { it.isNotBlank() }
                    ?: stringResource(R.string.settings_configured_folder)
                SupportingText(stringResource(R.string.settings_backing_up_to, folderName))
            } else {
                SupportingText(stringResource(R.string.settings_backup_folder_info))
            }
            OutlinedButton(
                onClick = { folderPicker.launch(null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                Text(
                    if (state.backupFolder.configured) {
                        stringResource(R.string.settings_change_folder)
                    } else {
                        stringResource(R.string.settings_choose_folder)
                    },
                )
            }
            if (state.backupFolder.configured) {
                TextButton(
                    onClick = { vm.clearBackupFolder() },
                    modifier = Modifier.padding(horizontal = 16.dp),
                ) { Text(stringResource(R.string.settings_remove_folder)) }
                SettingRow(
                    label = stringResource(R.string.settings_auto_backup),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    trailing = {
                        Switch(
                            checked = state.backupFolder.autoBackup,
                            onCheckedChange = { on -> vm.setAutoBackup(on) },
                        )
                    },
                )
                SupportingText(stringResource(R.string.settings_auto_backup_info))
                Button(
                    onClick = { vm.backupNowToFolder() },
                    enabled = !state.backupBusy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                ) { Text(stringResource(R.string.settings_backup_now)) }
            }
            state.backupFolder.lastBackupAt?.let { at ->
                SupportingText(
                    stringResource(
                        R.string.settings_last_backup,
                        DateUtils.getRelativeTimeSpanString(at.toEpochMilli()).toString(),
                    ),
                )
            }
        }

        SectionHeader(stringResource(R.string.settings_section_other))
        TextButton(
            onClick = onOpenHelp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text(stringResource(R.string.help))
        }
        TextButton(
            onClick = onOpenPrivacy,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text(stringResource(R.string.settings_privacy_policy))
        }
    }

    if (portionsInfoOpen) {
        InfoDialog(
            title = stringResource(R.string.settings_portions_per_day),
            onDismiss = { portionsInfoOpen = false },
            text = { Text(stringResource(R.string.settings_portions_info)) },
        )
    }
    if (keyInfoOpen) {
        InfoDialog(
            title = stringResource(R.string.settings_mistral_api_key),
            onDismiss = { keyInfoOpen = false },
            text = { Text(stringResource(R.string.settings_key_info)) },
        )
    }
}
