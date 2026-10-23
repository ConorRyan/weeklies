package io.github.conorryan.weeklies.ui.support

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.conorryan.weeklies.ui.ModalScreen

@Composable
private fun Section(title: String, paragraphs: List<String>) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 20.dp, bottom = 8.dp),
    )
    for (paragraph in paragraphs) {
        Text(paragraph, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(bottom = 8.dp))
    }
}

@Composable
fun HelpScreen(
    onBack: () -> Unit,
) {
    ModalScreen(title = "Help", onBack = onBack) {
        Section(
            "Weeklies",
            listOf(
                "Weeklies is for planning meals once, then not asking yourself what to eat every day. " +
                    "Your week runs Monday through Sunday. Tap a day to choose a recipe, reuse the same " +
                    "plan across multiple weeks, or change any day whenever your plans change. Today's row " +
                    "has a light highlight and the day name and recipe line are bold so you can spot the " +
                    "current day quickly.",
            ),
        )
        Section(
            "Recipes",
            listOf(
                "Recipes are for meals you already know how to cook. Store the ingredients you need, " +
                    "one line at a time, without writing out full cooking instructions. Open the Recipes " +
                    "tab to add, review, or remove saved meals.",
                "To edit a recipe, open it from the list and tap Edit recipe. The screen matches new " +
                    "recipe (same fields and Save recipe), with your current name and ingredients filled " +
                    "in; saving replaces that recipe and keeps the same entry everywhere it is used in your " +
                    "plan.",
                "Import (Recipes tab) reads recipe data from JSON-LD on the page — no API key " +
                    "needed. Enter a recipe URL; ingredient amounts are scaled to one portion before " +
                    "saving. You review and edit before saving.",
                "Import with AI uses your Mistral API key (Settings → Recipe import) to extract the " +
                    "recipe when schema data is missing or incomplete. The key stays on this device and is " +
                    "not included in backup files. Same URL input as Import; requires an internet " +
                    "connection.",
            ),
        )
        Section(
            "Shopping list",
            listOf(
                "Type at the bottom and tap + to add a line. Tap the checkbox or anywhere on the row to " +
                    "check an item off. Use Remove checked to delete every checked line. Tap Add " +
                    "today's recipe to append ingredients from whatever recipe is planned for today on " +
                    "the Weeklies tab; amounts are scaled using Portions per day in Settings (each number " +
                    "in an ingredient line is multiplied by that value).",
            ),
        )
        Section(
            "Settings",
            listOf(
                "Turn dark mode on or off; the choice is remembered after you close the app.",
                "Portions per day is the multiplier used when you use Add today's recipe on the " +
                    "shopping list: every numeric quantity in each ingredient line is multiplied by this " +
                    "value.",
                "Recipe import: add your Mistral API key under Recipe import. It is stored locally and " +
                    "excluded from backup export and import.",
                "This Help screen is linked from Settings.",
            ),
        )
        Section(
            "Data",
            listOf(
                "Weeklies is fully local and completely offline. Recipes, your weekly plan, and the " +
                    "shopping list are stored on this device, where they stay until you remove them in the " +
                    "app or replace them by importing a backup.",
                "In Settings, open the Data section. Export writes one JSON file (default name like " +
                    "weeklies-backup-2026-05-09.json) containing recipes, your weekly plan, the shopping list, and " +
                    "settings — including dark mode and portions per day.",
                "Import replaces all stored data with the contents of that file. Nothing is merged with " +
                    "what you already had — confirm only when you intend to overwrite the app " +
                    "completely.",
            ),
        )
    }
}

@Composable
fun PrivacyScreen(onBack: () -> Unit) {
    ModalScreen(title = "Privacy policy", onBack = onBack) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Section(
                "Summary",
                listOf(
                    "Weeklies stores your data on your device. The app has no accounts, no analytics, " +
                        "no advertising, and no tracking. It does not send your data anywhere unless you " +
                        "use the optional AI recipe import described below.",
                ),
            )
            Section(
                "Data stored on your device",
                listOf(
                    "Your recipes, weekly plan, shopping list, and settings (dark mode, portions per " +
                        "day) are saved in local storage on this device only. They stay there until you " +
                        "delete them in the app or replace them by importing a backup file. This data is " +
                        "never transmitted to us or to any server.",
                ),
            )
            Section(
                "Backups",
                listOf(
                    "Export (Settings → Data) writes a JSON file to a location you choose. The file is " +
                        "yours: the app never uploads it, and the app has no access to any other files on " +
                        "your device. Import reads only the backup file you pick, and replaces your " +
                        "stored data with its contents.",
                ),
            )
            Section(
                "Recipe import",
                listOf(
                    "Import fetches the recipe page from the web address you enter, on your device, and " +
                        "reads the recipe data from the page itself. The request goes directly to that " +
                        "website, the same as opening it in a browser.",
                    "Import with AI is optional and only runs when you ask for it. It sends the recipe " +
                        "web address or page text you provide to the Mistral AI API (api.mistral.ai) " +
                        "using your own API key, so the recipe can be extracted when the page does not " +
                        "contain structured recipe data. Your API key is stored on this device only and " +
                        "is never included in backup exports. These requests are processed by Mistral " +
                        "according to Mistral's privacy policy; we do not see them and cannot access " +
                        "your key. Without this feature, the app makes no network requests at all.",
                ),
            )
            Section(
                "Permissions",
                listOf(
                    "The app can open the system file picker when you export or import a backup, and " +
                        "only for the file or folder you select. It requests no other permissions.",
                ),
            )
            Section(
                "Children",
                listOf(
                    "Weeklies is a general-purpose meal planner and is not directed at children. " +
                        "Because the app collects no personal data, no age verification is used.",
                ),
            )
            Section(
                "Changes to this policy",
                listOf(
                    "If the app changes in a way that affects your privacy, this policy will be updated " +
                        "and published with the matching app update. The current version is always " +
                        "available on GitHub and inside the app.",
                ),
            )
            Section(
                "Contact",
                listOf("Questions about this policy can be raised on GitHub: github.com/ConorRyan/weeklies."),
            )
        }
    }
}
