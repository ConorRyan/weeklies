package io.github.conorryan.weeklies.data.model

data class Recipe(
    val id: String,
    val name: String,
    val ingredients: List<String>,
    /** Cooking steps as one editable text blob. */
    val instructions: String = "",
    /** Estimated calories for one portion; null when never estimated or entered. */
    val calories: Int? = null,
)

/**
 * A recipe without an id yet; assigned on save. [calories] travels with the
 * rest: the edit form owns it, and saving stores it.
 */
data class NewRecipe(
    val name: String,
    val ingredients: List<String>,
    val instructions: String = "",
    /** Estimated calories for one portion; null when never estimated or entered. */
    val calories: Int? = null,
)
