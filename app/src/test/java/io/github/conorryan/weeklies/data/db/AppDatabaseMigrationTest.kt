package io.github.conorryan.weeklies.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AppDatabaseMigrationTest {

    private lateinit var context: Context

    /** Each test gets its own file so one test's database can never leak into another's. */
    private val migrationDbName = "weeklies-migration-test.db"
    private val persistDbName = "weeklies-persist-test.db"

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(migrationDbName)
        context.deleteDatabase(persistDbName)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(migrationDbName)
        context.deleteDatabase(persistDbName)
    }

    /** Creates the v1 schema by hand, as an app updated from v1 would have left it. */
    private fun createV1Database(dbName: String) {
        val db = context.openOrCreateDatabase(dbName, Context.MODE_PRIVATE, null)
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `recipes` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, " +
                "`ingredients` TEXT NOT NULL, PRIMARY KEY(`id`))",
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `plan` (`weekday` TEXT NOT NULL, `recipeId` TEXT, " +
                "PRIMARY KEY(`weekday`))",
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `shopping_items` (`id` TEXT NOT NULL, `label` TEXT NOT NULL, " +
                "`checked` INTEGER NOT NULL, `position` INTEGER NOT NULL, PRIMARY KEY(`id`))",
        )
        db.execSQL(
            "INSERT INTO recipes (`id`, `name`, `ingredients`) VALUES ('r1', 'Pasta', '[\"200g pasta\"]')",
        )
        db.version = 1
        db.close()
    }

    /** Creates the v2 schema by hand, as an app updated from v2 would have left it. */
    private fun createV2Database(dbName: String) {
        createV1Database(dbName)
        val db = context.openOrCreateDatabase(dbName, Context.MODE_PRIVATE, null)
        db.execSQL("ALTER TABLE recipes ADD COLUMN instructions TEXT NOT NULL DEFAULT ''")
        db.execSQL("UPDATE recipes SET instructions = 'Boil.' WHERE id = 'r1'")
        db.version = 2
        db.close()
    }

    @Test
    fun migratesV1ToV2KeepingRecipes() = runBlocking {
        createV1Database(migrationDbName)

        val db = Room.databaseBuilder(context, AppDatabase::class.java, migrationDbName)
            .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3)
            .build()
        try {
            val recipe = db.recipeDao().getById("r1")
            assertEquals("Pasta", recipe?.name)
            assertEquals(listOf("200g pasta"), recipe?.ingredients)
            assertEquals("", recipe?.instructions)
            assertNull(recipe?.calories)
        } finally {
            db.close()
        }
    }

    @Test
    fun migratesV2ToV3KeepingRecipesAndCalories() = runBlocking {
        createV2Database(migrationDbName)
        val db = Room.databaseBuilder(context, AppDatabase::class.java, migrationDbName)
            .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3)
            .build()
        try {
            val recipe = db.recipeDao().getById("r1")
            assertEquals("Pasta", recipe?.name)
            assertEquals(listOf("200g pasta"), recipe?.ingredients)
            assertEquals("Boil.", recipe?.instructions)
            assertNull(recipe?.calories)

            db.recipeDao().update(recipe!!.copy(calories = 550))
            assertEquals(550, db.recipeDao().getById("r1")?.calories)
        } finally {
            db.close()
        }
    }

    @Test
    fun newRecipesPersistInstructions() = runBlocking {
        val db = Room.databaseBuilder(context, AppDatabase::class.java, persistDbName)
            .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3)
            .build()
        try {
            db.recipeDao().insert(
                RecipeEntity(id = "r1", name = "Soup", ingredients = listOf("1 onion"), instructions = "Simmer.", calories = 300),
            )
            assertEquals("Simmer.", db.recipeDao().getById("r1")?.instructions)
            assertEquals(300, db.recipeDao().getById("r1")?.calories)
        } finally {
            db.close()
        }
    }
}
