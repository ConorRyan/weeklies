package io.github.conorryan.weeklies.data.import

import io.github.conorryan.weeklies.data.quantity.NUMERIC_TOKEN
import io.github.conorryan.weeklies.data.quantity.ingredientPartsToLine
import io.github.conorryan.weeklies.data.quantity.parseNumericToken
import kotlin.math.abs
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private val CLEANUP_SYSTEM_PROMPT = """
You clean up a recipe's ingredient list. Return only a JSON object matching the schema.

- Keep every ingredient: do not add or remove any. Preserve the original order, except when you merge duplicates or split a line that lists several ingredients ("salt and pepper, to taste" may become one line per ingredient).
- If the same ingredient appears on multiple lines, combine them into a single line.
- Each ingredient must end up as one line with exactly one unit (e.g. "250 g flour", never "2 cups plus 1 tbsp flour").
- The quantity is a single amount and unit: never append a second, equivalent amount after a dash, slash, equals sign, or in parentheses ("0.5 tbsp", never "0.5 tbsp - 7 g" or "0.5 tbsp (7 g)").
- Prefer metric units (g, ml). Convert US or imperial units when a standard conversion exists.
- For volume-to-mass conversions, use this table of weights per 1 US cup and never guess a different value:
  all-purpose or bread flour: 120 g; whole-wheat flour: 120 g; granulated sugar: 200 g; packed brown sugar: 220 g; powdered sugar: 120 g; butter or margarine: 227 g; cocoa powder: 100 g; rolled oats: 90 g; uncooked white rice: 190 g; chocolate chips: 170 g; chopped nuts: 140 g; shredded or grated cheese: 110 g; grated zucchini: 110 g; mashed banana: 225 g; honey: 340 g; maple syrup: 320 g.
  Liquids (milk, water, cream, oil, broth) convert by volume, not mass: 1 cup = 240 ml, 1 fl oz = 30 ml, 1 pint = 480 ml, 1 quart = 960 ml.
  If the ingredient is not in the table and you are not confident of its weight per cup, keep the original unit as-is ("1 cup shredded coconut" stays in cups) rather than inventing a conversion.
- Keep tablespoons and teaspoons as they are: do not convert tbsp/tsp to milliliters ("2 tbsp olive oil" stays "2 tbsp olive oil").
- Round amounts, but never by more than 10%: if a value is within 10% of a rounder value (a whole number, a simple multiple of 10, or a simple fraction for small amounts), use the rounder value; otherwise keep the exact value ("0.333 onion" stays "0.333 onion", never "1 onion").
- Write amounts as decimals, not fractions: "0.5 cup", never "1/2 cup"; "1.5 tbsp", never "1 1/2 tbsp".
- If a countable ingredient has no amount, give it quantity "1" (e.g. "1 green onion"). Never do the reverse: an ingredient that already has an amount keeps that amount (rounded at most 10%), even when it is a fraction of a countable ingredient. Keep the quantity empty only for to-taste items like salt and pepper.
- Never invent a unit: if the original amount has no unit ("20 olives"), keep it without one — do not write "20 g olives". Only add a unit when converting from another unit that is already there.
- Put preparation notes (chopped, trimmed, halved, grated…) in parentheses at the end of the name: "250 g mushrooms (trimmed)", "1 onion (finely chopped)". Parentheses are never for units — a unit word (clove, stem, head, bunch, wedge…) always stays in the quantity: "1 clove garlic", "2 stem green onion", never "1 garlic (clove)" or "3 green onion (stem)".
- Never change an amount except by unit conversion or by summing lines you are actually merging — one input line in, one output line out, same count.
- Each cleaned line must also carry "from": the zero-based positions of the original ingredient lines it came from (the first is 0). A merged line lists every original position it combined, in ascending order; a split line repeats the position of the original it came from. Every original position must be used at least once.
- Always write unit words in the quantity in singular form, even for amounts above 1 ("2 clove garlic", "1.5 cup milk"). Metric unit symbols (g, ml) never change.
- "name" is the ingredient without its amount and must not repeat the unit the quantity already carries ("1 clove garlic", never "1 clove garlic cloves").
- "quantity" is the amount with its unit (e.g. "250 g").
""".trimIndent()

