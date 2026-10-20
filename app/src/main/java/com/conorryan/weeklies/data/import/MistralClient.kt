package com.conorryan.weeklies.data.import

import com.conorryan.weeklies.data.model.NewRecipe
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

fun mapMistralHttpError(status: Int): RecipeImportException {
    return when {
        status == 401 -> RecipeImportErrors.mistralAuth()
        status == 429 -> RecipeImportErrors.mistralRateLimit()
        status >= 500 -> RecipeImportErrors.mistralUnavailable()
        else -> RecipeImportErrors.mistralFailed(status)
    }
}

/**
 * Calls Mistral's chat completions endpoint with a strict JSON schema and
 * returns a [NewRecipe] normalized to one serving, retrying once when the
 * response is not valid JSON.
 */
class MistralClient(private val transport: HttpTransport) {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun extractRecipe(
        apiKey: String,
        preprocessed: PreprocessedRecipePage,
        sourceUrl: String? = null,
    ): NewRecipe {
        val key = apiKey.trim()
        if (key.isEmpty()) throw RecipeImportErrors.mistralKeyMissing()

        var content = callMistralOnce(key, preprocessed, sourceUrl, retryFixJson = false)

        var result = parseMistralResponse(content)
        if (result is MistralParseOutcome.NoRecipe) throw RecipeImportErrors.noRecipeData()
        if (result == null) {
            content = callMistralOnce(key, preprocessed, sourceUrl, retryFixJson = true)
            result = parseMistralResponse(content)
            if (result is MistralParseOutcome.NoRecipe) throw RecipeImportErrors.noRecipeData()
        }

        val payload = (result as? MistralParseOutcome.Recipe)?.payload
            ?: throw RecipeImportErrors.mistralParse()

        return mistralPayloadToNewRecipe(
            payload,
            ImportConstants.RECIPE_IMPORT_TARGET_SERVINGS,
        ) ?: throw RecipeImportErrors.mistralParse()
    }

    private suspend fun callMistralOnce(
        apiKey: String,
        preprocessed: PreprocessedRecipePage,
        sourceUrl: String?,
        retryFixJson: Boolean,
    ): String {
        val userContent = if (retryFixJson) {
            "Your previous response was not valid JSON for the schema. " +
                "Reply with only valid JSON for the recipe schema, no markdown."
        } else {
            buildUserMessage(preprocessed, sourceUrl)
        }

        val requestBody = buildJsonObject {
            put("model", ImportConstants.MISTRAL_RECIPE_MODEL)
            put("temperature", 0.1)
            put("messages", buildJsonArray {
                add(buildJsonObject {
                    put("role", "system")
                    put("content", SYSTEM_PROMPT)
                })
                add(buildJsonObject {
                    put("role", "user")
                    put("content", userContent)
                })
            })
            put("response_format", buildJsonObject {
                put("type", "json_schema")
                put("json_schema", buildJsonObject {
                    put("name", "recipe_extract")
                    put("strict", true)
                    put("schema", RECIPE_JSON_SCHEMA)
                })
            })
        }.toString()

        val response = transport.execute(
            method = "POST",
            url = ImportConstants.MISTRAL_CHAT_URL,
            headers = mapOf(
                "Authorization" to "Bearer $apiKey",
                "Content-Type" to "application/json",
            ),
            body = requestBody,
        )

        if (response.status !in 200..299) throw mapMistralHttpError(response.status)

        val body = try {
            json.parseToJsonElement(response.bodyText) as? JsonObject
        } catch (e: Exception) {
            null
        }
        val content = (((body?.get("choices") as? JsonArray)
            ?.firstOrNull() as? JsonObject)
            ?.get("message") as? JsonObject)
            ?.get("content")?.let { (it as? JsonPrimitive)?.content }

        if (content.isNullOrBlank()) throw RecipeImportErrors.mistralEmpty()
        return content
    }
}
