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
    )

    @Test
    fun `folder proposal is deterministic and local`() {
        val proposal = LocalSmartOrganizer.proposeFolders(apps)
        assertEquals(proposal, LocalSmartOrganizer.proposeFolders(apps.reversed()))
        assertTrue(proposal.any { it.kind == FolderKind.AI && "ai" in it.appKeys })
        assertTrue(proposal.any { it.kind == FolderKind.COMMUNICATION && "mail" in it.appKeys })
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
}
