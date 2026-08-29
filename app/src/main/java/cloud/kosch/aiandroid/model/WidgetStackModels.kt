package cloud.kosch.aiandroid.model

enum class WidgetStackMode(val title: String) {
    MANUAL("Manuell"),
    CONTEXTUAL("Kontextuell"),
    AUTO_CYCLE("Auto-Wechsel"),
}

/**
 * Device-local stack of already bound AppWidgetHost ids.
 *
 * appWidgetIds are Android host bindings and therefore must never be copied into portable workspace/settings backup.
 */
data class WidgetStack(
    val id: String,
    val title: String,
    val appWidgetIds: List<Int>,
    val activeIndex: Int = 0,
    val mode: WidgetStackMode = WidgetStackMode.MANUAL,
    val autoCycleSeconds: Int = 0,
) {
    val activeWidgetId: Int?
        get() = appWidgetIds.getOrNull(activeIndex)
}
