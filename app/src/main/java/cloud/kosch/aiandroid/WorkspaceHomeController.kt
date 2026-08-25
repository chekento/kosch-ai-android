package cloud.kosch.aiandroid

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cloud.kosch.aiandroid.data.WorkspaceStore
import cloud.kosch.aiandroid.model.WorkspaceCellBounds
import cloud.kosch.aiandroid.model.WorkspaceDocument
import cloud.kosch.aiandroid.model.WorkspaceItemContent
import cloud.kosch.aiandroid.model.WorkspacePage
import cloud.kosch.aiandroid.model.WorkspacePageEditor
import java.util.UUID

/**
 * Activity-recreation-safe controller for the user-facing v7 Home pages.
 *
 * The legacy scene controller remains available while Stage B rolls out. This controller writes only the
 * portable v7 workspace document and keeps one in-memory undo checkpoint for destructive/move operations.
 */
class WorkspaceHomeController(context: Context) {
    private val store = WorkspaceStore(context.applicationContext)
    private var undoDocument: WorkspaceDocument? = null

    var document by mutableStateOf(store.loadWorkspaceDocument().normalized())
        private set
    var statusMessage by mutableStateOf<String?>(null)
        private set
    var canUndo by mutableStateOf(false)
        private set

    val activePage: WorkspacePage
        get() = document.pages.firstOrNull { it.id == document.activePageId } ?: document.pages.first()

    fun reload() {
        document = store.loadWorkspaceDocument().normalized()
        undoDocument = null
        canUndo = false
    }

    fun consumeStatus() {
        statusMessage = null
    }

    fun createPage(title: String = "") {
        val updated = runCatching {
            WorkspacePageEditor.createUserPage(
                document = document,
                pageId = "page:user:${UUID.randomUUID()}",
                title = title,
            )
        }.getOrElse {
            statusMessage = it.message ?: "Home-Seite konnte nicht erstellt werden"
            return
        }
        persist(updated, "Neue Home-Seite erstellt")
    }

    fun activatePage(pageId: String) {
        val updated = runCatching { WorkspacePageEditor.activatePage(document, pageId) }
            .getOrElse {
                statusMessage = it.message ?: "Home-Seite konnte nicht geöffnet werden"
                return
            }
        persist(updated, null, rememberUndo = false)
    }

    fun renameActivePage(title: String) {
        val updated = runCatching {
            WorkspacePageEditor.renameUserPage(document, activePage.id, title)
        }.getOrElse {
            statusMessage = it.message ?: "Home-Seite konnte nicht umbenannt werden"
            return
        }
        persist(updated, "Home-Seite umbenannt")
    }

    fun deleteActiveUserPage() {
        val updated = runCatching { WorkspacePageEditor.deleteUserPage(document, activePage.id) }
            .getOrElse {
                statusMessage = it.message ?: "Diese Seite kann nicht gelöscht werden"
                return
            }
        persist(updated, "Home-Seite gelöscht")
    }

    fun moveActivePage(delta: Int) {
        val updated = runCatching { WorkspacePageEditor.movePage(document, activePage.id, delta) }
            .getOrElse {
                statusMessage = it.message ?: "Home-Seite konnte nicht verschoben werden"
                return
            }
        persist(updated, "Seitenreihenfolge geändert")
    }

    fun addApp(appKey: String) {
        placePortableItem(
            kindLabel = "App",
            add = { source, pageId, itemId -> WorkspacePageEditor.addApp(source, pageId, itemId, appKey) },
        )
    }

    fun addFolder(folderId: String) {
        placePortableItem(
            kindLabel = "Ordner",
            add = { source, pageId, itemId -> WorkspacePageEditor.addFolder(source, pageId, itemId, folderId) },
        )
    }

