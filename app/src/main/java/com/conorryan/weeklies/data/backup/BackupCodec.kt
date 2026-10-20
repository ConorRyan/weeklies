package com.conorryan.weeklies.data.backup

import com.conorryan.weeklies.data.model.AppColorScheme
import com.conorryan.weeklies.data.model.AppSettings
import com.conorryan.weeklies.data.model.DayAssignments
import com.conorryan.weeklies.data.model.Recipe
import com.conorryan.weeklies.data.model.ShoppingItem
import com.conorryan.weeklies.data.model.Weekday
import com.conorryan.weeklies.data.model.emptyByDay
import com.conorryan.weeklies.data.model.isValidPortionsPerDay
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Everything needed to build a backup payload. */
data class BackupSnapshot(
    val byDay: DayAssignments,
    val recipes: List<Recipe>,
    val items: List<ShoppingItem>,
)

/** Normalized data extracted from a backup file. */
data class ImportedBackup(
    val byDay: DayAssignments,
    val recipes: List<Recipe>,
    val items: List<ShoppingItem>,
    val settings: AppSettings,
)

class BackupFormatException(message: String) : Exception(message)

/**
 * Encodes and decodes the Weeklies backup format. The wire format is identical
 * to the React Native app so existing backup files keep working:
 *
 * `{ format: "backup", version: 1, exportedAt, settings, weeklyPlan, recipes, shoppingList }`
 */
object BackupCodec {

    const val FORMAT = "backup"
    const val VERSION = 1

    private val json = Json { prettyPrint = true; prettyPrintIndent = "  " }
    private val filenameFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    fun defaultBackupFilename(date: LocalDate = LocalDate.now()): String =
        "backup-${date.format(filenameFormat)}.json"

    fun buildBackupJson(settings: AppSettings, snapshot: BackupSnapshot, exportedAt: String): String {
        val payload = buildJsonObject {
            put("format", FORMAT)
            put("version", VERSION)
            put("exportedAt", exportedAt)
            put("settings", buildJsonObject {
                put("colorScheme", settings.colorScheme.key)
                put("portionsPerDay", settings.portionsPerDay)
                put("settingsSchemaVersion", 1)
            })
            put("weeklyPlan", buildJsonObject {
                put("byDay", buildJsonObject {
                    for (day in Weekday.entries) {
                        put(day.key, snapshot.byDay[day]?.let { JsonPrimitive(it) } ?: JsonNull)
                    }
                })
            })
            put("recipes", buildJsonArray {
                for (recipe in snapshot.recipes) {
                    add(buildJsonObject {
                        put("id", recipe.id)
                        put("name", recipe.name)
                        put("ingredients", JsonArray(recipe.ingredients.map { JsonPrimitive(it) }))
                    })
                }
            })
            put("shoppingList", buildJsonObject {
                put("items", buildJsonArray {
                    for (item in snapshot.items) {
                        add(buildJsonObject {
                            put("id", item.id)
                            put("label", item.label)
                            put("checked", item.checked)
                        })
                    }
                })
            })
        }
        return json.encodeToString(JsonElement.serializer(), payload)
    }

    /** Parses and normalizes a backup file; throws [BackupFormatException] on bad input. */
    fun parseBackup(rawJson: String, defaults: AppSettings): ImportedBackup {
        val root = try {
            json.parseToJsonElement(rawJson).jsonObject
        } catch (e: BackupFormatException) {
            throw e
        } catch (e: Exception) {
            throw BackupFormatException("Could not parse JSON")
        }

        if (root["format"]?.jsonPrimitive?.content != FORMAT) {
            throw BackupFormatException("Not a valid backup file")
        }
        val version = root["version"]?.jsonPrimitive?.intOrNullSafe()
        if (version == null || version != VERSION) {
            throw BackupFormatException("Unsupported backup version")
        }

        return ImportedBackup(
            byDay = normalizeWeeklyPlanImport(root["weeklyPlan"]),
            recipes = sanitizeRecipesForImport(root["recipes"]),
            items = sanitizeItemsForImport(
                (root["shoppingList"] as? JsonObject)?.get("items"),
            ),
            settings = parseStoredSettings(root["settings"], defaults),
        )
    }

