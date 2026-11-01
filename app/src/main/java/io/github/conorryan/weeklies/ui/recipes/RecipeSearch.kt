package io.github.conorryan.weeklies.ui.recipes

import io.github.conorryan.weeklies.data.model.Recipe

/**
 * Recipes whose name or any ingredient line contains every word of [query],
 * ignoring case and accents; all recipes when the query is blank.
 */
fun filterRecipes(recipes: List<Recipe>, query: String): List<Recipe> {
    val words = normalize(query).split(Regex("""\s+""")).filter { it.isNotEmpty() }
    if (words.isEmpty()) return recipes
    return recipes.filter { recipe ->
        val haystack = normalize(recipe.name + "\n" + recipe.ingredients.joinToString("\n"))
        words.all { it in haystack }
    }
}

private fun normalize(text: String): String =
    java.text.Normalizer.normalize(text.lowercase(), java.text.Normalizer.Form.NFD)
        .replace(Regex("""\p{Mn}+"""), "")