    fun moveItemBy(itemId: String, columns: Int, rows: Int) {
        val item = activePage.items.firstOrNull { it.id == itemId }
        if (item == null) {
            statusMessage = "Element wurde nicht gefunden"
            return
        }
        val requested = item.bounds.copy(
            column = item.bounds.column + columns,
            row = item.bounds.row + rows,
        )
        val updated = runCatching {
            WorkspacePageEditor.moveItem(document, activePage.id, itemId, requested)
        }.getOrElse {
            statusMessage = it.message ?: "Element konnte nicht verschoben werden"
            return
        }
        if (updated == document) {
            statusMessage = "Kein freier Zielbereich in dieser Richtung"
            return
        }
        persist(updated, "Element verschoben")
    }

    fun moveItemTo(itemId: String, bounds: WorkspaceCellBounds) {
        val updated = runCatching {
            WorkspacePageEditor.moveItem(document, activePage.id, itemId, bounds)
        }.getOrElse {
            statusMessage = it.message ?: "Element konnte nicht verschoben werden"
            return
        }
        if (updated != document) persist(updated, "Element verschoben")
    }

    fun removeItem(itemId: String) {
        val updated = runCatching { WorkspacePageEditor.removeItem(document, activePage.id, itemId) }
            .getOrElse {
                statusMessage = it.message ?: "Element konnte nicht entfernt werden"
                return
            }
        persist(updated, "Element vom Homescreen entfernt")
    }

    fun undo() {
        val previous = undoDocument ?: return
        val current = document
        if (!store.saveWorkspaceDocument(previous)) {
            statusMessage = "Rückgängig konnte nicht gespeichert werden"
            return
        }
        document = previous
        undoDocument = current
        canUndo = true
        statusMessage = "Letzte Homescreen-Änderung rückgängig"
    }

    fun isUserPage(page: WorkspacePage = activePage): Boolean = page.sceneAdapter == null

    fun visiblePortableItems(page: WorkspacePage = activePage) = page.items.filter {
        it.content is WorkspaceItemContent.App || it.content is WorkspaceItemContent.Folder
    }

    private fun placePortableItem(
        kindLabel: String,
        add: (WorkspaceDocument, String, String) -> WorkspaceDocument,
    ) {
        var working = document
        var targetPage = working.pages.firstOrNull { it.id == working.activePageId }
        if (targetPage?.sceneAdapter != null) {
            working = runCatching {
                WorkspacePageEditor.createUserPage(
                    working,
                    "page:user:${UUID.randomUUID()}",
                    "",
                )
            }.getOrElse {
                statusMessage = it.message ?: "Keine freie Home-Seite verfügbar"
                return
            }
            targetPage = working.pages.first { it.id == working.activePageId }
        }

        val firstAttempt = runCatching {
            add(working, requireNotNull(targetPage).id, "item:user:${UUID.randomUUID()}")
        }
        val updated = firstAttempt.getOrElse { failure ->
            if (failure !is IllegalStateException) {
                statusMessage = failure.message ?: "$kindLabel konnte nicht platziert werden"
                return
            }
            val withNewPage = runCatching {
                WorkspacePageEditor.createUserPage(
                    working,
                    "page:user:${UUID.randomUUID()}",
                    "",
                )
            }.getOrElse {
                statusMessage = "Homescreen ist voll und es kann keine weitere Seite erstellt werden"
                return
            }
            runCatching {
                add(withNewPage, withNewPage.activePageId, "item:user:${UUID.randomUUID()}")
            }.getOrElse {
                statusMessage = it.message ?: "$kindLabel konnte nicht platziert werden"
                return
            }
        }
        persist(updated, "$kindLabel zum Homescreen hinzugefügt")
    }

    private fun persist(
        updated: WorkspaceDocument,
        message: String?,
        rememberUndo: Boolean = true,
    ) {
        val normalized = updated.normalized()
        if (normalized == document) {
            message?.let { statusMessage = it }
            return
        }
        if (!store.saveWorkspaceDocument(normalized)) {
            statusMessage = "Homescreen konnte nicht dauerhaft gespeichert werden"
            return
        }
        if (rememberUndo) {
            undoDocument = document
            canUndo = true
        }
        document = normalized
        message?.let { statusMessage = it }
    }
}
