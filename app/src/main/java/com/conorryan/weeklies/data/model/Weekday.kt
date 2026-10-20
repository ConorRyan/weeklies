package com.conorryan.weeklies.data.model

enum class Weekday(val key: String, val label: String) {
    MON("mon", "Monday"),
    TUE("tue", "Tuesday"),
    WED("wed", "Wednesday"),
    THU("thu", "Thursday"),
    FRI("fri", "Friday"),
    SAT("sat", "Saturday"),
    SUN("sun", "Sunday");

    companion object {
        fun fromKey(key: String): Weekday? = entries.firstOrNull { it.key == key }

        /** Maps a JS-style day index (0 = Sunday .. 6 = Saturday) to a weekday. */
        fun fromJsDayIndex(jsDay: Int): Weekday = when (jsDay) {
            0 -> SUN
            1 -> MON
            2 -> TUE
            3 -> WED
            4 -> THU
            5 -> FRI
            6 -> SAT
            else -> MON
        }

        fun fromDate(d: java.util.Date): Weekday = fromJsDayIndex(compatGetDay(d))

        private fun compatGetDay(d: java.util.Date): Int =
            java.time.Instant.ofEpochMilli(d.time)
                .atZone(java.time.ZoneId.systemDefault())
                .dayOfWeek
                .let { dow ->
                    when (dow) {
                        java.time.DayOfWeek.SUNDAY -> 0
                        java.time.DayOfWeek.MONDAY -> 1
                        java.time.DayOfWeek.TUESDAY -> 2
                        java.time.DayOfWeek.WEDNESDAY -> 3
                        java.time.DayOfWeek.THURSDAY -> 4
                        java.time.DayOfWeek.FRIDAY -> 5
                        java.time.DayOfWeek.SATURDAY -> 6
                    }
                }
    }
}

/** Recipe assigned per weekday; null means nothing planned. */
typealias DayAssignments = Map<Weekday, String?>

fun emptyByDay(): DayAssignments = Weekday.entries.associateWith { null }

fun normalizeDayAssignments(raw: Map<String, String?>): DayAssignments {
    val next = emptyByDay().toMutableMap()
    for ((key, value) in raw) {
        val day = Weekday.fromKey(key) ?: continue
        next[day] = value
    }
    return next.toMap()
}
