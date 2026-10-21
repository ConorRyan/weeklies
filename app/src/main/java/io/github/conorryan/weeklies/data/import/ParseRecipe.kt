package io.github.conorryan.weeklies.data.import

import io.github.conorryan.weeklies.data.model.NewRecipe
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

data class MistralIngredient(val quantity: String?, val name: String)

data class MistralRecipePayload(
    val name: String,
    val servings: Double,
    val ingredients: List<MistralIngredient>,
)

fun isValidServings(n: Double): Boolean = !n.isNaN() && !n.isInfinite() && n > 0

/** Scale ingredient lines from recipe yield to a target number of servings. */
fun scaleIngredientsToServings(
    ingredients: List<String>,
    recipeServings: Double,
    targetServings: Double,
): List<String> {
    if (!isValidServings(recipeServings) || !isValidServings(targetServings)) return ingredients
    if (recipeServings == targetServings) return ingredients
    val factor = targetServings / recipeServings
    return ingredients.map { scaleIngredientLine(it, factor) }
}

fun ingredientPartsToLine(quantity: String?, name: String): String? {
    val n = name.trim()
    if (n.isEmpty()) return null
    val q = quantity?.trim() ?: ""
    val line = listOf(q, n).filter { it.isNotEmpty() }.joinToString(" ")
    return line.ifEmpty { null }
}

private fun isMistralIngredient(value: JsonElement): Boolean =
    value is JsonObject && value["name"] is JsonPrimitive

fun parseMistralRecipePayload(data: JsonElement?): MistralRecipePayload? {
    val o = data as? JsonObject ?: return null
    val name = (o["name"] as? JsonPrimitive)?.content ?: return null
    val ingredientsArray = o["ingredients"] as? JsonArray ?: return null
    val ingredients = ingredientsArray.filter { isMistralIngredient(it) }
        .mapNotNull { el ->
            val obj = el as JsonObject
            val n = (obj["name"] as? JsonPrimitive)?.content ?: return@mapNotNull null
            val q = (obj["quantity"] as? JsonPrimitive)?.content
            MistralIngredient(quantity = q, name = n)
        }
    if (ingredients.isEmpty()) return null

    val servings = when (val s = o["servings"]) {
        is JsonPrimitive -> s.content.toDoubleOrNull() ?: return null
        else -> return null
    }
    if (!isValidServings(servings)) return null

    return MistralRecipePayload(name = name.trim(), servings = servings, ingredients = ingredients)
}

fun mistralPayloadToNewRecipe(
    payload: MistralRecipePayload,
    targetServings: Int? = null,
): NewRecipe? {
    val name = payload.name.trim()
    if (name.isEmpty()) return null

    var ingredients = payload.ingredients.mapNotNull { item ->
        ingredientPartsToLine(item.quantity, item.name)
    }
    if (ingredients.isEmpty()) return null

    if (targetServings != null && isValidServings(targetServings.toDouble())) {
        ingredients = scaleIngredientsToServings(
            ingredients,
            payload.servings,
            targetServings.toDouble(),
        )
    }
    return NewRecipe(name = name, ingredients = ingredients)
}

sealed class MistralParseOutcome {
    data class Recipe(val payload: MistralRecipePayload) : MistralParseOutcome()
    data object NoRecipe : MistralParseOutcome()
}

/** Parse Mistral JSON content, distinguishing "no recipe on page" from invalid JSON. */
fun parseMistralResponse(content: String): MistralParseOutcome? {
    val data = try {
        Json.parseToJsonElement(content)
    } catch (e: Exception) {
        return null
    }
    if (data !is JsonObject) return null

    if ((data["found"] as? JsonPrimitive)?.content == "false") return MistralParseOutcome.NoRecipe

    val payload = parseMistralRecipePayload(data) ?: return null
    return MistralParseOutcome.Recipe(payload)
}
