package io.github.conorryan.weeklies.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recipes")
data class RecipeEntity(
    @PrimaryKey val id: String,
    val name: String,
    /** Ingredient lines, one per row, in order. */
    val ingredients: List<String>,
    /** Cooking steps as one editable text blob; added in schema v2. */
    @ColumnInfo(defaultValue = "")
    val instructions: String = "",
)

@Entity(tableName = "plan")
data class PlanEntryEntity(
    @PrimaryKey val weekday: String,
    val recipeId: String?,
)

@Entity(tableName = "shopping_items")
data class ShoppingItemEntity(
    @PrimaryKey val id: String,
    val label: String,
    val checked: Boolean,
    val position: Long,
)
