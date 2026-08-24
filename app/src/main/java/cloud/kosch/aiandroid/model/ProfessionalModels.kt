package cloud.kosch.aiandroid.model

data class SelectedContact(
    val displayName: String,
    val phoneNumber: String,
)

data class AppUsageSignal(
    val key: String,
    val launchCount: Int,
    val lastUsedEpochMillis: Long,
)

data class BackupPreview(
    val scene: SceneId,
    val homePage: HomePage,
    val positionCount: Int,
    val recentCount: Int,
    val pinnedCount: Int,
    val hiddenCount: Int,
    val usageSignalCount: Int,
    val folderCount: Int,
    val inkStrokeCount: Int,
    val createdAtEpochMillis: Long,
    val skippedItems: List<String>,
)

enum class AuditAction(val title: String) {
    APP_LAUNCH("App gestartet"),
    APP_SHORTCUT("App-Shortcut gestartet"),
    APP_VISIBILITY("App-Sichtbarkeit geändert"),
    APP_UNINSTALL_REQUEST("Deinstallation angefordert"),
    WORK_PROFILE("Arbeitsprofil geändert"),
    COMMAND("Lokaler Befehl"),
    DIALER("Telefon-Wähler geöffnet"),
    MESSAGE_COMPOSER("Nachrichten-Composer geöffnet"),
    CALENDAR("Kalender geöffnet"),
    ALARMS("Wecker geöffnet"),
    CAMERA("Kamera geöffnet"),
    SYSTEM_NOTE("Systemnotiz geöffnet"),
    CONTACT_PICKER("Kontakt gewählt"),
    DOCUMENT_INSPECT("Dokument lokal geprüft"),
    FILE_WORKSPACE("Datei-Arbeitsraum geöffnet"),
    FILE_CREATE_DIRECTORY("Ordner erstellt"),
    FILE_RENAME("Dokument umbenannt"),
    FILE_DELETE("Dokument gelöscht"),
    FILE_REFRESH("Dateiansicht aktualisiert"),
    SYSTEM_PANEL("Systembereich geöffnet"),
    SCENE_SWITCH("Szene gewechselt"),
    LAYOUT_CHANGE("Layout geändert"),
    WIDGET_CHANGE("Widget geändert"),
    BACKUP_EXPORT("Backup exportiert"),
    BACKUP_IMPORT("Backup importiert"),
    AUDIT_EXPORT("Audit exportiert"),
    AUDIT_CLEAR("Audit gelöscht"),
    PEN_SAVE("Pen-Inhalt gespeichert"),
    PEN_EXPORT("Pen-Inhalt exportiert"),
    PERSONALIZATION_RESET("Lernsignale gelöscht"),
}

enum class AuditOutcome(val title: String) {
    SUCCESS("Erfolgreich"),
    REJECTED("Abgelehnt"),
    FAILED("Fehlgeschlagen"),
}

data class AuditEvent(
    val timestampEpochMillis: Long,
    val action: AuditAction,
    val outcome: AuditOutcome,
)
