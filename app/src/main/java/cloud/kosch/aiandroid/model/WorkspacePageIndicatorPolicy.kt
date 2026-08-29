package cloud.kosch.aiandroid.model

/**
 * Keeps Home page navigation visually quiet even when users create many personal pages.
 *
 * A null slot represents one ellipsis between directly addressable page dots. The policy deliberately keeps the
 * first and last page visible and centers the active page when possible, so the indicator never grows with the
 * workspace while full page management remains in Home Studio.
 */
object WorkspacePageIndicatorPolicy {
    const val MAX_VISIBLE_SLOTS = 7

    fun slots(pageCount: Int, activeIndex: Int): List<Int?> {
        if (pageCount <= 0) return emptyList()
        val safeActive = activeIndex.coerceIn(0, pageCount - 1)
        if (pageCount <= MAX_VISIBLE_SLOTS) return (0 until pageCount).map { it }

        return when {
            safeActive <= 2 -> listOf(0, 1, 2, 3, 4, null, pageCount - 1)
            safeActive >= pageCount - 3 -> listOf(
                0,
                null,
                pageCount - 5,
                pageCount - 4,
                pageCount - 3,
                pageCount - 2,
                pageCount - 1,
            )
            else -> listOf(
                0,
                null,
                safeActive - 1,
                safeActive,
                safeActive + 1,
                null,
                pageCount - 1,
            )
        }
    }
}
