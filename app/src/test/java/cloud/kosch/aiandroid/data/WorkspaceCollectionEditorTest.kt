package cloud.kosch.aiandroid.data

import cloud.kosch.aiandroid.model.FolderKind
import cloud.kosch.aiandroid.model.LauncherFolder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkspaceCollectionEditorTest {
    private val folder = LauncherFolder(
        id = "work",
        title = "Arbeit",
        kind = FolderKind.WORK,
        appKeys = listOf("mail", "docs", "meet"),
    )

    @Test
    fun `manual folder title is normalized and bounded`() {
        val created = WorkspaceCollectionEditor.create(
            folders = emptyList(),
            id = "manual-1",
            title = "  Mein   sehr guter Ordner  ",
            kind = FolderKind.OTHER,
        ).single()

        assertEquals("Mein sehr guter Ordner", created.title)
        assertFalse(created.generatedLocally)
    }

    @Test
    fun `removing final app removes and therefore persists deletion of empty folder`() {
        val result = WorkspaceCollectionEditor.removeApp(
            listOf(folder.copy(appKeys = listOf("mail"))),
            folder.id,
            "mail",
        )

        assertTrue(result.isEmpty())
    }

    @Test
    fun `folder apps and dock keys can be moved without duplication`() {
        assertEquals(
            listOf("docs", "mail", "meet"),
            WorkspaceCollectionEditor.move(folder.appKeys, "mail", 1),
        )
        assertEquals(
            listOf("mail", "meet", "docs"),
            WorkspaceCollectionEditor.move(folder.appKeys, "docs", 1),
        )
    }

    @Test
    fun `adding an app is idempotent`() {
        val result = WorkspaceCollectionEditor.addApp(listOf(folder), folder.id, "docs").single()
        assertEquals(folder.appKeys, result.appKeys)
    }
}
