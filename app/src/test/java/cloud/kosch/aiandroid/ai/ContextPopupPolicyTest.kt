package cloud.kosch.aiandroid.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContextPopupPolicyTest {
    @Test
    fun `open wins then published shortcuts and privacy bounded badge summary`() {
        val items = ContextPopupPolicy.build(
            ContextPopupInput(
                appKey = "personal:mail",
                appLabel = "Mail",
                isPinned = false,
                isHidden = false,
                badgeCount = 12,
                publishedShortcuts = listOf(
                    ContextPopupShortcut("compose", "Neue Nachricht"),
                    ContextPopupShortcut("inbox", "Posteingang"),
                ),
            ),
        )

        assertEquals(ContextPopupItemKind.OPEN_APP, items[0].kind)
        assertEquals(ContextPopupItemKind.PUBLISHED_SHORTCUT, items[1].kind)
        assertEquals(ContextPopupItemKind.PUBLISHED_SHORTCUT, items[2].kind)
        assertEquals(ContextPopupItem.BadgeSummary(12), items[3])
    }

    @Test
    fun `shortcuts are deduplicated bounded and deterministic`() {
        val shortcuts = (0 until 20).map {
            ContextPopupShortcut(id = "id-${it % 10}", label = "Shortcut ${20 - it}")
        }
        val items = ContextPopupPolicy.build(
            ContextPopupInput(
                appKey = "app",
                appLabel = "App",
                isPinned = true,
                isHidden = true,
                publishedShortcuts = shortcuts,
            ),
        )

        assertEquals(
            ContextPopupPolicy.MAX_SHORTCUTS,
            items.count { it.kind == ContextPopupItemKind.PUBLISHED_SHORTCUT },
        )
        assertTrue(items.size <= ContextPopupPolicy.MAX_ITEMS)
        assertEquals(items.map(ContextPopupItem::stableId).distinct().size, items.size)
    }

    @Test
    fun `badge is capped and no content field exists in output model`() {
        val items = ContextPopupPolicy.build(
            ContextPopupInput(
                appKey = "chat",
                appLabel = "Chat",
                isPinned = false,
                isHidden = false,
                badgeCount = Int.MAX_VALUE,
            ),
        )
        val badge = items.filterIsInstance<ContextPopupItem.BadgeSummary>().single()

        assertEquals(ContextPopupPolicy.MAX_BADGE_COUNT, badge.count)
        assertFalse(items.any { it.title.contains("message body", ignoreCase = true) })
    }

    @Test
    fun `widget entry advertises capability but carries no binding id`() {
        val items = ContextPopupPolicy.build(
            ContextPopupInput(
                appKey = "weather",
                appLabel = "Weather",
                isPinned = false,
                isHidden = false,
                publishedWidgetCount = 3,
            ),
        )

        val widget = items.filterIsInstance<ContextPopupItem.WidgetEntry>().single()
        assertEquals(3, widget.providerCount)
        assertEquals("widgets", widget.stableId)
    }

    @Test
    fun `invalid app identity fails closed`() {
        assertTrue(
            ContextPopupPolicy.build(
                ContextPopupInput(
                    appKey = "",
                    appLabel = "App",
                    isPinned = false,
                    isHidden = false,
                ),
            ).isEmpty(),
        )
    }
}
