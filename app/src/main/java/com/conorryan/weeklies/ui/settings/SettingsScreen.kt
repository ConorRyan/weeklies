package com.conorryan.weeklies.ui.settings

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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.conorryan.weeklies.AppGraph
import com.conorryan.weeklies.data.backup.BackupErrors
import com.conorryan.weeklies.data.model.AppColorScheme
import com.conorryan.weeklies.data.model.isValidPortionsPerDay
import com.conorryan.weeklies.ui.TabHeader
import com.conorryan.weeklies.ui.ToastBus
import com.conorryan.weeklies.ui.theme.HeaderColor
import com.conorryan.weeklies.ui.theme.headerColorFor
import kotlinx.coroutines.launch

private const val PORTIONS_INFO_TEXT =
    "Used to scale recipe amounts on your shopping list. Will only work if numeric quantities are included in ingredients."

private const val MISTRAL_KEY_INFO_TEXT =
    "Your Mistral API key is stored only on this device and is not included in backup export or import. Get a key at console.mistral.ai."

@Composable
fun SectionHeading(text: String, first: Boolean = false) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
        modifier = Modifier.padding(top = if (first) 24.dp else 28.dp, bottom = 4.dp),
    )
}

@Composable
fun SettingsScreen(
    graph: AppGraph,
    toastBus: ToastBus,
    onOpenHelp: () -> Unit,
) {
    val settings by graph.settingsStore.settings.collectAsStateWithLifecycle(
        com.conorryan.weeklies.data.model.AppSettings(
            colorScheme = com.conorryan.weeklies.data.model.AppColorScheme.DARK,
            portionsPerDay = 2,
        ),
    )
    val apiKey by graph.secretsStore.mistralApiKey.collectAsStateWithLifecycle("")
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var backupBusy by remember { mutableStateOf(false) }
    var portionsText by remember { mutableStateOf(settings.portionsPerDay.toString()) }
    var keyText by remember { mutableStateOf("") }
    var portionsInfoOpen by remember { mutableStateOf(false) }
    var keyInfoOpen by remember { mutableStateOf(false) }

    // Keep inputs in sync when values change elsewhere (e.g. after backup import).
    LaunchedEffect(settings.portionsPerDay) { portionsText = settings.portionsPerDay.toString() }
    LaunchedEffect(apiKey) { keyText = apiKey }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri == null) {
            toastBus.info("Export canceled")
        } else {
            scope.launch {
                backupBusy = true
                try {
                    val json = graph.backupManager.buildBackupJson()
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(json.toByteArray(Charsets.UTF_8))
                    } ?: error("Could not open file for writing")
                } catch (e: Exception) {
                    if (e is kotlinx.coroutines.CancellationException) {
                        toastBus.info("Export canceled")
                    } else {
                        toastBus.error("Export failed", BackupErrors.formatBackupError(e, BackupErrors.Kind.EXPORT))
                    }
                } finally {
                    backupBusy = false
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) {
            toastBus.info("Import canceled")
        } else {
            scope.launch {
                backupBusy = true
                try {
                    val text = context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
                        ?: error("Could not read that file")
                    graph.backupManager.applyBackupImport(text)
                } catch (e: Exception) {
                    if (e is kotlinx.coroutines.CancellationException) {
                        toastBus.info("Import canceled")
                    } else {
                        toastBus.error("Import failed", BackupErrors.formatBackupError(e, BackupErrors.Kind.IMPORT))
                    }
                } finally {
                    backupBusy = false
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        TabHeader("Settings", Icons.Filled.Settings, headerColorFor(HeaderColor.SETTINGS))

        SectionHeading("Appearance", first = true)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Dark mode", style = MaterialTheme.typography.bodyLarge)
            Switch(
                checked = settings.colorScheme == AppColorScheme.DARK,
                onCheckedChange = { on ->
                    scope.launch {
                        graph.settingsStore.setColorScheme(
                            if (on) AppColorScheme.DARK else AppColorScheme.LIGHT,
                        )
                    }
                },
            )
        }

        SectionHeading("Planning")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
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
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.width(88.dp),
            )
        }
        TextButton(onClick = {
            val n = portionsText.trim().toDoubleOrNull()
            if (n != null && isValidPortionsPerDay(n)) {
                scope.launch { graph.settingsStore.setPortionsPerDay(n.toInt()) }
                portionsText = n.toInt().toString()
            } else {
                portionsText = settings.portionsPerDay.toString()
            }
        }) { Text("Apply") }

        SectionHeading("Recipe import")
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(vertical = 8.dp),
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
            TextButton(onClick = {
                scope.launch { graph.secretsStore.setMistralApiKey(keyText.trim()) }
            }) { Text("Save key") }
            if (apiKey.trim().isNotEmpty()) {
                TextButton(onClick = {
                    scope.launch { graph.secretsStore.clearMistralApiKey() }
                    keyText = ""
                }) { Text("Clear key") }
            }
        }

        SectionHeading("Data")
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(vertical = 8.dp),
        ) {
            TextButton(
                onClick = { exportLauncher.launch(graph.backupManager.defaultBackupFilename()) },
                enabled = !backupBusy,
            ) { Text("Export…") }
            TextButton(
                onClick = { importLauncher.launch(arrayOf("application/json", "text/plain")) },
                enabled = !backupBusy,
            ) {
                Text("Import… (WARNING: replaces all data)")
            }
        }

        SectionHeading("Support")
        TextButton(onClick = onOpenHelp, modifier = Modifier.padding(vertical = 8.dp)) {
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