/** JSON schema sent to Mistral via `response_format: json_schema` for cleanup. */
val CLEANUP_JSON_SCHEMA: JsonObject = buildJsonObject {
    put("type", "object")
    put("properties", buildJsonObject {
        put("ingredients", buildJsonObject {
            put("type", "array")
            put("items", buildJsonObject {
                put("properties", buildJsonObject {
                    put("name", buildJsonObject { put("type", "string") })
                    put("quantity", buildJsonObject { put("type", "string") })
                    put("from", buildJsonObject {
                        put("type", "array")
                        put("items", buildJsonObject { put("type", "integer") })
                    })
                })
                put("required", buildJsonArray {
                    add(JsonPrimitive("name"))
                    add(JsonPrimitive("quantity"))
                    add(JsonPrimitive("from"))
                })
                put("additionalProperties", false)
                put("type", "object")
            })
        })
    })
    put("required", buildJsonArray { add(JsonPrimitive("ingredients")) })
    put("additionalProperties", false)
}

fun buildCleanupUserMessage(recipeName: String, ingredients: List<String>): String {
    val lines = ingredients.joinToString("\n") { "- $it" }
    return "Recipe: $recipeName\n\nIngredients to clean up:\n$lines"
}

private val INSTRUCTIONS_CLEANUP_SYSTEM_PROMPT = """
You rewrite a recipe's instructions as a concise checklist. Return only a JSON object matching the schema.

- Rewrite the instructions as one string of bullet points: one step per line, each line starting with "- ".
- The whole list must be readable in about two minutes: 5 to 15 bullets, each a short concrete action with its key time, temperature, or amount.
- Keep every essential step and its order. Do not invent, skip, or reorder steps.
- Keep all times, temperatures, and amounts exactly as the original states them; never invent new ones.
- Cut fluff: stories, introductions, serving suggestions, storage tips, nutrition notes, and sign-offs.
""" + "\n"

/** JSON schema sent to Mistral via `response_format: json_schema` for instructions cleanup. */
val INSTRUCTIONS_CLEANUP_JSON_SCHEMA: JsonObject = buildJsonObject {
    put("type", "object")
    put("properties", buildJsonObject {
        put("instructions", buildJsonObject { put("type", "string") })
    })
    put("required", buildJsonArray { add(JsonPrimitive("instructions")) })
    put("additionalProperties", false)
}

fun buildInstructionsCleanupUserMessage(
    recipeName: String,
    ingredients: List<String>,
    instructions: String,
): String {
    val lines = ingredients.joinToString("\n") { "- $it" }
    return "Recipe: $recipeName\n\nIngredients:\n$lines\n\nInstructions to clean up:\n$instructions"
}

/**
 * Parses instructions-cleanup response content into the rewritten instruction
 * text; null when the content is not valid JSON or carries no instructions.
 */
fun parseInstructionsCleanupResponse(content: String): String? {
    val data = try {
        Json.parseToJsonElement(content)
    } catch (e: Exception) {
        return null
    }
    val text = (data as? JsonObject)?.get("instructions") as? JsonPrimitive ?: return null
    val trimmed = text.content.trim()
    return trimmed.ifEmpty { null }
}

/** Irregular plurals the strip check would otherwise miss, mapped to singular. */
private val IRREGULAR_SINGULARS = mapOf("leaves" to "leaf", "loaves" to "loaf")

/**
 * Backstop for the cleanup prompt's no-duplicate-unit rule: strips a unit
 * word repeated at the end of the ingredient name, matching singular and
 * plural forms (including common irregular plurals). Quantity "1 clove" with
 * name "garlic cloves" becomes "garlic", so the line reads "1 clove garlic"
 * instead of "1 clove garlic cloves". The name is kept unchanged when nothing
 * would be left after stripping.
 */
fun removeDuplicatedUnit(quantity: String, name: String): String {
    val tokens = quantity.trim().split(Regex("""\s+"""))
    val unit = tokens.lastOrNull()?.lowercase() ?: return name
    if (tokens.size < 2 || !unit.matches(Regex("""[a-zA-Z]+"""))) return name

    val words = name.trim().split(Regex("""\s+"""))
    val lastWord = words.lastOrNull()?.lowercase() ?: return name
    val singular = IRREGULAR_SINGULARS[lastWord] ?: lastWord
    val isRepeatedUnit = singular == unit || lastWord in setOf(unit, unit + "s", unit + "es")
    if (!isRepeatedUnit) return name

    val stripped = words.dropLast(1).joinToString(" ")
    return stripped.ifEmpty { name }
}

