package io.github.conorryan.weeklies.ui.support

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.conorryan.weeklies.R
import io.github.conorryan.weeklies.ui.ModalScreen

@Composable
private fun Section(@StringRes title: Int, paragraphs: List<Int>) {
    Text(
        stringResource(title),
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 20.dp, bottom = 8.dp),
    )
    for (paragraph in paragraphs) {
        Text(stringResource(paragraph), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(bottom = 8.dp))
    }
}

@Composable
fun HelpScreen(
    onBack: () -> Unit,
) {
    ModalScreen(title = stringResource(R.string.help), onBack = onBack) {
        Section(
            R.string.app_name,
            listOf(
                R.string.help_weeklies_intro,
            ),
        )
        Section(
            R.string.tab_recipes,
            listOf(
                R.string.help_recipes_intro,
                R.string.help_recipes_edit,
                R.string.help_recipes_import,
                R.string.help_recipes_import_ai,
            ),
        )
        Section(
            R.string.help_shopping_list_title,
            listOf(
                R.string.help_shopping_add,
            ),
        )
        Section(
            R.string.settings,
            listOf(
                R.string.help_settings_dark_mode,
                R.string.help_settings_portions,
                R.string.help_settings_import_key,
                R.string.help_settings_help_link,
            ),
        )
        Section(
            R.string.help_data_title,
            listOf(
                R.string.help_data_local,
                R.string.help_data_export,
                R.string.help_data_import,
            ),
        )
    }
}

@Composable
fun PrivacyScreen(onBack: () -> Unit) {
    ModalScreen(title = stringResource(R.string.privacy_policy_title), onBack = onBack) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Section(
                R.string.privacy_summary_title,
                listOf(
                    R.string.privacy_summary,
                ),
            )
            Section(
                R.string.privacy_data_stored_title,
                listOf(
                    R.string.privacy_data_stored,
                ),
            )
            Section(
                R.string.privacy_backups_title,
                listOf(
                    R.string.privacy_backups,
                ),
            )
            Section(
                R.string.privacy_recipe_import_title,
                listOf(
                    R.string.privacy_recipe_import_native,
                    R.string.privacy_recipe_import_ai,
                ),
            )
            Section(
                R.string.privacy_permissions_title,
                listOf(
                    R.string.privacy_permissions,
                ),
            )
            Section(
                R.string.privacy_children_title,
                listOf(
                    R.string.privacy_children,
                ),
            )
            Section(
                R.string.privacy_changes_title,
                listOf(
                    R.string.privacy_changes,
                ),
            )
            Section(
                R.string.privacy_contact_title,
                listOf(
                    R.string.privacy_contact,
                ),
            )
        }
    }
}
