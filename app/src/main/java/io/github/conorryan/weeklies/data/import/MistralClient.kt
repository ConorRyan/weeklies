package io.github.conorryan.weeklies.data.import

import io.github.conorryan.weeklies.data.model.NewRecipe
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private val SYSTEM_PROMPT = """
You extract recipe data from web page content.
Return only a JSON object matching the schema.

"found": set to true ONLY when the page is a food recipe with a genuine ingredient list for cooking a dish.
Set found to false when the page is not a recipe — e.g. homepages, news, blogs, shop pages, login screens, search results, or any page without recipe ingredients.
When found is false, return name as "", servings as 1, and ingredients as [].

Do NOT treat navigation links, menu items, page headers, footers, categories, or section titles as recipe name or ingredients.
Do NOT invent a recipe from page chrome or marketing copy.

When found is true:
- "name" is the recipe dish title (not the site name or page heading).
- "servings" is how many people the listed ingredient amounts are for (e.g. from "Serves 4", recipeYield, or portion count). Use a positive number; if only a range is given, use the lower number.
- Each ingredient has "name" (required) and optional "quantity" (amount and unit, e.g. "200 g", "1/2 cup").
- Do not include cooking steps, notes, or equipment.
- If quantities appear in the source, put them in "quantity" so they can be scaled later.
- Prefer fewer, accurate lines over guessing.
""".trimIndent()

/** JSON schema sent to Mistral via `response_format: json_schema`. */
val RECIPE_JSON_SCHEMA: JsonObject = buildJsonObject {
    put("type", "object")
    put("properties", buildJsonObject {
        put("found", buildJsonObject {
            put("type", "boolean")
            put(
                "description",
                "True only when the page contains a cookable food recipe with a real ingredient list.",
            )
        })
        put("name", buildJsonObject { put("type", "string") })
        put("servings", buildJsonObject { put("type", "number") })
        put("ingredients", buildJsonObject {
            put("type", "array")
            put("items", buildJsonObject {
                put("properties", buildJsonObject {
                    put("quantity", buildJsonObject { put("type", "string") })
                    put("name", buildJsonObject { put("type", "string") })
                })
                put("required", buildJsonArray { add(JsonPrimitive("name")) })
                put("additionalProperties", false)
                put("type", "object")
            })
        })
    })
    put("required", buildJsonArray {
        add(JsonPrimitive("found"))
        add(JsonPrimitive("name"))
        add(JsonPrimitive("servings"))
        add(JsonPrimitive("ingredients"))
    })
    put("additionalProperties", false)
}

fun buildUserMessage(preprocessed: PreprocessedRecipePage, sourceUrl: String?): String {
    val parts = mutableListOf<String>()
    if (sourceUrl != null) parts.add("Source URL: $sourceUrl")
    if (preprocessed.jsonLdRecipes.isNotEmpty()) {
        parts.add(
            "JSON-LD Recipe data:\n" +
                Json.encodeToString(
                    JsonElement.serializer(),
                    JsonArray(preprocessed.jsonLdRecipes.take(3)),
                ),
        )
    }
    parts.add("Page content:\n${preprocessed.htmlSnippet}")
    return parts.joinToString("\n\n")
}

/**
 * Calls Mistral's chat completions endpoint with a strict JSON schema and
 * returns a [NewRecipe] normalized to one serving. The wire protocol and
 * retry behavior live in [MistralChat]; this class owns the import prompt,
 * schema, and response parsing.
 */
class MistralClient(transport: HttpTransport) {

    private val chat = MistralChat(transport)

    suspend fun extractRecipe(
        apiKey: String,
        preprocessed: PreprocessedRecipePage,
        sourceUrl: String? = null,
    ): NewRecipe {
        val key = apiKey.trim()
        if (key.isEmpty()) throw RecipeImportErrors.mistralKeyMissing()

        val payload = chat.chatForJson(
            apiKey = key,
            model = ImportConstants.MISTRAL_RECIPE_MODEL,
            schemaName = "recipe_extract",
            systemPrompt = SYSTEM_PROMPT,
            userContent = buildUserMessage(preprocessed, sourceUrl),
            schema = RECIPE_JSON_SCHEMA,
        ) { content ->
            when (val result = parseMistralResponse(content)) {
                is MistralParseOutcome.NoRecipe -> throw RecipeImportErrors.noRecipeData()
                is MistralParseOutcome.Recipe -> result.payload
                null -> null
            }
        }

        return mistralPayloadToNewRecipe(
            payload,
            ImportConstants.RECIPE_IMPORT_TARGET_SERVINGS,
        ) ?: throw RecipeImportErrors.mistralParse()
    }
}