/** Units (singular) a model may wrongly wrap in parentheses at the end of a name. */
private val PARENTHESIZED_UNIT_WORDS = setOf(
    "g", "gram", "kg", "mg", "ml", "l", "liter", "litre",
    "cup", "tbsp", "tablespoon", "tsp", "teaspoon",
    "oz", "ounce", "lb", "pound",
    "clove", "slice", "can", "pinch", "sprig", "leaf", "stick",
    "head", "bunch", "wedge", "stem", "piece", "packet", "tin", "dash",
)

/**
 * Backstop for the cleanup prompt's parentheses rule: when the model moves the
 * unit into a trailing parenthesis of the name ("garlic (clove)") while the
 * quantity is a bare number, the unit goes back into the quantity, so the
 * line reads "1 clove garlic" instead of "1 garlic (clove)". Preparation
 * notes ("mushrooms (trimmed)") and parenthesized words that are not units
 * are left alone, as is anything whose quantity already carries a unit.
 */
fun moveParenthesizedUnitToQuantity(quantity: String, name: String): Pair<String, String> {
    val trimmed = name.trim()
    val match = Regex("""^(.+?)\s*\(([a-zA-Z]+)\)$""").find(trimmed)
        ?: return quantity to name
    val base = match.groupValues[1].trim()
    if (base.isEmpty()) return quantity to name
    if (!quantity.trim().matches(Regex("""\d*\.?\d+"""))) return quantity to name

    val word = match.groupValues[2].lowercase()
    val unit = listOfNotNull(
        word,
        IRREGULAR_SINGULARS[word],
        word.dropLast(1).takeIf { word.endsWith("s") },
        word.dropLast(2).takeIf { word.endsWith("es") },
    ).firstOrNull { it in PARENTHESIZED_UNIT_WORDS } ?: return quantity to name

    return "${quantity.trim()} $unit" to base
}

/**
 * Backstop for the cleanup prompt's one-unit rule: the model sometimes keeps a
 * unit and appends its metric equivalent ("0.5 tbsp - 7 g"); only the first
 * amount survives, so the line reads "0.5 tbsp soy sauce". A range whose only
 * unit sits at the end ("1 - 2 tbsp") and anything without a second amount
 * after a dash, slash, equals sign, or parenthesis is left alone.
 */
fun stripAlternateQuantity(quantity: String): String {
    val match = Regex(
        """^(.*?\d+(?:[.,]\d+)?\s*[a-zA-Z]+.*?)\s*(?:[-–=/]|\()\s*(?:[a-zA-Z]+\s+)?\d+(?:[.,]\d+)?\s*[a-zA-Z]+\.?\)?\s*$""",
    ).find(quantity.trim()) ?: return quantity.trim()
    return match.groupValues[1].trim()
}

/** Max relative amount change rounding is allowed to make. */
private const val ROUNDING_TOLERANCE = 0.10

/**
 * Backstop for the cleanup prompt's rounding rule: on a 1:1 line whose unit
 * word is unchanged, the amount may move at most 10% ([ROUNDING_TOLERANCE]);
 * a bigger jump (the model turning "0.333 onion" into "1 onion") puts the
 * original amount back into the cleaned line. Merges, splits (an original
 * claimed by several lines), unit conversions, and lines without a comparable
 * number are left alone.
 */
fun clampRounding(before: List<String>, cleaned: CleanedIngredients): CleanedIngredients {
    if (!isValidCleanupMapping(cleaned.from, before.size)) return cleaned
    val claims = IntArray(before.size)
    cleaned.from.forEach { origins -> origins.forEach { claims[it]++ } }

    val lines = cleaned.lines.mapIndexed { index, line ->
        val origins = cleaned.from[index]
        if (origins.size != 1) return@mapIndexed line
        val origin = origins[0]
        if (claims[origin] != 1) return@mapIndexed line
        clampLine(before[origin], line) ?: line
    }
    return cleaned.copy(lines = lines)
}

