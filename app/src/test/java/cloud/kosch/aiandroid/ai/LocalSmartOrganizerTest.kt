package cloud.kosch.aiandroid.ai

import cloud.kosch.aiandroid.model.FolderKind
import cloud.kosch.aiandroid.model.SceneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalSmartOrganizerTest {
    private val apps = listOf(
        SmartAppDescriptor("camera", "Kamera", "com.android.camera"),
        SmartAppDescriptor("mail", "E-Mail", "com.example.mail"),
        SmartAppDescriptor("files", "Dateien", "com.android.files"),
        SmartAppDescriptor("ai", "PocketPal AI", "com.pocketpalai"),
        SmartAppDescriptor("calendar", "Projekt Kalender", "com.example.calendar"),
    )

    @Test
    fun `folder proposal is deterministic and local`() {
        val proposal = LocalSmartOrganizer.proposeFolders(apps)
        assertEquals(proposal, LocalSmartOrganizer.proposeFolders(apps.reversed()))
        assertTrue(proposal.any { it.kind == FolderKind.AI && "ai" in it.appKeys })
        assertTrue(proposal.any { it.kind == FolderKind.COMMUNICATION && "mail" in it.appKeys })
        assertTrue(proposal.any { it.kind == FolderKind.WORK && "calendar" in it.appKeys })
    }

    @Test
    fun `pinned apps stay ahead of scene suggestions`() {
        val dock = LocalSmartOrganizer.smartDockKeys(
            apps = apps,
            pinnedKeys = listOf("files"),
            recentPackages = listOf("com.android.camera"),
            scene = SceneId.STUDIO,
            limit = 4,
        )
        assertEquals(listOf("files", "camera"), dock.take(2))
        assertEquals(dock.distinct(), dock)
    }

    @Test
    fun `ranking explains why every app appears`() {
        val suggestions = LocalSmartOrganizer.smartDockSuggestions(
            apps = apps,
            pinnedKeys = listOf("files"),
            recentPackages = listOf("com.android.camera"),
            usageKeys = listOf("mail"),
            scene = SceneId.WORK,
            limit = apps.size,
        )
        assertEquals(SmartDockReason.PINNED, suggestions.first { it.app.key == "files" }.primaryReason)
        assertTrue(SmartDockReason.RECENT in suggestions.first { it.app.key == "camera" }.reasons)
        assertTrue(SmartDockReason.LEARNED in suggestions.first { it.app.key == "mail" }.reasons)
        assertTrue(SmartDockReason.SCENE in suggestions.first { it.app.key == "calendar" }.reasons)
    }

    @Test
    fun `learned local usage can outrank generic scene candidates without beating pins`() {
        val suggestions = LocalSmartOrganizer.smartDockSuggestions(
            apps = apps,
            pinnedKeys = listOf("files"),
            recentPackages = emptyList(),
            usageKeys = listOf("mail"),
            scene = SceneId.STUDIO,
            limit = 5,
        )
        assertEquals("files", suggestions.first().app.key)
        val mailIndex = suggestions.indexOfFirst { it.app.key == "mail" }
        val genericAiIndex = suggestions.indexOfFirst { it.app.key == "ai" }
        assertTrue(mailIndex in 1 until genericAiIndex)
    }

    @Test
    fun `zero dock limit stays empty`() {
        assertTrue(
            LocalSmartOrganizer.smartDockSuggestions(
                apps = apps,
                pinnedKeys = emptyList(),
                recentPackages = emptyList(),
                scene = SceneId.WORK,
                limit = 0,
            ).isEmpty(),
        )
    }
}
