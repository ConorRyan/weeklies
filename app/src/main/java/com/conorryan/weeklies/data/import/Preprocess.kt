package com.conorryan.weeklies.data.import

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray

data class PreprocessedRecipePage(
    val jsonLdRecipes: List<JsonElement>,
    val htmlSnippet: String,
)

private val SCRIPT_BLOCK = Regex(
    """<script\b[^<]*(?:(?!</script>)<[^<]*)*</script>""",
    setOf(RegexOption.IGNORE_CASE),
)

private val STYLE_BLOCK = Regex(
    """<style\b[^<]*(?:(?!</style>)<[^<]*)*</style>""",
    setOf(RegexOption.IGNORE_CASE),
)

private val JSON_LD_BLOCK = Regex(
    """<script[^>]*type\s*=\s*["']application/ld\+json["'][^>]*>([\s\S]*?)</script>""",
    setOf(RegexOption.IGNORE_CASE),
)

fun isRecipeSchemaType(typeValue: JsonElement?): Boolean {
    when (typeValue) {
        is JsonPrimitive -> {
            val s = typeValue.content
            return s == "Recipe" || s.endsWith("Recipe")
        }
        is JsonArray -> {
            return typeValue.jsonArray.any { el ->
                el is JsonPrimitive && (el.content == "Recipe" || el.content.endsWith("Recipe"))
            }
        }
        else -> return false
    }
}

private fun collectRecipeNodes(node: JsonElement?, out: MutableList<JsonElement>) {
    when (node) {
        null -> return
        is JsonArray -> {
            for (item in node) collectRecipeNodes(item, out)
        }
        is JsonObject -> {
            node["@graph"]?.let { collectRecipeNodes(it, out) }
            if (isRecipeSchemaType(node["@type"])) out.add(node)
        }
        else -> return
    }
}

/** Recipe-shaped JSON-LD nodes extracted from HTML script tags. */
fun extractJsonLdRecipeNodes(html: String): List<JsonElement> {
    val recipes = mutableListOf<JsonElement>()
    for (match in JSON_LD_BLOCK.findAll(html)) {
        val raw = match.groupValues[1].trim()
        if (raw.isEmpty()) continue
        try {
            val parsed = Json.parseToJsonElement(raw)
            collectRecipeNodes(parsed, recipes)
        } catch (e: Exception) {
            // skip invalid JSON-LD block
        }
    }
    return recipes
}

private fun capText(text: String, max: Int): String = if (text.length <= max) text else text.take(max)

fun preprocessRecipeHtml(html: String): PreprocessedRecipePage {
    val stripped = STYLE_BLOCK.replace(SCRIPT_BLOCK.replace(html, ""), "")
    val jsonLdRecipes = extractJsonLdRecipeNodes(html)
    val htmlSnippet = capText(Regex("""\s+""").replace(stripped, " ").trim(), ImportConstants.MAX_HTML_FOR_LLM)
    return PreprocessedRecipePage(jsonLdRecipes = jsonLdRecipes, htmlSnippet = htmlSnippet)
}