/** Original amount restored into [cleaned] when it jumped too far; null to keep [cleaned]. */
private fun clampLine(original: String, cleaned: String): String? {
    val originalNumber = NUMERIC_TOKEN.find(original) ?: return null
    val cleanedNumber = NUMERIC_TOKEN.find(cleaned) ?: return null
    val originalValue = parseNumericToken(originalNumber.value) ?: return null
    val cleanedValue = parseNumericToken(cleanedNumber.value) ?: return null
    if (originalValue == 0.0 || cleanedValue == 0.0) return null

    val change = abs(cleanedValue - originalValue) / originalValue
    if (change <= ROUNDING_TOLERANCE) return null
    if (amountUnit(original, originalNumber) != amountUnit(cleaned, cleanedNumber)) return null

    return cleaned.replaceRange(cleanedNumber.range, originalNumber.value)
}

/** The word right after the amount, singularized; null when the amount ends the line. */
private fun amountUnit(line: String, number: MatchResult): String? {
    val word = Regex("""[a-zA-Z]+""").find(line.substring(number.range.last + 1))
        ?.value?.lowercase() ?: return null
    IRREGULAR_SINGULARS[word]?.let { return it }
    if (word.endsWith("es")) return word.dropLast(2)
    if (word.endsWith("s")) return word.dropLast(1)
    return word
}

/** One review row: the original line(s) and the cleaned line(s) they align to. */
data class CleanupPairing(val before: List<String>, val after: List<String>)

/** Unit and size words ignored when judging whether two lines name the same ingredient. */
private val UNIT_TOKENS = setOf(
    "g", "gram", "grams", "kg", "mg", "ml", "l", "liter", "liters", "litre", "litres",
    "cup", "cups", "tbsp", "tbs", "tablespoon", "tablespoons", "tsp", "teaspoon", "teaspoons",
    "oz", "ounce", "ounces", "lb", "lbs", "pound", "pounds",
    "clove", "cloves", "slice", "slices", "can", "cans", "pinch", "pinches",
    "sprig", "sprigs", "leaf", "leaves", "stick", "sticks",
    "large", "small", "medium",
)

private fun lineTokens(line: String): Set<String> =
    Regex("""[a-zA-Z]+""").findAll(line).map { it.value.lowercase() }.filter { it !in UNIT_TOKENS }.toSet()

private fun tokenSimilarity(a: Set<String>, b: Set<String>): Double {
    if (a.isEmpty() && b.isEmpty()) return 1.0
    if (a.isEmpty() || b.isEmpty()) return 0.0
    return a.intersect(b).size.toDouble() / a.union(b).size
}

/**
 * Weakest similarity between any line of [aLines] and any line of [bLines], so
 * bundling unrelated ingredients into one candidate scores near zero while a
 * genuine merge (same ingredient on every line) keeps its full score.
 */
private fun crossSimilarity(aLines: List<String>, bLines: List<String>): Double {
    if (aLines.isEmpty() || bLines.isEmpty()) return 0.0
    val bTokens = bLines.map { lineTokens(it) }
    return aLines.minOf { a -> bTokens.minOf { b -> tokenSimilarity(lineTokens(a), b) } }
}

/** True for a 1:1 pairing the cleanup left exactly as it was. */
fun isUnchangedPairing(pairing: CleanupPairing): Boolean =
    pairing.before.size == 1 && pairing.after.size == 1 && pairing.before[0] == pairing.after[0]

/**
 * True when the model-declared mapping is trustworthy: every index is in
 * range, no line claims the same original twice, every line claims at least
 * one original, and every original is covered. An original may be claimed by
 * several lines — that is a declared split.
 */
fun isValidCleanupMapping(from: List<List<Int>>, originalCount: Int): Boolean {
    if (from.isEmpty() || from.any { it.isEmpty() }) return false
    val flat = from.flatten()
    if (flat.any { it !in 0 until originalCount }) return false
    if (from.any { it.toSet().size != it.size }) return false
    return flat.toSet().size == originalCount
}

/**
 * Pairs originals and cleaned lines exactly as the model declared it. Lines
 * sharing an original are grouped into one pairing (a split); a line claiming
 * several originals pairs them all (a merge).
 */
