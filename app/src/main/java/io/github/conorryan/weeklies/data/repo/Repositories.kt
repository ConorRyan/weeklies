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
import io.github.conorryan.weeklies.data.shopping.combineShoppingLine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

private fun RecipeEntity.toModel() = Recipe(id = id, name = name, ingredients = ingredients)
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
        )
        db.recipeDao().insert(entity)
        return entity.toModel()
    }

    suspend fun updateRecipe(id: String, recipe: NewRecipe) {
        val existing = db.recipeDao().getById(id) ?: return
        db.recipeDao().update(
            existing.copy(name = recipe.name, ingredients = recipe.ingredients),
        )
    }

    suspend fun removeRecipe(id: String) = db.recipeDao().delete(id)

    /** Replaces the whole recipe list. */
    suspend fun replaceAll(recipes: List<Recipe>) {
        db.recipeDao().replaceAll(recipes.map { it.toEntity() })
    }

    private fun Recipe.toEntity() = RecipeEntity(id = id, name = name, ingredients = ingredients)
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
