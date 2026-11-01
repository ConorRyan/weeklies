package io.github.conorryan.weeklies.ui.import

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.conorryan.weeklies.AppGraph
import io.github.conorryan.weeklies.R
import io.github.conorryan.weeklies.ui.Eyebrow
import io.github.conorryan.weeklies.ui.ModalScreen
import io.github.conorryan.weeklies.ui.recipes.RecipeForm
import io.github.conorryan.weeklies.ui.recipes.RecipeFormSaveButton
import io.github.conorryan.weeklies.ui.recipes.RecipeFormViewModel
import io.github.conorryan.weeklies.ui.recipes.RecommendedSitesDialog

@Composable
fun ImportScreen(
    graph: AppGraph,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val vm: ImportViewModel = viewModel(factory = ImportViewModel.Factory(graph))
    val state by vm.state.collectAsStateWithLifecycle()
    // Warm app state (loaded eagerly at startup), so the key is known in the
    // screen's first frame and no hint flashes in.
    val key by graph.mistralKeyState.collectAsStateWithLifecycle()
    val storedKey = key
    val keyReady = storedKey != null && storedKey.trim().isNotEmpty()
    var withAi by rememberSaveable { mutableStateOf(false) }
    var sitesOpen by remember { mutableStateOf(false) }

    val preview = state.preview
    if (preview != null) {
        val formVm: RecipeFormViewModel = viewModel(
            key = "import-review",
            factory = RecipeFormViewModel.importReviewFactory(graph, preview),
        )
        val formState by formVm.state.collectAsStateWithLifecycle()
        ModalScreen(
            title = stringResource(R.string.import_review_title),
            onBack = onBack,
            actions = { RecipeFormSaveButton(formVm) { recipe -> formVm.save(recipe, onBack) } },
        ) {
            Text(
                stringResource(R.string.import_review_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            RecipeForm(state = formState, vm = formVm)
            TextButton(onClick = { vm.clearPreview() }) { Text(stringResource(R.string.import_again)) }
        }
        return
    }

    ModalScreen(
        title = stringResource(R.string.import_recipe_title),
        onBack = onBack,
        bottomBar = {
            if (state.busy) {
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 52.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 3.dp)
                    Text(stringResource(R.string.import_importing), modifier = Modifier.weight(1f))
                    OutlinedButton(onClick = { vm.cancelImport() }) { Text(stringResource(R.string.cancel)) }
                }
            } else {
                Button(
                    onClick = { if (withAi) vm.runAiImport(storedKey.orEmpty()) else vm.runScraperImport() },
                    enabled = !withAi || keyReady,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                ) {
                    Text(stringResource(if (withAi) R.string.import_with_ai else R.string.import_button))
                }
            }
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Eyebrow(stringResource(R.string.import_url_label))
            OutlinedTextField(
                value = state.url,
                onValueChange = { vm.setUrl(it) },
                placeholder = { Text("https://…") },
                singleLine = true,
                enabled = !state.busy,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                shape = MaterialTheme.shapes.small,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ModeCard(
                title = stringResource(R.string.import_mode_standard),
                body = stringResource(R.string.import_mode_standard_body),
                selected = !withAi,
                onClick = { withAi = false },
                modifier = Modifier.weight(1f),
            )
            ModeCard(
                title = stringResource(R.string.import_mode_ai),
                body = stringResource(R.string.import_mode_ai_body),
                selected = withAi,
                ai = true,
                onClick = { withAi = true },
                modifier = Modifier.weight(1f),
            )
        }

        if (withAi && storedKey != null && !keyReady) {
            Surface(
                color = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                shape = MaterialTheme.shapes.medium,
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(R.string.import_ai_needs_key),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = onOpenSettings) { Text(stringResource(R.string.import_open_settings)) }
                }
            }
        }

        val error = state.error
        if (error != null) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                shape = MaterialTheme.shapes.medium,
            ) {
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(stringResource(error.title), fontWeight = FontWeight.Bold)
                        Text(
                            error.messageArg?.let { stringResource(error.message, it) }
                                ?: stringResource(error.message),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        val hint = error.hint
                        if (hint != null) {
                            Text(stringResource(hint), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }

        Surface(
            onClick = { sitesOpen = true },
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(Icons.Outlined.Public, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.recipe_recommended_sites), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                    Text(
                        stringResource(R.string.import_sites_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
            }
        }

        Text(
            stringResource(R.string.import_explainer),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    if (sitesOpen) RecommendedSitesDialog(onDismiss = { sitesOpen = false })
}

@Composable
private fun ModeCard(
    title: String,
    body: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    ai: Boolean = false,
) {
    Surface(
        selected = selected,
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            if (selected) 2.dp else 1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        ),
        modifier = modifier,
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (ai) {
                    Icon(
                        Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.size(14.dp),
                    )
                }
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            }
            Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
