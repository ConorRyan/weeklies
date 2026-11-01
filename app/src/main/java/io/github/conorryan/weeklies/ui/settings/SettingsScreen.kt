package io.github.conorryan.weeklies.ui.settings

import android.text.format.DateUtils
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.conorryan.weeklies.AppGraph
import io.github.conorryan.weeklies.R
import io.github.conorryan.weeklies.data.model.AppColorScheme
import io.github.conorryan.weeklies.ui.CardDivider
import io.github.conorryan.weeklies.ui.CardGroup
import io.github.conorryan.weeklies.ui.ConfirmDialog
import io.github.conorryan.weeklies.ui.Eyebrow
import io.github.conorryan.weeklies.ui.ScreenTitle
import io.github.conorryan.weeklies.ui.ToastBus
import io.github.conorryan.weeklies.ui.shopping.MAX_PORTIONS_STEPPER

@Composable
private fun SectionLabel(text: String) {
    Eyebrow(text, Modifier.padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 8.dp))
}

/** Settings row: title, optional subtitle, optional trailing control; clickable when [onClick] is set. */
@Composable
private fun SettingRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    role: Role? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(role = role, onClick = onClick) else Modifier)
            .heightIn(min = 60.dp)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        trailing?.invoke()
    }
}

@Composable
private fun ChevronRow(title: String, onClick: () -> Unit) {
    SettingRow(
        title = title,
        onClick = onClick,
        trailing = {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
            )
        },
    )
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
    var keyText by remember { mutableStateOf("") }
    var restoreConfirmOpen by remember { mutableStateOf(false) }

    // Keep the key field in sync when it changes elsewhere (e.g. after a clear).
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
            .verticalScroll(rememberScrollState())
            .padding(bottom = 32.dp),
    ) {
        ScreenTitle(stringResource(R.string.settings))

        SectionLabel(stringResource(R.string.settings_section_cooking))
        CardGroup(Modifier.padding(horizontal = 20.dp)) {
            val portions = state.settings.portionsPerDay
            SettingRow(
                title = stringResource(R.string.settings_portions_per_day),
                subtitle = stringResource(R.string.settings_portions_subtitle),
                trailing = {
                    Row(
                        modifier = Modifier.background(MaterialTheme.colorScheme.background, MaterialTheme.shapes.extraLarge),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = { vm.applyPortions((portions - 1).toString()) }, enabled = portions > 1) {
                            Icon(Icons.Filled.Remove, contentDescription = stringResource(R.string.portions_fewer))
                        }
                        Text(portions.toString(), style = MaterialTheme.typography.titleMedium)
                        IconButton(
                            onClick = { vm.applyPortions((portions + 1).toString()) },
                            enabled = portions < MAX_PORTIONS_STEPPER,
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.portions_more))
                        }
                    }
                },
            )
            CardDivider()
            val dark = state.settings.colorScheme == AppColorScheme.DARK
            SettingRow(
                title = stringResource(R.string.settings_dark_mode),
                onClick = { vm.setColorScheme(!dark) },
                role = Role.Switch,
                trailing = { Switch(checked = dark, onCheckedChange = null) },
            )
        }

        SectionLabel(stringResource(R.string.settings_section_backups))
        val folder = state.backupFolder
        CardGroup(Modifier.padding(horizontal = 20.dp)) {
            Row(
                Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 14.dp, bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Icon(
                    Icons.Outlined.Folder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.shapes.small)
                        .padding(10.dp)
                        .size(24.dp),
                )
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        if (folder.configured) {
                            folder.name?.takeIf { it.isNotBlank() } ?: stringResource(R.string.settings_configured_folder)
                        } else {
                            stringResource(R.string.settings_no_folder)
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    val lastBackup = folder.lastBackupAt
                    if (lastBackup != null) {
                        Text(
                            stringResource(
                                R.string.settings_last_backup,
                                DateUtils.getRelativeTimeSpanString(lastBackup.toEpochMilli()).toString(),
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                TextButton(onClick = { folderPicker.launch(null) }) {
                    Text(stringResource(if (folder.configured) R.string.settings_change_folder else R.string.settings_choose_folder))
                }
            }
            // Full width under the row so the explanation isn't squeezed beside the button.
            if (!folder.configured) {
                Text(
                    stringResource(R.string.settings_backup_folder_info),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 14.dp),
                )
            }
            if (folder.configured) {
                CardDivider()
                SettingRow(
                    title = stringResource(R.string.settings_auto_backup),
                    subtitle = stringResource(R.string.settings_auto_backup_info),
                    onClick = { vm.setAutoBackup(!folder.autoBackup) },
                    role = Role.Switch,
                    trailing = { Switch(checked = folder.autoBackup, onCheckedChange = null) },
                )
            }
            CardDivider()
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (folder.configured) {
                    Button(
                        onClick = { vm.backupNowToFolder() },
                        enabled = !state.backupBusy,
                        modifier = Modifier.weight(1f).heightIn(min = 44.dp),
                    ) { Text(stringResource(R.string.settings_backup_now)) }
                }
                OutlinedButton(
                    onClick = { exportLauncher.launch(graph.backupManager.defaultBackupFilename()) },
                    enabled = !state.backupBusy,
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.weight(1f).heightIn(min = 44.dp),
                ) { Text(stringResource(R.string.settings_export_file)) }
            }
        }
        Row(Modifier.padding(horizontal = 12.dp)) {
            TextButton(
                onClick = { restoreConfirmOpen = true },
                enabled = !state.backupBusy,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) { Text(stringResource(R.string.settings_restore_backup)) }
            if (folder.configured) {
                TextButton(
                    onClick = { vm.clearBackupFolder() },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                ) { Text(stringResource(R.string.settings_remove_folder)) }
            }
        }

        SectionLabel(stringResource(R.string.settings_section_recipe_import))
        CardGroup(Modifier.padding(horizontal = 20.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    stringResource(R.string.settings_mistral_api_key_optional),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                OutlinedTextField(
                    value = keyText,
                    onValueChange = { keyText = it },
                    placeholder = { Text(stringResource(R.string.settings_paste_api_key)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { vm.saveApiKey(keyText) },
                        enabled = keyText.trim() != state.apiKey.trim(),
                        modifier = Modifier.weight(1f),
                    ) { Text(stringResource(R.string.settings_save_key)) }
                    if (state.apiKey.trim().isNotEmpty()) {
                        FilledTonalButton(
                            onClick = {
                                vm.clearApiKey()
                                keyText = ""
                            },
                            modifier = Modifier.weight(1f),
                        ) { Text(stringResource(R.string.settings_clear_key)) }
                    }
                }
                Text(
                    stringResource(R.string.settings_key_info),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(24.dp))
        CardGroup(Modifier.padding(horizontal = 20.dp)) {
            ChevronRow(stringResource(R.string.help), onOpenHelp)
            CardDivider()
            ChevronRow(stringResource(R.string.settings_privacy_policy), onOpenPrivacy)
        }
    }

    if (restoreConfirmOpen) {
        ConfirmDialog(
            title = stringResource(R.string.settings_restore_confirm_title),
            confirmLabel = stringResource(R.string.settings_restore_choose_file),
            destructive = true,
            onConfirm = {
                restoreConfirmOpen = false
                importLauncher.launch(arrayOf("application/json", "text/plain"))
            },
            onDismiss = { restoreConfirmOpen = false },
            text = { Text(stringResource(R.string.settings_import_replaces_data)) },
        )
    }
}
