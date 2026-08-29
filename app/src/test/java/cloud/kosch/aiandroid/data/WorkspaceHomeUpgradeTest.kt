package cloud.kosch.aiandroid.data

import cloud.kosch.aiandroid.model.SceneId
import cloud.kosch.aiandroid.model.WorkspaceDocument
import cloud.kosch.aiandroid.model.WorkspacePage
import cloud.kosch.aiandroid.model.WorkspaceStableIds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkspaceHomeUpgradeTest {
    @Test
    fun sceneOnlyDocument_getsOneCleanHomeWithoutLosingScenePages() {
        val scenes = SceneId.entries.mapIndexed { index, scene ->
            WorkspacePage(
                id = WorkspaceStableIds.scenePage(scene),
                title = scene.title,
                order = index,
                sceneAdapter = scene,
            )
        }
        val source = WorkspaceDocument(
            activePageId = scenes.first().id,
            pages = scenes,
        )

        val upgraded = WorkspaceHomeUpgrade.ensureCleanHome(source)

        assertEquals(SceneId.entries.size + 1, upgraded.pages.size)
        assertEquals(WorkspaceDocument.DEFAULT_PAGE_ID, upgraded.activePageId)
        assertEquals("Home", upgraded.pages.first().title)
        assertEquals(null, upgraded.pages.first().sceneAdapter)
        assertTrue(upgraded.pages.drop(1).map { it.id }.containsAll(scenes.map { it.id }))
    }

    @Test
    fun existingUserHome_isIdempotentAndKeepsActivePage() {
        val source = WorkspaceDocument(
            activePageId = "page:user:work",
            pages = listOf(
                WorkspacePage("page:user:home", "Home", 0),
                WorkspacePage("page:user:work", "Work", 1),
            ),
        ).normalized()

        val upgraded = WorkspaceHomeUpgrade.ensureCleanHome(source)

        assertEquals(source, upgraded)
    }
}
