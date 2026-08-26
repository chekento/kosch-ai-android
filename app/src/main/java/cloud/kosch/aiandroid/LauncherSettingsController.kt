package cloud.kosch.aiandroid

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import android.content.Context
import cloud.kosch.aiandroid.data.LauncherSettingsStore
import cloud.kosch.aiandroid.model.AppearanceSettings
import cloud.kosch.aiandroid.model.HomeSettings
import cloud.kosch.aiandroid.model.LauncherAssistantSettings
import cloud.kosch.aiandroid.model.LauncherSettingsDocument
import cloud.kosch.aiandroid.model.SettingsSection

/**
 * Activity-recreation-safe state holder for the versioned launcher Settings Center.
 *
 * The controller owns only portable launcher preferences. Credentials, widget host ids and Android grants live in
 * their dedicated stores. Disruptive Home changes are coordinated with WorkspaceHomeController before settings are
 * committed so a failed reflow cannot leave settings and workspace out of sync.
 */
class LauncherSettingsController(context: Context) {
    private val store = LauncherSettingsStore(context.applicationContext)
    private var undoDocument: LauncherSettingsDocument? = null

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
        val previous = document
        val gridChanged = previous.home.gridColumns != normalizedHome.gridColumns ||
            previous.home.gridRows != normalizedHome.gridRows

        if (gridChanged && !home.applyGlobalGrid(normalizedHome.gridColumns, normalizedHome.gridRows)) {
            notice = home.statusMessage ?: "Raster konnte nicht übernommen werden"
            return false
        }

        val updated = previous.copy(home = normalizedHome).normalized()
        if (!persist(updated, previous)) {
            if (gridChanged) home.applyGlobalGrid(previous.home.gridColumns, previous.home.gridRows, rememberUndo = false)
            notice = "Home-Einstellungen konnten nicht gespeichert werden"
            return false
        }
        notice = "Home & Raster übernommen"
        return true
    }

    fun applyAppearance(settings: AppearanceSettings): Boolean =
        persistSection(document.copy(appearance = settings.normalized()), "Darstellung gespeichert")

    fun applyAssistant(settings: LauncherAssistantSettings): Boolean =
        persistSection(document.copy(assistant = settings.normalized()), "Assistent-Einstellungen gespeichert")

    fun undo(home: WorkspaceHomeController): Boolean {
        val previous = undoDocument ?: return false
        val current = document
        val gridChanged = current.home.gridColumns != previous.home.gridColumns ||
            current.home.gridRows != previous.home.gridRows
        if (gridChanged && !home.applyGlobalGrid(previous.home.gridColumns, previous.home.gridRows, rememberUndo = false)) {
            notice = "Vorheriges Raster konnte nicht wiederhergestellt werden"
            return false
        }
        if (!store.save(previous)) {
            if (gridChanged) home.applyGlobalGrid(current.home.gridColumns, current.home.gridRows, rememberUndo = false)
            notice = "Settings-Undo konnte nicht gespeichert werden"
            return false
        }
        undoDocument = current
        document = previous
        canUndo = true
        notice = "Letzte Settings-Änderung rückgängig"
        return true
    }

    fun reload() {
        document = store.load().normalized()
        undoDocument = null
        canUndo = false
    }

    private fun persistSection(updated: LauncherSettingsDocument, message: String): Boolean {
        val previous = document
        val normalized = updated.normalized()
        if (normalized == previous) {
            notice = "Keine Änderung"
            return true
        }
        if (!persist(normalized, previous)) {
            notice = "Einstellungen konnten nicht gespeichert werden"
            return false
        }
        notice = message
        return true
    }

    private fun persist(updated: LauncherSettingsDocument, previous: LauncherSettingsDocument): Boolean {
        if (!store.save(updated)) return false
        undoDocument = previous
        document = updated
        canUndo = true
        return true
    }
}
