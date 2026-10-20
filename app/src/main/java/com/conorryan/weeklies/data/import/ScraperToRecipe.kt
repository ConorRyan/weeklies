package com.conorryan.weeklies.data.import

import com.conorryan.weeklies.data.model.NewRecipe

private val YIELD_NUMBER = Regex("""(\d+(?:\.\d+)?)""")

/** Extract a positive serving count from recipeYield text (uses first number found). */
fun parseRecipeYield(raw: String?): Int? {
    if (raw.isNullOrBlank()) return null
    val match = YIELD_NUMBER.find(raw) ?: return null
    val n = match.groupValues[1].toDoubleOrNull() ?: return null
    return if (isValidServings(n)) n.toInt() else null
}

fun scrapedRecipeToNewRecipe(
    data: ScrapedRecipeData,
    targetServings: Int = ImportConstants.RECIPE_IMPORT_TARGET_SERVINGS,
): NewRecipe? {
    val name = data.name.trim()
    if (name.isEmpty()) return null

    val ingredients = data.recipeIngredients
        .map { it.trim() }
        .filter { it.isNotEmpty() }
    if (ingredients.isEmpty()) return null

    val recipeServings = parseRecipeYield(data.recipeYield)
    if (recipeServings != null && isValidServings(targetServings.toDouble())) {
        return NewRecipe(
            name = name,
            ingredients = scaleIngredientsToServings(
                ingredients,
                recipeServings.toDouble(),
                targetServings.toDouble(),
            ),
        )
    }

    return NewRecipe(name = name, ingredients = ingredients)
}
