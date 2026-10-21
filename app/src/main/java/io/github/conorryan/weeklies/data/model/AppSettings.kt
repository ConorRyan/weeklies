package io.github.conorryan.weeklies.data.model

enum class AppColorScheme(val key: String) {
    LIGHT("light"),
    DARK("dark");

    companion object {
        fun fromKey(key: String?): AppColorScheme? =
            entries.firstOrNull { it.key == key }
    }
}

data class AppSettings(val colorScheme: AppColorScheme, val portionsPerDay: Int) {
    companion object {
        const val DEFAULT_PORTIONS_PER_DAY = 2
    }
}

fun isValidPortionsPerDay(n: Int): Boolean = n > 0

fun isValidPortionsPerDay(n: Double): Boolean = !n.isNaN() && !n.isInfinite() && n > 0
