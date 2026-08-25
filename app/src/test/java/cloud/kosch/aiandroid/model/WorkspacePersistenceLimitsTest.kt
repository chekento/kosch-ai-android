package cloud.kosch.aiandroid.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkspacePersistenceLimitsTest {
    @Test
    fun portableReferences_rejectValuesBeyondPersistenceContract() {
        assertRejected { WorkspaceItemContent.App("a".repeat(MAX_WORKSPACE_REFERENCE_LENGTH + 1)) }
        assertRejected { WorkspaceItemContent.Folder("f".repeat(MAX_WORKSPACE_REFERENCE_LENGTH + 1)) }
        assertRejected { WorkspaceItemContent.Widget("w".repeat(MAX_WORKSPACE_REFERENCE_LENGTH + 1)) }
        assertRejected {
            WorkspaceItemContent.ActionTile(
                SceneId.AI,
                "t".repeat(MAX_WORKSPACE_REFERENCE_LENGTH + 1),
                TileAction.ASK,
            )
        }

        assertEquals(
            MAX_WORKSPACE_REFERENCE_LENGTH,
            WorkspaceItemContent.App("a".repeat(MAX_WORKSPACE_REFERENCE_LENGTH)).appKey.length,
        )
    }

    @Test
    fun pageItemAndActiveIds_shareCodecLengthContract() {
        val bounds = WorkspaceCellBounds(0, 0, 1, 1)
        assertRejected {
            WorkspaceItem(
                "i".repeat(MAX_WORKSPACE_ID_LENGTH + 1),
                bounds,
                WorkspaceItemContent.App("app:key"),
            )
        }
        assertRejected {
            WorkspacePage(
                "p".repeat(MAX_WORKSPACE_ID_LENGTH + 1),
                "Page",
                0,
            )
        }
        assertRejected {
            WorkspacePage(
                "page:ok",
                "T".repeat(MAX_WORKSPACE_TITLE_LENGTH + 1),
                0,
            )
        }
        assertRejected {
            WorkspaceDocument(
                activePageId = "a".repeat(MAX_WORKSPACE_ID_LENGTH + 1),
                pages = listOf(WorkspacePage("page:ok", "Page", 0)),
            ).normalized()
        }
    }

    @Test
    fun blankRecoveryInputsRemainAllowedUntilNormalization() {
        val normalized = WorkspaceDocument(
            activePageId = "",
            pages = listOf(WorkspacePage("", "", 0)),
        ).normalized()

        assertEquals(WorkspaceDocument.DEFAULT_PAGE_ID, normalized.activePageId)
        assertEquals("Home", normalized.pages.single().title)
    }

    private fun assertRejected(block: () -> Unit) {
        var rejected = false
        try {
            block()
        } catch (_: IllegalArgumentException) {
            rejected = true
        }
        assertTrue("Expected value outside the persistence contract to be rejected", rejected)
    }
}
