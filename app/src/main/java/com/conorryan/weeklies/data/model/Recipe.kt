package com.conorryan.weeklies.data.model

data class Recipe(val id: String, val name: String, val ingredients: List<String>)

/** A recipe without an id yet; assigned on save. */
data class NewRecipe(val name: String, val ingredients: List<String>)
