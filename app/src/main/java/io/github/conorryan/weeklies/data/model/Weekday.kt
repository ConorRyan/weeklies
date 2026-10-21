package io.github.conorryan.weeklies.data.model

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

        fun fromDayOfWeek(dayOfWeek: java.time.DayOfWeek): Weekday = when (dayOfWeek) {
            java.time.DayOfWeek.MONDAY -> MON
            java.time.DayOfWeek.TUESDAY -> TUE
            java.time.DayOfWeek.WEDNESDAY -> WED
            java.time.DayOfWeek.THURSDAY -> THU
            java.time.DayOfWeek.FRIDAY -> FRI
            java.time.DayOfWeek.SATURDAY -> SAT
            java.time.DayOfWeek.SUNDAY -> SUN
        }

        /** The weekday containing "now"; the single point where "today" is defined. */
        fun today(): Weekday = fromDayOfWeek(java.time.LocalDate.now().dayOfWeek)
    }
}

/** Recipe assigned per weekday; null means nothing planned. */
typealias DayAssignments = Map<Weekday, String?>

fun emptyByDay(): DayAssignments = Weekday.entries.associateWith { null }
