package cloud.kosch.aiandroid

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cloud.kosch.aiandroid.data.LauncherSettingsStore
import cloud.kosch.aiandroid.data.WorkspaceStore
import cloud.kosch.aiandroid.model.AppearanceSettings
import cloud.kosch.aiandroid.model.HomeSettings
import cloud.kosch.aiandroid.model.LauncherAssistantSettings
import cloud.kosch.aiandroid.model.LauncherSettingsDocument
import cloud.kosch.aiandroid.model.SettingsSection
import cloud.kosch.aiandroid.model.WorkspaceDocument
import cloud.kosch.aiandroid.model.WorkspaceGridReflow

/**
 * Activity-recreation-safe state holder for the versioned launcher Settings Center.
 *
 * The controller owns only portable launcher preferences. Credentials, widget host ids and Android grants live in
 * their dedicated stores. Disruptive Home changes are coordinated with the v7 Workspace document before settings
 * are committed so a failed reflow cannot leave settings and workspace out of sync.
 */
class LauncherSettingsController(context: Context) {
    private val store = LauncherSettingsStore(context.applicationContext)
    private val workspaceStore = WorkspaceStore(context.applicationContext)
    private var undoDocument: LauncherSettingsDocument? = null
    private var undoWorkspace: WorkspaceDocument? = null

    var document by mutableStateOf(store.load().normalized())
        private set
    var visible by mutableStateOf(false)
        private set
    var requestedSection by mutableStateOf<SettingsSection?>(null)
        private set
    var notice by mutableStateOf<String?>(null)
        private set
    var canUndo by mutableStateOf(false)
        private set

    fun open(section: SettingsSection? = null) {
        requestedSection = section
        visible = true
    }

    fun close() {
        visible = false
        requestedSection = null
    }

    fun consumeNotice() {
        notice = null
    }

    fun applyHome(settings: HomeSettings, home: WorkspaceHomeController): Boolean {
        val normalizedHome = settings.normalized()
        val previousSettings = document
        val previousWorkspace = home.document
        val gridChanged = previousSettings.home.gridColumns != normalizedHome.gridColumns ||
            previousSettings.home.gridRows != normalizedHome.gridRows

        val updatedWorkspace = if (gridChanged) {
            runCatching {
                WorkspaceGridReflow.reflow(
                    document = previousWorkspace,
                    columns = normalizedHome.gridColumns,
                    rows = normalizedHome.gridRows,
                )
            }.getOrElse {
                notice = it.message ?: "Raster konnte nicht ohne Datenverlust übernommen werden"
                return false
            }
        } else previousWorkspace

        if (gridChanged && !workspaceStore.saveWorkspaceDocument(updatedWorkspace)) {
            notice = "Neues Raster konnte nicht dauerhaft gespeichert werden"
            return false
        }

        val updatedSettings = previousSettings.copy(home = normalizedHome).normalized()
        if (!store.save(updatedSettings)) {
            if (gridChanged) workspaceStore.saveWorkspaceDocument(previousWorkspace)
            notice = "Home-Einstellungen konnten nicht gespeichert werden"
            return false
        }

        undoDocument = previousSettings
        undoWorkspace = previousWorkspace.takeIf { gridChanged }
        document = updatedSettings
        canUndo = true
        if (gridChanged) home.reload()
        notice = if (gridChanged) {
            "Raster auf ${normalizedHome.gridColumns}×${normalizedHome.gridRows} übernommen"
        } else {
            "Home-Einstellungen übernommen"
        }
        return true
    }

    fun applyAppearance(settings: AppearanceSettings): Boolean =
        persistSection(document.copy(appearance = settings.normalized()), "Darstellung gespeichert")

    fun applyAssistant(settings: LauncherAssistantSettings): Boolean =
        persistSection(document.copy(assistant = settings.normalized()), "Assistent-Einstellungen gespeichert")

    fun undo(home: WorkspaceHomeController): Boolean {
        val previousSettings = undoDocument ?: return false
        val currentSettings = document
        val previousWorkspace = undoWorkspace
        val currentWorkspace = home.document

        if (previousWorkspace != null && !workspaceStore.saveWorkspaceDocument(previousWorkspace)) {
            notice = "Vorheriges Raster konnte nicht wiederhergestellt werden"
            return false
        }
        if (!store.save(previousSettings)) {
            if (previousWorkspace != null) workspaceStore.saveWorkspaceDocument(currentWorkspace)
            notice = "Settings-Undo konnte nicht gespeichert werden"
            return false
        }

        undoDocument = currentSettings
        undoWorkspace = currentWorkspace.takeIf { previousWorkspace != null }
        document = previousSettings
        canUndo = true
        if (previousWorkspace != null) home.reload()
        notice = "Letzte Settings-Änderung rückgängig"
        return true
    }

    fun reload() {
        document = store.load().normalized()
        undoDocument = null
        undoWorkspace = null
        canUndo = false
    }

    private fun persistSection(updated: LauncherSettingsDocument, message: String): Boolean {
        val previous = document
        val normalized = updated.normalized()
        if (normalized == previous) {
            notice = "Keine Änderung"
            return true
        }
        if (!store.save(normalized)) {
            notice = "Einstellungen konnten nicht gespeichert werden"
            return false
        }
        undoDocument = previous
        undoWorkspace = null
        document = normalized
        canUndo = true
        notice = message
        return true
    }
}
