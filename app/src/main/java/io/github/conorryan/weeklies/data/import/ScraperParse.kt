package io.github.conorryan.weeklies.data.import

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

data class ScrapedRecipeData(
    val url: String?,
    val name: String,
    val recipeYield: String?,
    val recipeIngredients: List<String>,
)

private fun cleanIngredientLine(line: String): String {
    val cleaned = line
        .replace("¼", "1/4")
        .replace("½", "1/2")
        .replace("¾", "3/4")
        .replace("⅔", "2/3")
        .replace(Regex("""<[^>]*>"""), "")
        .replace(Regex("""\s+"""), " ")
        .trim()
    return cleaned
}

private fun firstString(value: JsonElement?): String? {
    return when (value) {
        is JsonPrimitive -> value.content
        is JsonArray -> (value.firstOrNull() as? JsonPrimitive)?.content
        else -> null
    }
}

private fun ingredientObjectToLine(item: JsonObject): String? {
    val name = (item["name"] as? JsonPrimitive)?.content ?: return null
    val amount = (item["value"] as? JsonPrimitive)?.content
    val line = if (amount != null) "$amount $name" else name
    val cleaned = cleanIngredientLine(line)
    return cleaned.ifEmpty { null }
}

private fun transformIngredients(value: JsonElement?): List<String>? {
    if (value !is JsonArray) return null

    val lines = value.mapNotNull { item ->
        when (item) {
            is JsonPrimitive -> cleanIngredientLine(item.content).ifEmpty { null }
            is JsonObject -> ingredientObjectToLine(item)
            else -> null
        }
    }

    return lines.ifEmpty { null }
}

private fun buildScrapedRecipeData(props: JsonObject, url: String?): ScrapedRecipeData? {
    val name = firstString(props["name"])?.trim()
    val recipeYield = firstString(props["recipeYield"]) ?: firstString(props["yield"])
    val recipeIngredients = transformIngredients(
        props["recipeIngredient"] ?: props["recipeIngredients"] ?: props["ingredients"]
            ?: props["ingredient"],
    )

    if (name.isNullOrEmpty() || recipeIngredients.isNullOrEmpty()) return null

    return ScrapedRecipeData(
        url = url,
        name = name,
        recipeYield = recipeYield,
        recipeIngredients = recipeIngredients,
    )
}

/** Parse recipe data from JSON-LD embedded in HTML. */
fun parseRecipeHtml(html: String, url: String? = null): ScrapedRecipeData {
    val trimmedUrl = url?.trim()?.ifEmpty { null }

    for (node in extractJsonLdRecipeNodes(html)) {
        if (node !is JsonObject) continue
        buildScrapedRecipeData(node, trimmedUrl)?.let { return it }
    }

    throw RecipeImportErrors.noRecipeData()
}