    fun normalizeWeeklyPlanImport(raw: JsonElement?): DayAssignments {
        val next = emptyByDay().toMutableMap()
        val byDay = (raw as? JsonObject)?.get("byDay") as? JsonObject ?: return next.toMap()
        for (day in Weekday.entries) {
            val value = byDay[day.key]
            next[day] = when {
                value is JsonPrimitive && value.isString -> value.content
                else -> null
            }
        }
        return next.toMap()
    }

    fun sanitizeRecipesForImport(raw: JsonElement?): List<Recipe> {
        val array = raw as? JsonArray ?: return emptyList()
        return array.mapNotNull(::normalizeRecipe)
    }

    /** Accepts string lines and `{ name, quantity }` objects, like the RN app did. */
    fun normalizeRecipe(value: JsonElement?): Recipe? {
        val obj = value as? JsonObject ?: return null
        val id = obj["id"]?.jsonPrimitive?.contentOrNull() ?: return null
        val name = obj["name"]?.jsonPrimitive?.contentOrNull() ?: return null
        val ingredientsArray = obj["ingredients"] as? JsonArray ?: return null
        val ingredients = ingredientsArray.mapNotNull(::ingredientElementToLine)
        return Recipe(id = id, name = name, ingredients = ingredients)
    }

    fun sanitizeItemsForImport(raw: JsonElement?): List<ShoppingItem> {
        val array = raw as? JsonArray ?: return emptyList()
        return array.mapNotNull(::normalizeItem)
    }

    /** Accepts `label` or `name` + `quantity`, like the RN app did. */
    fun normalizeItem(value: JsonElement?): ShoppingItem? {
        val obj = value as? JsonObject ?: return null
        val id = obj["id"]?.jsonPrimitive?.contentOrNull() ?: return null
        val checked = obj["checked"]?.jsonPrimitive?.booleanOrNull ?: return null

        val label = obj["label"]?.jsonPrimitive?.contentOrNull()?.trim()
        if (label != null) {
            if (label.isEmpty()) return null
            return ShoppingItem(id = id, label = label, checked = checked)
        }

        val name = obj["name"]?.jsonPrimitive?.contentOrNull()?.trim() ?: return null
        val quantity = obj["quantity"]?.jsonPrimitive?.contentOrNull()?.trim() ?: ""
        val line = listOf(quantity, name).filter { it.isNotEmpty() }.joinToString(" ")
        val resolved = if (line.isEmpty()) name else line
        if (resolved.isEmpty()) return null
        return ShoppingItem(id = id, label = resolved, checked = checked)
    }

    /** Tolerant settings import: anything missing or invalid falls back to defaults. */
    fun parseStoredSettings(raw: JsonElement?, defaults: AppSettings): AppSettings {
        val obj = raw as? JsonObject ?: return defaults
        val scheme = obj["colorScheme"]?.jsonPrimitive?.contentOrNull()
            ?.let { AppColorScheme.fromKey(it) }
        val portions = obj["portionsPerDay"]?.jsonPrimitive?.doubleOrNullSafe()
        return AppSettings(
            colorScheme = scheme ?: defaults.colorScheme,
            portionsPerDay = if (portions != null && isValidPortionsPerDay(portions)) {
                portions.toInt()
            } else {
                defaults.portionsPerDay
            },
        )
    }

    private fun ingredientElementToLine(item: JsonElement): String? {
        when (item) {
            is JsonPrimitive -> {
                val t = item.contentOrNull()?.trim() ?: return null
                return t.ifEmpty { null }
            }
            is JsonObject -> {
                val name = item["name"]?.jsonPrimitive?.contentOrNull()?.trim() ?: ""
                val quantity = item["quantity"]?.jsonPrimitive?.contentOrNull()?.trim() ?: ""
                val line = listOf(quantity, name).filter { it.isNotEmpty() }.joinToString(" ")
                return line.ifEmpty { null }
            }
            else -> return null
        }
    }

    private fun JsonPrimitive.contentOrNull(): String? =
        if (this is JsonNull) null else content

    private fun JsonPrimitive.intOrNullSafe(): Int? =
        if (this is JsonNull) null
        else content.toIntOrNull() ?: content.toDoubleOrNull()?.let { runCatching { it.toInt() }.getOrNull() }

    private fun JsonPrimitive.doubleOrNullSafe(): Double? =
        if (this is JsonNull) null
        else content.toDoubleOrNull()?.takeIf { !it.isNaN() && !it.isInfinite() }
}
