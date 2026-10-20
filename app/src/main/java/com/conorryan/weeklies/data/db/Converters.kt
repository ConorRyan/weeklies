package com.conorryan.weeklies.data.db

import androidx.room.TypeConverter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Converters {
    @TypeConverter
    fun ingredientsToJson(ingredients: List<String>): String = Json.encodeToString(ingredients)

    @TypeConverter
    fun ingredientsFromJson(json: String): List<String> = Json.decodeFromString(json)
}
