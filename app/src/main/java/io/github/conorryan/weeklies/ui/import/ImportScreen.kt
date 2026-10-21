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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.conorryan.weeklies.AppGraph
import io.github.conorryan.weeklies.ui.ActionButton
import io.github.conorryan.weeklies.ui.ModalScreen
import io.github.conorryan.weeklies.ui.recipes.RecipeForm

@Composable
fun ImportScreen(
    graph: AppGraph,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val vm: ImportViewModel = viewModel(factory = ImportViewModel.Factory(graph))
    val state by vm.state.collectAsStateWithLifecycle()
    val apiKey by graph.secretsStore.mistralApiKey.collectAsStateWithLifecycle("")
    val keyReady = apiKey.trim().isNotEmpty()

    val preview = state.preview
    if (preview != null) {
        ModalScreen(title = "Review import", onBack = onBack) {
            Text(
                "Review the imported recipe, edit if needed, then save. Ingredient amounts were scaled " +
                    "to one portion (the recipe's serving count was used only for scaling).",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 8.dp),
            )
            RecipeForm(
                initialRecipe = preview,
                onAddRecipe = { recipe ->
                    vm.saveRecipe(recipe)
                    onBack()
                },
            )
            TextButton(onClick = { vm.clearPreview() }) { Text("Import again") }
        }
        return
    }

    ModalScreen(title = "Import recipe", onBack = onBack) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(vertical = 8.dp),
        ) {
            Text("Recipe URL", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
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
                    label = "Import",
                    onClick = { vm.runScraperImport() },
                    enabled = !state.busy,
                    modifier = Modifier.weight(1f),
                )
                ActionButton(
                    label = "Import with AI",
                    onClick = { vm.runAiImport(apiKey) },
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
                    TextButton(onClick = { vm.cancelImport() }) { Text("Cancel") }
                }
            }

            if (!keyReady) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Import with AI needs a Mistral API key in Settings.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    TextButton(onClick = onOpenSettings) { Text("Open Settings") }
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
                            error.title,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                        Text(
                            error.message,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        if (error.hint != null) {
                            Text(
                                error.hint,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }

            Text(
                "Import reads recipe data from JSON-LD on the page on this device — no API key " +
                    "needed. Import with AI sends page content to Mistral using your key. Amounts are stored " +
                    "for one portion; the shopping list scales them when you add a planned recipe. Requires an " +
                    "internet connection to fetch the page (native) or when using AI.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
