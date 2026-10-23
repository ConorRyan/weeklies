package io.github.conorryan.weeklies.data.import

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

fun mapMistralHttpError(status: Int): RecipeImportException {
    return when {
        status == 401 -> RecipeImportErrors.mistralAuth()
        status == 429 -> RecipeImportErrors.mistralRateLimit()
        status >= 500 -> RecipeImportErrors.mistralUnavailable()
        else -> RecipeImportErrors.mistralFailed(status)
    }
}

/**
 * One strict-schema chat request to Mistral's chat completions endpoint.
 *
 * This is the single place that knows the wire protocol: request-body shape,
 * temperature, `response_format: json_schema`, HTTP error mapping, content
 * extraction, the empty-response guard, and the retry-once-when-the-parse-
 * fails loop. Recipe import (MistralClient) and recipe cleanup (RecipeCleaner)
 * both sit on this module; only their prompts, schemas, and parse functions
 * differ.
 */
class MistralChat(private val transport: HttpTransport) {

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Sends [userContent] once and hands the returned content to [parse].
     * When [parse] returns null (not valid JSON for the schema), retries once
     * asking for valid JSON only; throws `mistralParse` when that fails too.
     * [parse] may throw directly to abort without retrying.
     */
    suspend fun <T : Any> chatForJson(
        apiKey: String,
        model: String,
        schemaName: String,
        systemPrompt: String,
        userContent: String,
        schema: JsonObject,
        parse: (String) -> T?,
    ): T {
        var content = chatOnce(apiKey, model, schemaName, systemPrompt, userContent, schema)
        var result = parse(content)
        if (result == null) {
            content = chatOnce(apiKey, model, schemaName, systemPrompt, RETRY_FIX_JSON_MESSAGE, schema)
            result = parse(content)
        }
        return result ?: throw RecipeImportErrors.mistralParse()
    }

    /** Sends one chat request and returns the `choices[0].message.content` text. */
    private suspend fun chatOnce(
        apiKey: String,
        model: String,
        schemaName: String,
        systemPrompt: String,
        userContent: String,
        schema: JsonObject,
    ): String {
        val requestBody = buildJsonObject {
            put("model", model)
            put("temperature", 0.1)
            put("messages", buildJsonArray {
                add(buildJsonObject {
                    put("role", "system")
                    put("content", systemPrompt)
                })
                add(buildJsonObject {
                    put("role", "user")
                    put("content", userContent)
                })
            })
            put("response_format", buildJsonObject {
                put("type", "json_schema")
                put("json_schema", buildJsonObject {
                    put("name", schemaName)
                    put("strict", true)
                    put("schema", schema)
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

    private companion object {
        val RETRY_FIX_JSON_MESSAGE =
            "Your previous response was not valid JSON for the schema. " +
                "Reply with only valid JSON for the schema, no markdown."
    }
}
