package cloud.kosch.aiandroid.data

import cloud.kosch.aiandroid.model.PortableSettingValue
import cloud.kosch.aiandroid.model.ScopedSettingsDocument
import cloud.kosch.aiandroid.model.WorkspaceCellBounds
import cloud.kosch.aiandroid.model.WorkspaceDocument
import cloud.kosch.aiandroid.model.WorkspaceItem
import cloud.kosch.aiandroid.model.WorkspaceItemContent
import cloud.kosch.aiandroid.model.WorkspacePage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class ScopedSettingsCodecTest {
    @Test
    fun pageAndObjectOverrides_roundTripDeterministically() {
        val source = ScopedSettingsDocument()
            .withPageOverride("page:user:one", "appearance.parallax", PortableSettingValue.Bool(true))
            .withPageOverride("page:user:one", "home.icon.scale", PortableSettingValue.Decimal(1.15))
            .withObjectOverride("item:user:mail", "home.icon.scale", PortableSettingValue.Decimal(1.35))

        val first = ScopedSettingsCodec.encode(source)
        val decoded = ScopedSettingsCodec.decode(first)
        val second = ScopedSettingsCodec.encode(decoded)

        assertEquals(first, second)
        assertEquals(PortableSettingValue.Bool(true), decoded.pageOverride("page:user:one", "appearance.parallax"))
        assertEquals(PortableSettingValue.Decimal(1.35), decoded.objectOverride("item:user:mail", "home.icon.scale"))
    }

    @Test
    fun inherit_isRepresentedByMissingRecord() {
        val document = ScopedSettingsDocument()
            .withPageOverride("page:user:one", "home.icon.scale", PortableSettingValue.Decimal(1.2))
            .withPageOverride("page:user:one", "home.icon.scale", null)

        assertNull(document.pageOverride("page:user:one", "home.icon.scale"))
        assertEquals("schema=1\n", ScopedSettingsCodec.encode(document))
    }

    @Test
    fun deviceAndSessionFeatures_cannotBeSerializedAsPortableOverrides() {
        assertThrows(IllegalArgumentException::class.java) {
            ScopedSettingsDocument().withPageOverride(
                "page:user:one",
                "assistant.screen_awareness",
                PortableSettingValue.Bool(true),
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            ScopedSettingsDocument().withObjectOverride(
                "item:user:one",
                "widgets.host_id",
                PortableSettingValue.Integer(42),
            )
        }
    }

    @Test
    fun reconciliation_removesOnlyOrphanOwners() {
        val workspace = WorkspaceDocument(
            activePageId = "page:user:kept",
            pages = listOf(
                WorkspacePage(
                    id = "page:user:kept",
                    title = "Kept",
                    order = 0,
                    items = listOf(
                        WorkspaceItem(
                            id = "item:user:kept",
                            bounds = WorkspaceCellBounds(0, 0, 1, 1),
                            content = WorkspaceItemContent.App("app:mail"),
                        ),
                    ),
                ),
            ),
        )
        val source = ScopedSettingsDocument()
            .withPageOverride("page:user:kept", "home.icon.scale", PortableSettingValue.Decimal(1.1))
            .withPageOverride("page:user:orphan", "home.icon.scale", PortableSettingValue.Decimal(1.3))
            .withObjectOverride("item:user:kept", "home.icon.scale", PortableSettingValue.Decimal(1.2))
            .withObjectOverride("item:user:orphan", "home.icon.scale", PortableSettingValue.Decimal(1.4))

        val pruned = source.prunedTo(workspace)
        assertEquals(PortableSettingValue.Decimal(1.1), pruned.pageOverride("page:user:kept", "home.icon.scale"))
        assertEquals(PortableSettingValue.Decimal(1.2), pruned.objectOverride("item:user:kept", "home.icon.scale"))
        assertNull(pruned.pageOverride("page:user:orphan", "home.icon.scale"))
        assertNull(pruned.objectOverride("item:user:orphan", "home.icon.scale"))
    }
}
