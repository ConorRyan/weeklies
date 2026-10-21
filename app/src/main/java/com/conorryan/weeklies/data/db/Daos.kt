package com.conorryan.weeklies.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RecipeDao {
    @Query("SELECT * FROM recipes ORDER BY rowid")
    fun observeAll(): Flow<List<RecipeEntity>>

    @Query("SELECT * FROM recipes WHERE id = :id")
    fun observeById(id: String): Flow<RecipeEntity?>

    @Query("SELECT * FROM recipes ORDER BY rowid")
    suspend fun getAll(): List<RecipeEntity>

    @Query("SELECT * FROM recipes WHERE id = :id")
    suspend fun getById(id: String): RecipeEntity?

    @Insert
    suspend fun insert(recipe: RecipeEntity)

    @Update
    suspend fun update(recipe: RecipeEntity)

    @Query("DELETE FROM recipes WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM recipes")
    suspend fun deleteAll()

    @Transaction
    suspend fun replaceAll(recipes: List<RecipeEntity>) {
        deleteAll()
        recipes.forEach { insert(it) }
    }
}

@Dao
interface PlanDao {
    @Query("SELECT * FROM plan")
    fun observeAll(): Flow<List<PlanEntryEntity>>

    @Query("SELECT * FROM plan")
    suspend fun getAll(): List<PlanEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: PlanEntryEntity)

    @Query("DELETE FROM plan")
    suspend fun deleteAll()

    @Transaction
    suspend fun replaceAll(entries: List<PlanEntryEntity>) {
        deleteAll()
        entries.forEach { upsert(it) }
    }
}

@Dao
interface ShoppingItemDao {
    @Query("SELECT * FROM shopping_items ORDER BY position")
    fun observeAll(): Flow<List<ShoppingItemEntity>>

    @Query("SELECT * FROM shopping_items ORDER BY position")
    suspend fun getAll(): List<ShoppingItemEntity>

    @Query("SELECT COALESCE(MAX(position), 0) + 1 FROM shopping_items")
    suspend fun nextPosition(): Long

    @Insert
    suspend fun insert(item: ShoppingItemEntity)

    @Query("UPDATE shopping_items SET checked = NOT checked WHERE id = :id")
    suspend fun toggle(id: String)

    @Query("UPDATE shopping_items SET label = :label WHERE id = :id")
    suspend fun setLabel(id: String, label: String)

    @Query("DELETE FROM shopping_items WHERE checked = 1")
    suspend fun deleteChecked()

    @Query("DELETE FROM shopping_items")
    suspend fun deleteAll()

    /** Caller supplies positions; only [insert] is used here. */
    @Transaction
    suspend fun replaceAll(items: List<ShoppingItemEntity>) {
        deleteAll()
        items.forEach { insert(it) }
    }
}
