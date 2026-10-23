package io.github.conorryan.weeklies.data.repo

import io.github.conorryan.weeklies.data.db.AppDatabase
import io.github.conorryan.weeklies.data.db.PlanEntryEntity
import io.github.conorryan.weeklies.data.db.RecipeEntity
import io.github.conorryan.weeklies.data.db.ShoppingItemEntity
import io.github.conorryan.weeklies.data.model.DayAssignments
import io.github.conorryan.weeklies.data.model.NewRecipe
import io.github.conorryan.weeklies.data.model.Recipe
import io.github.conorryan.weeklies.data.model.ShoppingItem
import io.github.conorryan.weeklies.data.model.Weekday
import io.github.conorryan.weeklies.data.model.emptyByDay
import io.github.conorryan.weeklies.data.quantity.scaleIngredientLine
import io.github.conorryan.weeklies.data.shopping.combineShoppingLine
import io.github.conorryan.weeklies.data.shopping.stripIngredientNotes
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

private fun RecipeEntity.toModel() =
    Recipe(id = id, name = name, ingredients = ingredients, instructions = instructions)
private fun ShoppingItemEntity.toModel() =
    ShoppingItem(id = id, label = label, checked = checked)

class RecipeRepository(private val db: AppDatabase) {

    val recipes: Flow<List<Recipe>> = db.recipeDao().observeAll().map { rows ->
        rows.map { it.toModel() }
    }

    fun observeRecipe(id: String): Flow<Recipe?> = db.recipeDao().observeById(id).map { it?.toModel() }

    suspend fun snapshot(): List<Recipe> = db.recipeDao().getAll().map { it.toModel() }

    suspend fun addRecipe(recipe: NewRecipe): Recipe {
        val entity = RecipeEntity(
            id = UUID.randomUUID().toString(),
            name = recipe.name,
            ingredients = recipe.ingredients,
            instructions = recipe.instructions,
        )
        db.recipeDao().insert(entity)
        return entity.toModel()
    }

    suspend fun updateRecipe(id: String, recipe: NewRecipe) {
        val existing = db.recipeDao().getById(id) ?: return
        db.recipeDao().update(
            existing.copy(name = recipe.name, ingredients = recipe.ingredients, instructions = recipe.instructions),
        )
    }

    suspend fun removeRecipe(id: String) = db.recipeDao().delete(id)

    /** Replaces the whole recipe list. */
    suspend fun replaceAll(recipes: List<Recipe>) {
        db.recipeDao().replaceAll(recipes.map { it.toEntity() })
    }

    private fun Recipe.toEntity() = RecipeEntity(
        id = id,
        name = name,
        ingredients = ingredients,
        instructions = instructions,
    )
}

class PlanRepository(private val db: AppDatabase) {

    val byDay: Flow<DayAssignments> = db.planDao().observeAll().map { rows ->
        val next = emptyByDay().toMutableMap()
        for (row in rows) {
            val day = Weekday.fromKey(row.weekday) ?: continue
            next[day] = row.recipeId
        }
        next.toMap()
    }

    suspend fun snapshot(): DayAssignments {
        val next = emptyByDay().toMutableMap()
        for (row in db.planDao().getAll()) {
            val day = Weekday.fromKey(row.weekday) ?: continue
            next[day] = row.recipeId
        }
        return next.toMap()
    }

    suspend fun setDayRecipe(day: Weekday, recipeId: String?) {
        db.planDao().upsert(PlanEntryEntity(weekday = day.key, recipeId = recipeId))
    }

    /** Replaces the whole plan; days missing from [byDay] are cleared. */
    suspend fun replaceAll(byDay: DayAssignments) {
        val entries = byDay.map { (day, recipeId) ->
            PlanEntryEntity(weekday = day.key, recipeId = recipeId)
        }
        db.planDao().replaceAll(entries)
    }
}

class ShoppingRepository(private val db: AppDatabase) {

    val items: Flow<List<ShoppingItem>> = db.shoppingItemDao().observeAll().map { rows ->
        rows.map { it.toModel() }
    }

    suspend fun snapshot(): List<ShoppingItem> =
        db.shoppingItemDao().getAll().map { it.toModel() }

    /**
     * Adds an item, summing the first number into an existing unchecked line
     * whose remaining text matches (e.g. "100g pasta" + "100g pasta" becomes
     * "200g pasta"); otherwise inserts a new row.
     */
    suspend fun addItem(label: String) {
        val trimmed = label.trim()
        if (trimmed.isEmpty()) return
        val merge = db.shoppingItemDao().getAll().firstNotNullOfOrNull { row ->
            if (row.checked) null
            else combineShoppingLine(row.label, trimmed)?.let { row.id to it }
        }
        if (merge != null) {
            db.shoppingItemDao().setLabel(merge.first, merge.second)
        } else {
            db.shoppingItemDao().insert(
                ShoppingItemEntity(
                    id = UUID.randomUUID().toString(),
                    label = trimmed,
                    checked = false,
                    position = db.shoppingItemDao().nextPosition(),
                ),
            )
        }
    }

    suspend fun toggleItem(id: String) {
        db.shoppingItemDao().toggle(id)
    }

    suspend fun renameItem(id: String, label: String) {
        val trimmed = label.trim()
        if (trimmed.isEmpty()) return
        db.shoppingItemDao().setLabel(id, trimmed)
    }

    /**
     * Adds a recipe's ingredients to the list, scaled to [portions]:
     * preparation notes are stripped, note-only lines are skipped, amounts
     * are multiplied, and each line merges into an existing unchecked line
     * whose remaining text matches (the same rule as [addItem]). Runs as one
     * transaction so the whole recipe lands or nothing does.
     */
    suspend fun addRecipeIngredients(recipe: Recipe, portions: Int) {
        val factor = portions.toDouble()
        val lines = recipe.ingredients
            .map { scaleIngredientLine(stripIngredientNotes(it), factor).trim() }
            .filter { it.isNotEmpty() }
        if (lines.isEmpty()) return

        db.withTransaction {
            var rows = db.shoppingItemDao().getAll()
            var position = db.shoppingItemDao().nextPosition()
            for (line in lines) {
                val trimmed = line.trim()
                if (trimmed.isEmpty()) continue
                val merge = rows.firstNotNullOfOrNull { row ->
                    if (row.checked) null
                    else combineShoppingLine(row.label, trimmed)?.let { row.id to it }
                }
                if (merge != null) {
                    db.shoppingItemDao().setLabel(merge.first, merge.second)
                    rows = rows.map { if (it.id == merge.first) it.copy(label = merge.second) else it }
                } else {
                    val entity = ShoppingItemEntity(
                        id = UUID.randomUUID().toString(),
                        label = trimmed,
                        checked = false,
                        position = position,
                    )
                    db.shoppingItemDao().insert(entity)
                    position += 1
                    rows = rows + entity
                }
            }
        }
    }

    suspend fun removeCheckedItems() = db.shoppingItemDao().deleteChecked()

    /** Replaces the whole list; order and positions follow the given list. */
    suspend fun replaceAll(items: List<ShoppingItem>) {
        db.shoppingItemDao().replaceAll(
            items.mapIndexed { index, item -> item.toEntity(index.toLong()) },
        )
    }

    private fun ShoppingItem.toEntity(position: Long) = ShoppingItemEntity(
        id = id,
        label = label,
        checked = checked,
        position = position,
    )
}
