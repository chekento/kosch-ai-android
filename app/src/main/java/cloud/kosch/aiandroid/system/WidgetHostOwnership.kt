package cloud.kosch.aiandroid.system

/** Pure ownership rule for AppWidgetHost ids that survive independently from launcher process state. */
object WidgetHostOwnership {
    fun orphanedIds(
        hostedIds: Set<Int>,
        ownedIds: Set<Int>,
    ): Set<Int> = hostedIds
        .asSequence()
        .filter { it > 0 && it !in ownedIds }
        .toSet()
}
