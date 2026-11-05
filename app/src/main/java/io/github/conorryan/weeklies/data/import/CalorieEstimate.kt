package io.github.conorryan.weeklies.data.import

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** How sure the model claims to be about its estimate; shown next to it, never stored. */
enum class CalorieConfidence { HIGH, MEDIUM, LOW }

/** One AI guess for a recipe's per-portion calories. */
data class CalorieEstimate(
    val calories: Int,
    val confidence: CalorieConfidence,
    /** One short sentence on how the number was reached. */
    val rationale: String,
)

private val CALORIE_SYSTEM_PROMPT = """
You estimate the calories of a recipe portion. Return only a JSON object matching the schema.

- The recipe's ingredient amounts are for exactly ONE portion; "calories" is the total energy of that portion, in kcal.
- Sum the energy of every ingredient (fats and oils included, even when the amount is "to taste"); account for what cooking loses or gains (fried food absorbs oil, water boils off).
- Estimate to the nearest 10 kcal.
- "confidence" is how sure you are of the estimate: high when most amounts are precise and energy-dense ingredients are explicit, medium when some amounts are vague but the dish type is familiar, low when amounts are mostly missing or unit conversions are guesswork. When in doubt, pick the lower level.
- "rationale" is one short sentence naming the biggest energy contributors or the main uncertainty.
""".trimIndent()

/** JSON schema sent to Mistral via `response_format: json_schema` for calorie estimates. */
val CALORIE_JSON_SCHEMA: JsonObject = buildJsonObject {
    put("type", "object")
    put("properties", buildJsonObject {
        put("calories", buildJsonObject { put("type", "integer") })
        put("confidence", buildJsonObject {
            put("type", "string")
            put("enum", buildJsonArray {
                add(JsonPrimitive("high"))
                add(JsonPrimitive("medium"))
                add(JsonPrimitive("low"))
            })
        })
        put("rationale", buildJsonObject { put("type", "string") })
    })
    put("required", buildJsonArray {
        add(JsonPrimitive("calories"))
        add(JsonPrimitive("confidence"))
        add(JsonPrimitive("rationale"))
    })
    put("additionalProperties", false)
}

fun buildCalorieUserMessage(
    recipeName: String,
    ingredients: List<String>,
    instructions: String,
): String {
    val lines = ingredients.joinToString("\n") { "- $it" }
    val steps = if (instructions.isBlank()) "" else "\n\nInstructions:\n${instructions.trim()}"
    return "Recipe: $recipeName\n\nIngredients (amounts are for one portion):\n$lines$steps"
}

/** Plausibility window for one portion; anything outside is treated as a bad response. */
const val MIN_ESTIMATED_CALORIES = 50
const val MAX_ESTIMATED_CALORIES = 5000

/**
 * Parses calorie-estimate response content; null when the content is not valid
 * JSON or its number is implausible for a single portion (the chat layer then
 * retries once). An unparsable confidence degrades to [CalorieConfidence.MEDIUM]
 * rather than losing the estimate.
 */
fun parseCalorieEstimateResponse(content: String): CalorieEstimate? {
    val data = try {
        Json.parseToJsonElement(content)
    } catch (e: Exception) {
        return null
    }
    val obj = data as? JsonObject ?: return null
    val calories = when (val c = obj["calories"]) {
        is JsonPrimitive -> c.content.toIntOrNull() ?: c.content.toDoubleOrNull()?.toInt()
        else -> null
    } ?: return null
    if (calories !in MIN_ESTIMATED_CALORIES..MAX_ESTIMATED_CALORIES) return null
    val confidence = when ((obj["confidence"] as? JsonPrimitive)?.content?.lowercase()) {
        "high" -> CalorieConfidence.HIGH
        "low" -> CalorieConfidence.LOW
        else -> CalorieConfidence.MEDIUM
    }
    val rationale = (obj["rationale"] as? JsonPrimitive)?.content?.trim() ?: ""
    return CalorieEstimate(calories = calories, confidence = confidence, rationale = rationale)
}

/**
 * Calls Mistral to guesstimate a recipe's per-portion calories with a
 * self-reported confidence level. The wire protocol and retry behavior live
 * in [MistralChat]; this class owns the estimator prompt, schema, and
 * response parsing.
 */
class CalorieEstimator(
    transport: HttpTransport,
    outputLanguage: () -> String = { DEFAULT_OUTPUT_LANGUAGE },
) {

    private val chat = MistralChat(transport, outputLanguage)

    suspend fun estimate(
        apiKey: String,
        recipeName: String,
        ingredients: List<String>,
        instructions: String,
    ): CalorieEstimate {
        val key = apiKey.trim()
        if (key.isEmpty()) throw RecipeImportErrors.mistralKeyMissing()

        return chat.chatForJson(
            apiKey = key,
            model = ImportConstants.MISTRAL_MODEL,
            schemaName = "calorie_estimate",
            systemPrompt = CALORIE_SYSTEM_PROMPT,
            userContent = buildCalorieUserMessage(recipeName, ingredients, instructions),
            schema = CALORIE_JSON_SCHEMA,
            parse = ::parseCalorieEstimateResponse,
        )
    }
}
