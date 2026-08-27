package cloud.kosch.aiandroid

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cloud.kosch.aiandroid.data.WorkspaceStore
import cloud.kosch.aiandroid.data.WorkspaceWidgetBindingStore
import cloud.kosch.aiandroid.model.DeviceWidgetBinding
import cloud.kosch.aiandroid.model.WorkspaceCellBounds
import cloud.kosch.aiandroid.model.WorkspaceDocument
import cloud.kosch.aiandroid.model.WorkspaceItemContent
import cloud.kosch.aiandroid.model.WorkspacePage
import cloud.kosch.aiandroid.model.WorkspacePageEditor
import cloud.kosch.aiandroid.model.WorkspaceWidgetEditor
import java.lang.ref.WeakReference
import java.util.UUID

/**
 * Activity-recreation-safe controller for the user-facing v7 Home pages.
 *
 * The portable WorkspaceDocument remains independent from Android widget host ids. DeviceWidgetBinding
 * records are kept in a separate local store, so duplicate/restore operations cannot accidentally copy
 * a device-bound appWidgetId into another page or device.
 */
class WorkspaceHomeController(
    context: Context,
    registerAsActive: Boolean = true,
) {
    private val store = WorkspaceStore(context.applicationContext)
    private val widgetBindingStore = WorkspaceWidgetBindingStore(context.applicationContext)
    private var undoDocument: WorkspaceDocument? = null

    init {
        if (registerAsActive) activeController = WeakReference(this)
    }

    var document by mutableStateOf(store.loadWorkspaceDocument().normalized())
        private set
    var widgetBindings by mutableStateOf(widgetBindingStore.load())
        private set
    var statusMessage by mutableStateOf<String?>(null)
        private set
    var canUndo by mutableStateOf(false)
        private set

    val activePage: WorkspacePage
        get() = document.pages.firstOrNull { it.id == document.activePageId } ?: document.pages.first()

    fun reload() {
        document = store.loadWorkspaceDocument().normalized()
        widgetBindings = widgetBindingStore.load()
        undoDocument = null
        canUndo = false
    }

    fun consumeStatus() {
        statusMessage = null
    }

    fun widgetBindingFor(workspaceItemId: String): Int? = widgetBindings[workspaceItemId]

    /**
     * Removes stale or crossed device bindings only at an explicit lifecycle/host-validation gate.
     *
     * Validation is pair- and provider-aware. A host id that still exists but now belongs to a different provider
     * after restore/reuse is not accepted. This intentionally does not run on every item removal so an in-session
     * Home undo can still restore the original item id before the next lifecycle reconciliation.
     */
    fun pruneWidgetBindings(hostedProviderComponents: Map<Int, String?>): Set<Int> {
        val expectedProviders = document.pages
            .flatMap(WorkspacePage::items)
            .mapNotNull { item ->
                val widget = item.content as? WorkspaceItemContent.Widget ?: return@mapNotNull null
                val expectedProvider = widget.providerComponent ?: return@mapNotNull null
                item.id to expectedProvider
            }
            .toMap()
        val validBindings = widgetBindings.filter { (itemId, appWidgetId) ->
            val expectedProvider = expectedProviders[itemId] ?: return@filter false
            val actualProvider = hostedProviderComponents[appWidgetId] ?: return@filter false
            expectedProvider == actualProvider
        }
        val released = widgetBindingStore.prune(validBindings)
        widgetBindings = widgetBindingStore.load()
        return released
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

    fun duplicateActivePage() {
        if (!isUserPage()) {
            statusMessage = "Legacy-Szenenseiten können nicht dupliziert werden"
            return
        }
        val source = activePage
        val updated = runCatching {
            WorkspacePageEditor.duplicateUserPage(
                document = document,
                sourcePageId = source.id,
                pageId = "page:user:${UUID.randomUUID()}",
                title = "",
                newItemIds = source.items.map { "item:user:${UUID.randomUUID()}" },
            )
        }.getOrElse {
            statusMessage = it.message ?: "Home-Seite konnte nicht dupliziert werden"
            return
        }
        persist(updated, "Home-Seite dupliziert · Widgets müssen auf der Kopie neu zugeordnet werden")
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

    fun compactActivePage() {
        if (!isUserPage()) {
            statusMessage = "Legacy-Szenenseiten bleiben unverändert"
            return
        }
        val updated = runCatching {
            WorkspacePageEditor.compactUserPage(document, activePage.id)
        }.getOrElse {
            statusMessage = it.message ?: "Home-Seite konnte nicht automatisch angeordnet werden"
            return
        }
        if (updated == document) {
            statusMessage = "Home-Seite ist bereits kompakt angeordnet"
            return
        }
        persist(updated, "Home-Seite automatisch angeordnet")
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

    /** Returns true only when both portable placement and device-local host binding succeed. */
    fun addWidget(appWidgetId: Int, providerComponent: String?): Boolean {
        if (appWidgetId <= 0) {
            statusMessage = "Ungültige Android-Widget-ID"
            return false
        }
        val itemId = placePortableItem(
            kindLabel = "Widget",
            add = { source, pageId, newItemId ->
                WorkspaceWidgetEditor.addWidget(
                    document = source,
                    pageId = pageId,
                    itemId = newItemId,
                    providerComponent = providerComponent,
                )
            },
        ) ?: return false

        val bound = widgetBindingStore.bind(DeviceWidgetBinding(itemId, appWidgetId))
        widgetBindings = widgetBindingStore.load()
        if (!bound) {
            statusMessage = "Widget platziert, aber Gerätebindung konnte nicht gespeichert werden · neu zuordnen"
            return false
        }
        statusMessage = "Widget zum Homescreen hinzugefügt"
        return true
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

    fun resizeItem(itemId: String, columnSpan: Int, rowSpan: Int) {
        val updated = runCatching {
            WorkspacePageEditor.resizeItem(
                document = document,
                pageId = activePage.id,
                itemId = itemId,
                columnSpan = columnSpan,
                rowSpan = rowSpan,
            )
        }.getOrElse {
            statusMessage = it.message ?: "Element konnte nicht skaliert werden"
            return
        }
        if (updated == document) {
            statusMessage = "Für diese Größe ist kein freier Bereich verfügbar"
            return
        }
        persist(updated, "Element auf ${columnSpan}×${rowSpan} skaliert")
    }

    fun moveItemToPage(
        itemId: String,
        targetPageId: String,
        bounds: WorkspaceCellBounds? = null,
    ) {
        val sourcePageId = activePage.id
        val updated = runCatching {
            WorkspacePageEditor.moveItemToPage(
                document = document,
                sourcePageId = sourcePageId,
                targetPageId = targetPageId,
                itemId = itemId,
                requestedBounds = bounds,
            )
        }.getOrElse {
            statusMessage = it.message ?: "Element konnte nicht auf die Zielseite verschoben werden"
            return
        }
        if (updated == document) {
            statusMessage = "Auf der Zielseite ist kein freier Platz"
            return
        }
        persist(updated, "Element auf ${updated.pages.first { it.id == targetPageId }.title} verschoben")
    }

    fun adjacentUserPageId(delta: Int): String? {
        if (delta == 0 || activePage.sceneAdapter != null) return null
        val userPages = document.pages.filter { it.sceneAdapter == null }
        val index = userPages.indexOfFirst { it.id == activePage.id }
        if (index < 0) return null
        return userPages.getOrNull(index + delta)?.id
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
        it.content is WorkspaceItemContent.App ||
            it.content is WorkspaceItemContent.Folder ||
            it.content is WorkspaceItemContent.Widget
    }

    private fun placePortableItem(
        kindLabel: String,
        add: (WorkspaceDocument, String, String) -> WorkspaceDocument,
    ): String? {
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
                return null
            }
            targetPage = working.pages.first { it.id == working.activePageId }
        }

        val itemId = "item:user:${UUID.randomUUID()}"
        val firstAttempt = runCatching {
            add(working, requireNotNull(targetPage).id, itemId)
        }
        val updated = firstAttempt.getOrElse { failure ->
            if (failure !is IllegalStateException) {
                statusMessage = failure.message ?: "$kindLabel konnte nicht platziert werden"
                return null
            }
            val withNewPage = runCatching {
                WorkspacePageEditor.createUserPage(
                    working,
                    "page:user:${UUID.randomUUID()}",
                    "",
                )
            }.getOrElse {
                statusMessage = "Homescreen ist voll und es kann keine weitere Seite erstellt werden"
                return null
            }
            runCatching {
                add(withNewPage, withNewPage.activePageId, itemId)
            }.getOrElse {
                statusMessage = it.message ?: "$kindLabel konnte nicht platziert werden"
                return null
            }
        }
        return itemId.takeIf { persist(updated, "$kindLabel zum Homescreen hinzugefügt") }
    }

    private fun persist(
        updated: WorkspaceDocument,
        message: String?,
        rememberUndo: Boolean = true,
    ): Boolean {
        val normalized = updated.normalized()
        if (normalized == document) {
            message?.let { statusMessage = it }
            return false
        }
        if (!store.saveWorkspaceDocument(normalized)) {
            statusMessage = "Homescreen konnte nicht dauerhaft gespeichert werden"
            return false
        }
        if (rememberUndo) {
            undoDocument = document
            canUndo = true
        }
        document = normalized
        message?.let { statusMessage = it }
        return true
    }

    companion object {
        private var activeController: WeakReference<WorkspaceHomeController>? = null

        /**
         * Refreshes the active launcher controller after another in-process component committed Workspace state.
         * A weak reference avoids extending the controller lifetime; after process death normal persisted restore wins.
         */
        fun notifyPersistedChange() {
            activeController?.get()?.reload()
        }
    }
}
