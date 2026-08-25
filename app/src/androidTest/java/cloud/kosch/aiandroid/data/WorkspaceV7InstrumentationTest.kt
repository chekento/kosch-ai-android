package cloud.kosch.aiandroid.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import cloud.kosch.aiandroid.model.SceneId
import cloud.kosch.aiandroid.model.TileAction
import cloud.kosch.aiandroid.model.WorkspaceCellBounds
import cloud.kosch.aiandroid.model.WorkspaceItem
import cloud.kosch.aiandroid.model.WorkspaceItemContent
import cloud.kosch.aiandroid.model.WorkspacePage
import cloud.kosch.aiandroid.model.WorkspaceStableIds
import cloud.kosch.aiandroid.model.WorkspaceV7Migration
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkspaceV7InstrumentationTest {
    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    private val preferences by lazy {
        context.getSharedPreferences("workspace_v7_instrumentation", Context.MODE_PRIVATE)
    }

    @Before
    fun clearBefore() {
        preferences.edit().clear().commit()
    }

    @After
    fun clearAfter() {
        preferences.edit().clear().commit()
    }

    @Test
    fun codec_roundTripsAllPortableKinds_withoutDeviceWidgetIds() {
        val migrated = WorkspaceV7Migration.fromLegacyScenePositions(SceneId.AI, emptyMap())
        val pageId = migrated.activePageId
        val page = migrated.pages.single { it.id == pageId }
        val enriched = migrated.copy(
            pages = migrated.pages.map { candidate ->
                if (candidate.id != pageId) return@map candidate
                candidate.copy(
                    items = candidate.items + listOf(
                        WorkspaceItem(
                            id = "item:app",
                            bounds = WorkspaceCellBounds(0, 0, 1, 1),
                            content = WorkspaceItemContent.App("0:cloud.kosch.example"),
                        ),
                        WorkspaceItem(
                            id = "item:folder",
                            bounds = WorkspaceCellBounds(1, 0, 1, 1),
                            content = WorkspaceItemContent.Folder("folder:work"),
                        ),
                        WorkspaceItem(
                            id = "item:widget",
                            bounds = WorkspaceCellBounds(2, 0, 2, 2),
                            content = WorkspaceItemContent.Widget("cloud.kosch.widget/.Provider"),
                        ),
                        WorkspaceItem(
                            id = "item:widget-remap",
                            bounds = WorkspaceCellBounds(4, 0, 2, 2),
                            content = WorkspaceItemContent.Widget(null),
                        ),
                    ),
                )
            },
        ).normalized()

        val encoded = WorkspaceDocumentCodec.encode(enriched)
        val decoded = WorkspaceDocumentCodec.decode(encoded)

        assertEquals(enriched, decoded)
        assertFalse(encoded.contains("appWidgetId"))
        assertFalse(encoded.contains("widgetBindings"))
        assertEquals(page.items.size + 4, decoded.pages.single { it.id == pageId }.items.size)
    }

    @Test
    fun codec_rejectsEmbeddedDeviceWidgetId_andFutureSchema() {
        val document = WorkspaceV7Migration.fromLegacyScenePositions(SceneId.AI, emptyMap())
        val root = WorkspaceDocumentCodec.toJson(document)
        val firstItem = root
            .getJSONArray("pages")
            .getJSONObject(0)
            .getJSONArray("items")
            .getJSONObject(0)
        firstItem.put("appWidgetId", 42)

        assertFails { WorkspaceDocumentCodec.decode(root.toString()) }

        val future = WorkspaceDocumentCodec.toJson(document)
            .put("schemaVersion", 8)
        assertFails { WorkspaceDocumentCodec.decode(future.toString()) }
    }

    @Test
    fun persistence_isIdempotent_andCorruptStoredValueFallsBackWithoutOverwrite() {
        val persistence = WorkspaceV7Persistence(preferences)

        assertTrue(persistence.migrateIfAbsent(SceneId.AI, emptyMap()))
        val firstRaw = preferences.getString(WorkspaceV7Persistence.KEY_WORKSPACE_DOCUMENT, null)
        assertTrue(!firstRaw.isNullOrBlank())
        assertFalse(persistence.migrateIfAbsent(SceneId.WORK, emptyMap()))
        assertEquals(firstRaw, preferences.getString(WorkspaceV7Persistence.KEY_WORKSPACE_DOCUMENT, null))

        val corruptFuture = JSONObject()
            .put("schemaVersion", 999)
            .put("marker", "preserve-me")
            .toString()
        preferences.edit()
            .putString(WorkspaceV7Persistence.KEY_WORKSPACE_DOCUMENT, corruptFuture)
            .commit()

        assertNull(persistence.loadStoredOrNull())
        val fallback = persistence.loadOrLegacyFallback(SceneId.WORK, emptyMap())
        assertEquals(WorkspaceStableIds.scenePage(SceneId.WORK), fallback.activePageId)
        assertFalse(persistence.migrateIfAbsent(SceneId.AI, emptyMap()))
        assertEquals(
            corruptFuture,
            preferences.getString(WorkspaceV7Persistence.KEY_WORKSPACE_DOCUMENT, null),
        )
    }

    @Test
    fun persistence_saveAndReload_preservesPortableDocument() {
        val persistence = WorkspaceV7Persistence(preferences)
        val source = WorkspaceV7Migration.fromLegacyScenePositions(SceneId.STUDIO, emptyMap())
        val custom = source.copy(
            pages = source.pages + WorkspacePage(
                id = "page:custom",
                title = "Custom",
                order = source.pages.size,
                items = listOf(
                    WorkspaceItem(
                        id = "item:custom-action",
                        bounds = WorkspaceCellBounds(0, 0, 2, 2),
                        content = WorkspaceItemContent.ActionTile(
                            scene = SceneId.STUDIO,
                            legacyTileId = "custom-action",
                            action = TileAction.ASK,
                        ),
                    ),
                ),
            ),
        ).normalized()

        assertTrue(persistence.save(custom))
        assertEquals(custom, persistence.loadStoredOrNull())
    }

    private fun assertFails(block: () -> Unit) {
        var failed = false
        try {
            block()
        } catch (_: RuntimeException) {
            failed = true
        }
        assertTrue("Expected operation to reject invalid workspace data", failed)
    }
}
