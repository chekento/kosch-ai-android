package cloud.kosch.aiandroid.ai

import cloud.kosch.aiandroid.model.AppUsageSignal
import cloud.kosch.aiandroid.model.DockSettings
import cloud.kosch.aiandroid.model.SceneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartDockRuntimePolicyTest {
    private val apps = listOf(
        SmartAppDescriptor("mail", "Mail", "example.mail"),
        SmartAppDescriptor("chat", "Chat", "example.chat"),
        SmartAppDescriptor("music", "Music", "example.music"),
    )

    @Test
    fun disabledDock_returnsNoKeys() {
        val keys = SmartDockRuntimePolicy.selectKeys(
            apps = apps,
            pinnedKeys = listOf("mail"),
            recentPackages = listOf("example.chat"),
            usageSignals = emptyMap(),
            scene = SceneId.WORK,
            settings = DockSettings(enabled = false),
        )

        assertTrue(keys.isEmpty())
    }

    @Test
    fun adaptiveOff_usesPinnedOrderOnlyAndHonorsLimit() {
        val keys = SmartDockRuntimePolicy.selectKeys(
            apps = apps,
            pinnedKeys = listOf("music", "mail", "missing", "chat"),
            recentPackages = listOf("example.chat"),
            usageSignals = mapOf("chat" to AppUsageSignal("chat", launchCount = 99, lastUsedEpochMillis = 100L)),
            scene = SceneId.WORK,
            settings = DockSettings(adaptiveSuggestions = false, maxItems = 2),
            nowEpochMillis = 200L,
        )

        assertEquals(listOf("music", "mail"), keys)
    }

    @Test
    fun adaptiveOn_honorsConfiguredSlotLimit() {
        val keys = SmartDockRuntimePolicy.selectKeys(
            apps = apps,
            pinnedKeys = emptyList(),
            recentPackages = listOf("example.chat", "example.music", "example.mail"),
            usageSignals = emptyMap(),
            scene = SceneId.WORK,
            settings = DockSettings(adaptiveSuggestions = true, maxItems = 2),
        )

        assertEquals(2, keys.size)
    }

    @Test
    fun badgeVisibility_isAnOperationalGate() {
        assertEquals(7, SmartDockRuntimePolicy.visibleBadgeCount(7, showBadgesOnDock = true))
        assertEquals(0, SmartDockRuntimePolicy.visibleBadgeCount(7, showBadgesOnDock = false))
        assertEquals(0, SmartDockRuntimePolicy.visibleBadgeCount(-3, showBadgesOnDock = true))
    }
}