fun pairFromMapping(before: List<String>, after: List<String>, from: List<List<Int>>): List<CleanupPairing> {
    val groups = mutableListOf<List<Int>>()
    var current = mutableListOf<Int>()
    var currentOrigins = mutableSetOf<Int>()
    for (i in from.indices) {
        val origins = from[i].toSet()
        if (current.isNotEmpty() && origins.none { it in currentOrigins }) {
            groups.add(current)
            current = mutableListOf()
            currentOrigins = mutableSetOf()
        }
        current.add(i)
        currentOrigins.addAll(origins)
    }
    if (current.isNotEmpty()) groups.add(current)

    return groups
        .map { lines ->
            val origins = lines.flatMap { from[it] }.distinct()
            origins to lines
        }
        .sortedBy { (origins, _) -> origins.minOrNull() ?: 0 }
        .map { (origins, lines) ->
            CleanupPairing(
                before = origins.map { before[it] },
                after = lines.map { after[it] },
            )
        }
}

/**
 * Pairs originals with cleaned lines for review: uses the model-declared
 * mapping when it is valid, and falls back to name-based alignment when the
 * model's bookkeeping cannot be trusted.
 */
fun pairCleanupForReview(before: List<String>, cleaned: CleanedIngredients): List<CleanupPairing> =
    if (isValidCleanupMapping(cleaned.from, before.size)) {
        pairFromMapping(before, cleaned.lines, cleaned.from)
    } else {
        pairCleanupIngredients(before, cleaned.lines)
    }

/**
 * Aligns original and cleaned ingredient lines for review. Cleanup keeps the
 * order and may merge duplicate ingredients, so lines are matched by ingredient
 * name across the whole list (an optimal global alignment): a merge becomes one
 * pairing with two or more `before` lines, a split one pairing with two or more
 * `after` lines, and everything else pairs 1:1. Lines with no counterpart (the
 * model dropped or added one) become single-sided pairings instead of being
 * forced into a wrong-looking pair.
 */
fun pairCleanupIngredients(before: List<String>, after: List<String>): List<CleanupPairing> {
    val n = before.size
    val m = after.size
    if (n == 0 && m == 0) return emptyList()

    /** Bundle moves: match k originals with m cleaned lines. */
    val moves = listOf(1 to 1, 1 to 0, 0 to 1, 1 to 2, 1 to 3, 2 to 1, 3 to 1)

    val best = Array(n + 1) { DoubleArray(m + 1) }
    val choice = Array(n + 1) { arrayOfNulls<Pair<Int, Int>>(m + 1) }
    for (i in n downTo 0) {
        for (j in m downTo 0) {
            if (i == n && j == m) continue
            var bestScore = Double.NEGATIVE_INFINITY
            var bestMove: Pair<Int, Int>? = null
            for ((dk, dm) in moves) {
                val ni = i + dk
                val nj = j + dm
                if (ni > n || nj > m) continue
                val gain = when {
                    dk == 0 -> -DROP_ADD_PENALTY
                    dm == 0 -> -DROP_ADD_PENALTY
                    else -> {
                        val sim = crossSimilarity(before.subList(i, ni), after.subList(j, nj))
                        if (sim >= SAME_INGREDIENT_THRESHOLD) {
                            sim - BUNDLE_PENALTY * ((dk - 1) + (dm - 1))
                        } else {
                            sim - MISMATCH_PENALTY
                        }
                    }
                }
                val total = gain + best[ni][nj]
                if (total > bestScore) {
                    bestScore = total
                    bestMove = dk to dm
                }
            }
            best[i][j] = bestScore
            choice[i][j] = bestMove
        }
    }

    val pairings = mutableListOf<CleanupPairing>()
    var i = 0
    var j = 0
    while (i < n || j < m) {
        val (dk, dm) = choice[i][j] ?: break
        pairings.add(
            CleanupPairing(
                before = before.subList(i, i + dk),
                after = after.subList(j, j + dm),
            ),
        )
        i += dk
        j += dm
    }
    return pairings
}

/** Above this token-similarity, two lines are treated as the same ingredient. */
private const val SAME_INGREDIENT_THRESHOLD = 0.5

