package com.conorryan.weeklies.data.backup

import com.conorryan.weeklies.data.model.AppColorScheme
import com.conorryan.weeklies.data.model.AppSettings
import com.conorryan.weeklies.data.model.Recipe
import com.conorryan.weeklies.data.model.ShoppingItem
import com.conorryan.weeklies.data.model.Weekday
import com.conorryan.weeklies.data.model.emptyByDay
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class BackupCodecTest {

    private val defaults = AppSettings(AppColorScheme.DARK, 2)

    private fun sampleSnapshot() = BackupSnapshot(
        byDay = emptyByDay() + (Weekday.MON to "r1"),
        recipes = listOf(Recipe(id = "r1", name = "Pasta", ingredients = listOf("salt"))),
        items = listOf(ShoppingItem(id = "i1", label = "Milk", checked = false)),
    )

    @Test
    fun buildsPayloadMatchingTheRnFormat() {
        val json = BackupCodec.buildBackupJson(
            AppSettings(AppColorScheme.DARK, 2),
            sampleSnapshot(),
            "2024-01-01T00:00:00.000Z",
        )
        val root = Json.parseToJsonElement(json).let { it as kotlinx.serialization.json.JsonObject }

        assertEquals("backup", (root["format"] as kotlinx.serialization.json.JsonPrimitive).content)
        assertEquals(1, (root["version"] as kotlinx.serialization.json.JsonPrimitive).content.toInt())
        assertEquals(
            "2024-01-01T00:00:00.000Z",
            (root["exportedAt"] as kotlinx.serialization.json.JsonPrimitive).content,
        )
        val weeklyPlan = root["weeklyPlan"] as kotlinx.serialization.json.JsonObject
        val byDay = weeklyPlan["byDay"] as kotlinx.serialization.json.JsonObject
        assertEquals(
            "r1",
            (byDay["mon"] as kotlinx.serialization.json.JsonPrimitive).content,
        )
        val recipes = root["recipes"] as kotlinx.serialization.json.JsonArray
        assertEquals(1, recipes.size)
        val shoppingList = root["shoppingList"] as kotlinx.serialization.json.JsonObject
        val items = shoppingList["items"] as kotlinx.serialization.json.JsonArray
        assertEquals(1, items.size)
    }

    @Test
    fun roundTripsThroughImport() {
        val json = BackupCodec.buildBackupJson(
            AppSettings(AppColorScheme.LIGHT, 3),
            sampleSnapshot(),
            "2024-01-01T00:00:00.000Z",
        )
        val imported = BackupCodec.parseBackup(json, defaults)

        assertEquals("r1", imported.byDay[Weekday.MON])
        assertEquals(1, imported.recipes.size)
        assertEquals("Pasta", imported.recipes[0].name)
        assertEquals(listOf("salt"), imported.recipes[0].ingredients)
        assertEquals(1, imported.items.size)
        assertEquals("Milk", imported.items[0].label)
        assertEquals(AppColorScheme.LIGHT, imported.settings.colorScheme)
        assertEquals(3, imported.settings.portionsPerDay)
    }

    @Test
    fun rejectsInvalidJson() {
        try {
            BackupCodec.parseBackup("not json", defaults)
            fail("expected exception")
        } catch (e: BackupFormatException) {
            assertEquals("Could not parse JSON", e.message)
        }
    }

    @Test
    fun rejectsWrongFormat() {
        try {
            BackupCodec.parseBackup("""{"format":"other","version":1}""", defaults)
            fail("expected exception")
        } catch (e: BackupFormatException) {
            assertEquals("Not a valid backup file", e.message)
        }
    }

    @Test
    fun rejectsUnsupportedVersion() {
        try {
            BackupCodec.parseBackup("""{"format":"backup","version":99}""", defaults)
            fail("expected exception")
        } catch (e: BackupFormatException) {
            assertEquals("Unsupported backup version", e.message)
        }
    }

    @Test
    fun normalizesWeeklyPlanImport() {
        val raw = kotlinx.serialization.json.Json.parseToJsonElement(
            """{"byDay":{"mon":"recipe-1","tue":42,"wed":null}}""",
        )
        val result = BackupCodec.normalizeWeeklyPlanImport(raw)
        assertEquals("recipe-1", result[Weekday.MON])
        assertEquals(null, result[Weekday.TUE])
        assertEquals(null, result[Weekday.WED])
        assertEquals(null, result[Weekday.SUN])
    }

    @Test
    fun returnsEmptyPlanForInvalidInput() {
        val result = BackupCodec.normalizeWeeklyPlanImport(null)
        assertEquals(null, result[Weekday.MON])
        assertEquals(null, result[Weekday.SUN])
    }

    @Test
    fun sanitizesRecipesForImport() {
        val raw = kotlinx.serialization.json.Json.parseToJsonElement(
            """[
                {"id":"1","name":"Pasta","ingredients":["salt"]},
                {"id":"2","name":"Soup","ingredients":[{"quantity":"1 L","name":"water"}]},
                null
            ]""",
        )
        val result = BackupCodec.sanitizeRecipesForImport(raw)
        assertEquals(2, result.size)
        assertEquals("1 L water", result[1].ingredients[0])
    }

    @Test
    fun returnsEmptyRecipeListForNonArrayInput() {
        assertEquals(emptyList<Recipe>(), BackupCodec.sanitizeRecipesForImport(null))
    }

    @Test
    fun sanitizesItemsForImport() {
        val raw = kotlinx.serialization.json.Json.parseToJsonElement(
            """[
                {"id":"1","label":"Milk","checked":false},
                {"id":"2","name":"eggs","quantity":"6","checked":true}
            ]""",
        )
        val result = BackupCodec.sanitizeItemsForImport(raw)
        assertEquals(2, result.size)
        assertEquals("6 eggs", result[1].label)
        assertEquals(true, result[1].checked)
    }

    @Test
    fun backupErrorsFormatsImportFailures() {
        assertEquals(
            "That file is not a valid Weeklies backup.",
            BackupErrors.formatBackupError(BackupFormatException("Could not parse JSON"), BackupErrors.Kind.IMPORT),
        )
        assertEquals(
            "That backup version is not supported.",
            BackupErrors.formatBackupError(
                BackupFormatException("Unsupported backup version"),
                BackupErrors.Kind.IMPORT,
            ),
        )
    }

    @Test
    fun backupErrorsTreatsCancellationAsUserCanceled() {
        assertEquals(false, BackupErrors.isUserCanceled(null))
        assertEquals(
            "Export was canceled.",
            BackupErrors.formatBackupError(
                kotlinx.coroutines.CancellationException(),
                BackupErrors.Kind.EXPORT,
            ),
        )
    }
}
