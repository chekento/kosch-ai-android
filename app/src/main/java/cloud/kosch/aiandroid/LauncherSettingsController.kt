package cloud.kosch.aiandroid

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cloud.kosch.aiandroid.data.LauncherSettingsStore
import cloud.kosch.aiandroid.data.WorkspaceStore
import cloud.kosch.aiandroid.model.AccessibilitySettings
import cloud.kosch.aiandroid.model.AdvancedSettings
import cloud.kosch.aiandroid.model.AiSettings
import cloud.kosch.aiandroid.model.AppSpaceSettings
import cloud.kosch.aiandroid.model.AppearanceSettings
import cloud.kosch.aiandroid.model.AutomationSettings
import cloud.kosch.aiandroid.model.BackupSettings
import cloud.kosch.aiandroid.model.DockSettings
import cloud.kosch.aiandroid.model.FolderSettings
import cloud.kosch.aiandroid.model.GestureSettings
import cloud.kosch.aiandroid.model.HomeSettings
import cloud.kosch.aiandroid.model.LauncherAssistantSettings
import cloud.kosch.aiandroid.model.LauncherSettingsDocument
import cloud.kosch.aiandroid.model.NotificationSettings
import cloud.kosch.aiandroid.model.PageSettings
import cloud.kosch.aiandroid.model.PenSettings
import cloud.kosch.aiandroid.model.PrivacySettings
import cloud.kosch.aiandroid.model.SearchSettings
import cloud.kosch.aiandroid.model.SettingsSection
import cloud.kosch.aiandroid.model.SystemIntegrationSettings
import cloud.kosch.aiandroid.model.ThemeSettings
import cloud.kosch.aiandroid.model.VoiceSettings
import cloud.kosch.aiandroid.model.WidgetSettings
import cloud.kosch.aiandroid.model.WorkspaceDocument
import cloud.kosch.aiandroid.model.WorkspaceGridReflow

/**
 * Activity-recreation-safe state holder for the versioned launcher Settings Center.
 *
 * The controller owns only portable launcher preferences. Credentials, widget host ids and Android grants live in
 * their dedicated stores. Disruptive Home changes are coordinated with the v7 Workspace document before settings
 * are committed so a failed reflow cannot leave settings and workspace out of sync. Every portable settings domain
 * has one explicit apply route and therefore shares the same atomic store + single-level undo semantics.
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

    fun applyPages(settings: PageSettings): Boolean =
        persistSection(document.copy(pages = settings), "Seiten-Einstellungen gespeichert")

    fun applyApps(settings: AppSpaceSettings): Boolean =
        persistSection(document.copy(apps = settings), "App-Drawer-Einstellungen gespeichert")

    fun applyDock(settings: DockSettings): Boolean =
        persistSection(document.copy(dock = settings.normalized()), "Dock-Einstellungen gespeichert")

    fun applyFolders(settings: FolderSettings): Boolean =
        persistSection(document.copy(folders = settings), "Ordner-Einstellungen gespeichert")

    fun applyWidgets(settings: WidgetSettings): Boolean =
        persistSection(document.copy(widgets = settings.normalized()), "Widget-Einstellungen gespeichert")

    fun applyAppearance(settings: AppearanceSettings): Boolean =
        persistSection(document.copy(appearance = settings.normalized()), "Darstellung gespeichert")

    fun applyTheme(settings: ThemeSettings): Boolean =
        persistSection(document.copy(theme = settings), "Theme-Einstellungen gespeichert")

    fun applyAssistant(settings: LauncherAssistantSettings): Boolean =
        persistSection(document.copy(assistant = settings.normalized()), "Assistent-Darstellung gespeichert")

    fun applyAi(settings: AiSettings): Boolean =
        persistSection(document.copy(ai = settings.normalized()), "KI-Routing gespeichert")

    fun applyVoice(settings: VoiceSettings): Boolean =
        persistSection(document.copy(voice = settings), "Portable Sprachpräferenzen gespeichert")

    fun applyGestures(settings: GestureSettings): Boolean =
        persistSection(document.copy(gestures = settings.normalized()), "Gesten gespeichert")

    /**
     * Quick Personalize edits gesture and appearance drafts together. Persist both in one whole-document commit so
     * a storage failure can never leave gestures updated while the icon selection still reflects the previous state.
     */
    fun applyQuickPersonalization(
        gestures: GestureSettings,
        appearance: AppearanceSettings,
    ): Boolean = persistSection(
        document.copy(
            gestures = gestures.normalized(),
            appearance = appearance.normalized(),
        ),
        "Gesten & Icons gespeichert",
    )

    fun applySearch(settings: SearchSettings): Boolean =
        persistSection(document.copy(search = settings), "Such-Einstellungen gespeichert")

    fun applyNotifications(settings: NotificationSettings): Boolean =
        persistSection(document.copy(notifications = settings), "Badge-Einstellungen gespeichert")

    fun applyPen(settings: PenSettings): Boolean =
        persistSection(document.copy(pen = settings), "Smartpen-Einstellungen gespeichert")

    fun applyAutomation(settings: AutomationSettings): Boolean =
        persistSection(document.copy(automation = settings), "Automations-Einstellungen gespeichert")

    fun applyAccessibility(settings: AccessibilitySettings): Boolean =
        persistSection(document.copy(accessibility = settings), "Barrierefreiheits-Einstellungen gespeichert")

    fun applyPrivacy(settings: PrivacySettings): Boolean =
        persistSection(document.copy(privacy = settings.normalized()), "Datenschutz-Einstellungen gespeichert")

    fun applyBackup(settings: BackupSettings): Boolean =
        persistSection(document.copy(backup = settings), "Backup-Einstellungen gespeichert")

    fun applySystem(settings: SystemIntegrationSettings): Boolean =
        persistSection(document.copy(system = settings), "Systemintegrations-Einstellungen gespeichert")

    fun applyAdvanced(settings: AdvancedSettings): Boolean =
        persistSection(document.copy(advanced = settings), "Erweiterte Einstellungen gespeichert")

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
