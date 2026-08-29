package cloud.kosch.aiandroid.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import cloud.kosch.aiandroid.model.HomePage
import cloud.kosch.aiandroid.model.SceneId
import cloud.kosch.aiandroid.model.TilePosition
import cloud.kosch.aiandroid.model.WorkspaceCellBounds
import cloud.kosch.aiandroid.model.WorkspaceDocument
import cloud.kosch.aiandroid.model.WorkspaceItem
import cloud.kosch.aiandroid.model.WorkspaceItemContent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkspaceStoreInstrumentationTest {
    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    private val preferences
        get() = context.getSharedPreferences(WORKSPACE_PREFERENCES, Context.MODE_PRIVATE)

    @Test
    fun sceneAndHomePage_roundTripThroughDevicePreferences() {
        val store = WorkspaceStore(context)
        val originalScene = store.loadScene()
        val originalHomePage = store.loadHomePage()

        try {
            store.saveScene(SceneId.WORK)
            store.saveHomePage(HomePage.SMART_SPACE)

            val reloaded = WorkspaceStore(context)
            assertEquals(SceneId.WORK, reloaded.loadScene())
            assertEquals(HomePage.SMART_SPACE, reloaded.loadHomePage())
        } finally {
            store.saveScene(originalScene)
            store.saveHomePage(originalHomePage)
        }
    }

    @Test
    fun onboardingCompletion_isDurableAcrossStoreInstances() {
        val store = WorkspaceStore(context)
        store.completeOnboarding()

        assertTrue(WorkspaceStore(context).isOnboardingComplete())
    }

    @Test
    fun constructorSeedsV7_whenPortableDocumentIsAbsent() {
        val originalRaw = rawWorkspaceV7()
        try {
            preferences.edit().remove(WorkspaceV7Persistence.KEY_WORKSPACE_DOCUMENT).commit()

            val store = WorkspaceStore(context)
            val stored = WorkspaceV7Persistence(preferences).loadStoredOrNull()

            assertNotNull(stored)
            assertEquals(WorkspaceDocument.DEFAULT_PAGE_ID, stored!!.activePageId)
            assertEquals(WorkspaceDocument.DEFAULT_PAGE_ID, stored.pages.first().id)
            assertTrue(stored.pages.any { it.sceneAdapter == store.loadScene() })
        } finally {
            restoreRawWorkspaceV7(originalRaw)
        }
    }

    @Test
    fun saveScene_updatesLegacyState_withoutHijackingPersonalHome() {
        val store = WorkspaceStore(context)
        val originalScene = store.loadScene()
        val originalRaw = rawWorkspaceV7()
        try {
            WorkspaceV7Persistence(preferences).save(store.loadWorkspaceDocument())

            store.saveScene(SceneId.WORK)

            val reloaded = WorkspaceStore(context)
            assertEquals(SceneId.WORK, reloaded.loadScene())
            val document = reloaded.loadWorkspaceDocument()
            assertEquals(WorkspaceDocument.DEFAULT_PAGE_ID, document.activePageId)
            assertTrue(document.pages.any { it.sceneAdapter == SceneId.WORK })
        } finally {
            store.saveScene(originalScene)
            restoreRawWorkspaceV7(originalRaw)
        }
    }

    @Test
    fun savePositions_mirrorsLegacyBounds_withoutDroppingV7OnlyItems() {
        val store = WorkspaceStore(context)
        val originalRaw = rawWorkspaceV7()
        val originalAsk = store.loadPositions().getValue(SceneId.AI).getValue("ask")
        val custom = WorkspaceItem(
            id = "item:instrumentation-custom-app",
            bounds = WorkspaceCellBounds(10, 10, 1, 1),
            content = WorkspaceItemContent.App("0:cloud.kosch.instrumentation"),
        )
        try {
            val enriched = store.loadWorkspaceDocument().let { document ->
                document.copy(
                    pages = document.pages.map { page ->
                        if (page.sceneAdapter == SceneId.AI) {
                            page.copy(items = page.items.filterNot { it.id == custom.id } + custom)
                        } else {
                            page
                        }
                    },
                ).normalized()
            }
            WorkspaceV7Persistence(preferences).save(enriched)

            store.savePositions(
                SceneId.AI,
                mapOf("ask" to TilePosition(0.51f, 0.49f)),
            )

            val mirrored = WorkspaceStore(context).loadWorkspaceDocument()
            assertTrue(mirrored.pages.flatMap { it.items }.any { it == custom })
            val ask = mirrored.pages
                .single { it.sceneAdapter == SceneId.AI }
                .items
                .single { (it.content as? WorkspaceItemContent.ActionTile)?.legacyTileId == "ask" }
            assertEquals(WorkspaceCellBounds(6, 6, 6, 6), ask.bounds)
        } finally {
            store.savePositions(SceneId.AI, mapOf("ask" to originalAsk))
            restoreRawWorkspaceV7(originalRaw)
        }
    }

    @Test
    fun corruptFutureV7_survivesLegacyWritesByteForByte() {
        val originalRaw = rawWorkspaceV7()
        val store = WorkspaceStore(context)
        val originalScene = store.loadScene()
        val futureRaw = "{\"schemaVersion\":999,\"marker\":\"preserve-me\"}"
        try {
            preferences.edit()
                .putString(WorkspaceV7Persistence.KEY_WORKSPACE_DOCUMENT, futureRaw)
                .commit()

            store.saveScene(SceneId.STUDIO)

            assertEquals(SceneId.STUDIO, WorkspaceStore(context).loadScene())
            assertEquals(futureRaw, rawWorkspaceV7())
        } finally {
            store.saveScene(originalScene)
            restoreRawWorkspaceV7(originalRaw)
        }
    }

    @Test
    fun portableRestore_reconcilesLegacyAdapters_withoutDroppingV7OnlyItems() {
        val store = WorkspaceStore(context)
        val originalRaw = rawWorkspaceV7()
        val originalScene = store.loadScene()
        val originalAsk = store.loadPositions().getValue(SceneId.AI).getValue("ask")
        val custom = WorkspaceItem(
            id = "item:restore-custom-app",
            bounds = WorkspaceCellBounds(9, 9, 1, 1),
            content = WorkspaceItemContent.App("0:cloud.kosch.restore"),
        )
        try {
            val enriched = store.loadWorkspaceDocument().let { document ->
                document.copy(
                    pages = document.pages.map { page ->
                        if (page.sceneAdapter == SceneId.AI) {
                            page.copy(items = page.items.filterNot { it.id == custom.id } + custom)
                        } else {
                            page
                        }
                    },
                ).normalized()
            }
            WorkspaceV7Persistence(preferences).save(enriched)

            store.saveScene(SceneId.SOCIAL)
            store.savePositions(SceneId.AI, mapOf("ask" to TilePosition(0.51f, 0.49f)))
            val payload = store.createPortableSnapshot()

            store.saveScene(originalScene)
            store.savePositions(SceneId.AI, mapOf("ask" to originalAsk))
            WorkspaceV7Persistence(preferences).save(enriched)

            store.restorePortableSnapshot(payload)

            val restored = WorkspaceStore(context)
            assertEquals(SceneId.SOCIAL, restored.loadScene())
            val document = restored.loadWorkspaceDocument()
            assertEquals(WorkspaceDocument.DEFAULT_PAGE_ID, document.activePageId)
            assertEquals(WorkspaceDocument.DEFAULT_PAGE_ID, document.pages.first().id)
            assertTrue(document.pages.any { it.sceneAdapter == SceneId.SOCIAL })
            assertTrue(document.pages.flatMap { it.items }.any { it == custom })
            val ask = document.pages
                .single { it.sceneAdapter == SceneId.AI }
                .items
                .single { (it.content as? WorkspaceItemContent.ActionTile)?.legacyTileId == "ask" }
            assertEquals(WorkspaceCellBounds(6, 6, 6, 6), ask.bounds)
        } finally {
            store.saveScene(originalScene)
            store.savePositions(SceneId.AI, mapOf("ask" to originalAsk))
            restoreRawWorkspaceV7(originalRaw)
        }
    }

    private fun rawWorkspaceV7(): String? =
        preferences.getString(WorkspaceV7Persistence.KEY_WORKSPACE_DOCUMENT, null)

    private fun restoreRawWorkspaceV7(raw: String?) {
        preferences.edit().apply {
            if (raw == null) {
                remove(WorkspaceV7Persistence.KEY_WORKSPACE_DOCUMENT)
            } else {
                putString(WorkspaceV7Persistence.KEY_WORKSPACE_DOCUMENT, raw)
            }
        }.commit()
    }

    private companion object {
        const val WORKSPACE_PREFERENCES = "kosch_launcher_workspace"
    }
}