/** Small price per extra line bundled into a merge or split, so 1:1 wins ties. */
private const val BUNDLE_PENALTY = 0.01

/** Price for pairing lines that name different ingredients. */
private const val MISMATCH_PENALTY = 0.6

/** Price for a dropped or added line, so honest drop/add beats wrong pairing. */
private const val DROP_ADD_PENALTY = 0.25

/**
 * Cleaned ingredient lines plus the model's declaration of where each came
 * from: `from[i]` lists the indices into the original ingredient lines that
 * produced `lines[i]`.
 */
data class CleanedIngredients(
    val lines: List<String>,
    val from: List<List<Int>>,
)

/**
 * Parses cleanup response content into cleaned lines and their declared
 * origins; null when the content is not valid JSON or yields no usable lines.
 * Unreadable indices degrade to an empty `from` (pairing falls back to
 * name-based alignment) rather than losing the cleaned lines.
 */
fun parseCleanupResponse(content: String): CleanedIngredients? {
    val data = try {
        Json.parseToJsonElement(content)
    } catch (e: Exception) {
        return null
    }
    val ingredients = (data as? JsonObject)?.get("ingredients") as? JsonArray ?: return null
    val parsed = ingredients.mapNotNull { item ->
        val obj = item as? JsonObject ?: return@mapNotNull null
        val rawName = (obj["name"] as? JsonPrimitive)?.content ?: return@mapNotNull null
        val quantity = stripAlternateQuantity((obj["quantity"] as? JsonPrimitive)?.content ?: "")
        val name = removeDuplicatedUnit(quantity, rawName)
        val (fixedQuantity, fixedName) = moveParenthesizedUnitToQuantity(quantity, name)
        val line = ingredientPartsToLine(fixedQuantity, fixedName) ?: return@mapNotNull null
        val from = (obj["from"] as? JsonArray)
            ?.mapNotNull { (it as? JsonPrimitive)?.content?.toIntOrNull() }
            ?: emptyList()
        line to from
    }
    if (parsed.isEmpty()) return null
    return CleanedIngredients(
        lines = parsed.map { it.first },
        from = parsed.map { it.second },
    )
}

/**
 * Calls Mistral to clean up a recipe's ingredient lines: one unit each,
 * metric where convertible, amounts rounded within 10%. The wire protocol and
 * retry behavior live in [MistralChat]; this class owns the cleanup prompts,
 * schemas, and response parsing.
 */
class RecipeCleaner(transport: HttpTransport) {

    private val chat = MistralChat(transport)

    suspend fun cleanIngredients(apiKey: String, recipeName: String, ingredients: List<String>): CleanedIngredients {
        val key = apiKey.trim()
        if (key.isEmpty()) throw RecipeImportErrors.mistralKeyMissing()

        val cleaned = chat.chatForJson(
            apiKey = key,
            model = ImportConstants.MISTRAL_CLEANUP_MODEL,
            schemaName = "recipe_cleanup",
            systemPrompt = CLEANUP_SYSTEM_PROMPT,
            userContent = buildCleanupUserMessage(recipeName, ingredients),
            schema = CLEANUP_JSON_SCHEMA,
            parse = ::parseCleanupResponse,
        )
        return clampRounding(ingredients, cleaned)
    }

    /**
     * Calls Mistral to rewrite a recipe's instructions as a concise bulleted
     * list readable in about two minutes, with the fluff removed.
     */
    suspend fun cleanInstructions(
        apiKey: String,
        recipeName: String,
        ingredients: List<String>,
        instructions: String,
    ): String {
        val key = apiKey.trim()
        if (key.isEmpty()) throw RecipeImportErrors.mistralKeyMissing()

        return chat.chatForJson(
            apiKey = key,
            model = ImportConstants.MISTRAL_CLEANUP_MODEL,
            schemaName = "recipe_cleanup",
            systemPrompt = INSTRUCTIONS_CLEANUP_SYSTEM_PROMPT,
            userContent = buildInstructionsCleanupUserMessage(recipeName, ingredients, instructions),
            schema = INSTRUCTIONS_CLEANUP_JSON_SCHEMA,
            parse = ::parseInstructionsCleanupResponse,
        )
    }
}
