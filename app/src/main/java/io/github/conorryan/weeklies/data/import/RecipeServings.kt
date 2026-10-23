package io.github.conorryan.weeklies.data.import

import io.github.conorryan.weeklies.data.quantity.scaleIngredientLine

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

/**
 * Rescales ingredient lines whose amounts currently make [portions] portions
 * down (or up) to a single portion; unchanged when [portions] is not usable.
 */
fun rescaleToSinglePortion(ingredients: List<String>, portions: Double): List<String> =
    scaleIngredientsToServings(ingredients, portions, 1.0)
