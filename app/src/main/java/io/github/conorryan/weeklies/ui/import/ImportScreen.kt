package io.github.conorryan.weeklies.ui.import

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.conorryan.weeklies.AppGraph
import io.github.conorryan.weeklies.R
import io.github.conorryan.weeklies.ui.ActionButton
import io.github.conorryan.weeklies.ui.ModalScreen
import io.github.conorryan.weeklies.ui.recipes.RecipeForm
import io.github.conorryan.weeklies.ui.recipes.RecipeFormViewModel

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

    val preview = state.preview
    if (preview != null) {
        val formVm: RecipeFormViewModel = viewModel(
            key = "import-review",
            factory = RecipeFormViewModel.importReviewFactory(graph, preview),
        )
        val formState by formVm.state.collectAsStateWithLifecycle()
        ModalScreen(title = stringResource(R.string.import_review_title), onBack = onBack) {
            Text(
                stringResource(R.string.import_review_hint),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 8.dp),
            )
            RecipeForm(
                state = formState,
                vm = formVm,
                submitLabel = stringResource(R.string.import_save_recipe),
            ) { recipe ->
                formVm.save(recipe, onBack)
            }
            TextButton(onClick = { vm.clearPreview() }) { Text(stringResource(R.string.import_again)) }
        }
        return
    }

    ModalScreen(title = stringResource(R.string.import_recipe_title), onBack = onBack) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(vertical = 8.dp),
        ) {
            Text(
                stringResource(R.string.import_url_label),
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyMedium,
            )
            OutlinedTextField(
                value = state.url,
                onValueChange = { vm.setUrl(it) },
                placeholder = { Text("https://…") },
                singleLine = true,
                enabled = !state.busy,
                modifier = Modifier.fillMaxWidth(),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                ActionButton(
                    label = stringResource(R.string.import_button),
                    onClick = { vm.runScraperImport() },
                    enabled = !state.busy,
                    modifier = Modifier.weight(1f),
                )
                ActionButton(
                    label = stringResource(R.string.import_with_ai),
                    onClick = { vm.runAiImport(storedKey.orEmpty()) },
                    enabled = !state.busy && keyReady,
                    modifier = Modifier.weight(1f),
                )
            }

            if (state.busy) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator()
                    TextButton(onClick = { vm.cancelImport() }) { Text(stringResource(R.string.cancel)) }
                }
            }

            if (storedKey != null && !keyReady) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        stringResource(R.string.import_ai_needs_key),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    TextButton(onClick = onOpenSettings) { Text(stringResource(R.string.import_open_settings)) }
                }
            }

            val error = state.error
            if (error != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            stringResource(error.title),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                        Text(
                            error.messageArg?.let { stringResource(error.message, it) }
                                ?: stringResource(error.message),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        val hint = error.hint
                        if (hint != null) {
                            Text(
                                stringResource(hint),
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }

            Text(
                stringResource(R.string.import_explainer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
