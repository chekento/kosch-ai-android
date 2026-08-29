package cloud.kosch.aiandroid.ai

import cloud.kosch.aiandroid.model.WidgetStack
import cloud.kosch.aiandroid.model.WidgetStackMode

/** Pure policy for device-local widget stacks; it never allocates, binds or deletes Android AppWidget ids. */
object WidgetStackPolicy {
    const val MAX_STACKS = 16
    const val MAX_WIDGETS_PER_STACK = 12
    const val MIN_AUTO_CYCLE_SECONDS = 15
    const val MAX_AUTO_CYCLE_SECONDS = 3_600
    const val MAX_TITLE_CHARS = 48

    fun normalize(stack: WidgetStack): WidgetStack? {
        val id = stack.id.trim().take(80).ifBlank { return null }
        val ids = stack.appWidgetIds
            .asSequence()
            .filter { it > 0 }
            .distinct()
            .take(MAX_WIDGETS_PER_STACK)
            .toList()
        if (ids.isEmpty()) return null
        val mode = stack.mode
        val cycle = when (mode) {
            WidgetStackMode.AUTO_CYCLE -> stack.autoCycleSeconds.coerceIn(
                MIN_AUTO_CYCLE_SECONDS,
                MAX_AUTO_CYCLE_SECONDS,
            )
            WidgetStackMode.MANUAL,
            WidgetStackMode.CONTEXTUAL -> 0
        }
        return stack.copy(
            id = id,
            title = normalizedTitle(stack.title),
            appWidgetIds = ids,
            activeIndex = stack.activeIndex.coerceIn(0, ids.lastIndex),
            autoCycleSeconds = cycle,
        )
    }

    fun normalizeAll(stacks: List<WidgetStack>): List<WidgetStack> = stacks
        .asSequence()
        .mapNotNull(::normalize)
        .distinctBy(WidgetStack::id)
        .take(MAX_STACKS)
        .toList()

    fun repair(stacks: List<WidgetStack>, validWidgetIds: Set<Int>): List<WidgetStack> = normalizeAll(
        stacks.map { stack ->
            val previousActiveId = stack.activeWidgetId
            val surviving = stack.appWidgetIds.filter(validWidgetIds::contains)
            val nextIndex = previousActiveId
                ?.let(surviving::indexOf)
                ?.takeIf { it >= 0 }
                ?: stack.activeIndex.coerceIn(0, (surviving.size - 1).coerceAtLeast(0))
            stack.copy(appWidgetIds = surviving, activeIndex = nextIndex)
        },
    )

    fun next(stack: WidgetStack): WidgetStack? = normalize(stack)?.let { normalized ->
        normalized.copy(activeIndex = (normalized.activeIndex + 1) % normalized.appWidgetIds.size)
    }

    fun previous(stack: WidgetStack): WidgetStack? = normalize(stack)?.let { normalized ->
        normalized.copy(
            activeIndex = (normalized.activeIndex - 1 + normalized.appWidgetIds.size) % normalized.appWidgetIds.size,
        )
    }

    fun select(stack: WidgetStack, appWidgetId: Int): WidgetStack? = normalize(stack)?.let { normalized ->
        val index = normalized.appWidgetIds.indexOf(appWidgetId)
        if (index < 0) null else normalized.copy(activeIndex = index)
    }

    fun rename(stack: WidgetStack, title: String): WidgetStack? = normalize(stack)?.let { normalized ->
        normalized.copy(title = normalizedTitle(title))
    }

    fun addWidget(stack: WidgetStack, appWidgetId: Int): WidgetStack? {
        if (appWidgetId <= 0) return normalize(stack)
        val normalized = normalize(stack) ?: return null
        val updated = (normalized.appWidgetIds + appWidgetId).distinct().take(MAX_WIDGETS_PER_STACK)
        return normalize(normalized.copy(appWidgetIds = updated))
    }

    /** Moves one member without changing which widget is active. */
    fun moveWidget(stack: WidgetStack, appWidgetId: Int, delta: Int): WidgetStack? {
        val normalized = normalize(stack) ?: return null
        if (delta == 0) return normalized
        val from = normalized.appWidgetIds.indexOf(appWidgetId)
        if (from < 0) return null
        val to = (from + delta).coerceIn(0, normalized.appWidgetIds.lastIndex)
        if (to == from) return normalized
        val activeId = normalized.activeWidgetId
        val reordered = normalized.appWidgetIds.toMutableList().apply {
            val moved = removeAt(from)
            add(to, moved)
        }
        val activeIndex = activeId?.let(reordered::indexOf)?.takeIf { it >= 0 } ?: 0
        return normalize(normalized.copy(appWidgetIds = reordered, activeIndex = activeIndex))
    }

    fun removeWidget(stack: WidgetStack, appWidgetId: Int): WidgetStack? {
        val normalized = normalize(stack) ?: return null
        val remaining = normalized.appWidgetIds.filterNot { it == appWidgetId }
        if (remaining.isEmpty()) return null
        val previousActive = normalized.activeWidgetId
        val nextIndex = previousActive
            ?.takeUnless { it == appWidgetId }
            ?.let(remaining::indexOf)
            ?.takeIf { it >= 0 }
            ?: normalized.activeIndex.coerceIn(0, remaining.lastIndex)
        return normalize(normalized.copy(appWidgetIds = remaining, activeIndex = nextIndex))
    }

    private fun normalizedTitle(title: String): String =
        title.trim().take(MAX_TITLE_CHARS).ifBlank { "Widget Stack" }
}
