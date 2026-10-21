package io.github.conorryan.weeklies.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [RecipeEntity::class, PlanEntryEntity::class, ShoppingItemEntity::class],
    version = 2,
    exportSchema = false,
)
@androidx.room.TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun recipeDao(): RecipeDao
    abstract fun planDao(): PlanDao
    abstract fun shoppingItemDao(): ShoppingItemDao

    companion object {
        /** v2 added recipe instructions, stored as one text blob. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE recipes ADD COLUMN instructions TEXT NOT NULL DEFAULT ''",
                )
            }
        }

        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "weeklies.db")
                .addMigrations(MIGRATION_1_2)
                .build()
    }
}
