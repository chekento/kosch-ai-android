package cloud.kosch.aiandroid.model

data class SelectedContact(
    val displayName: String,
    val phoneNumber: String,
)

data class BackupPreview(
    val scene: SceneId,
    val homePage: HomePage,
    val positionCount: Int,
    val recentCount: Int,
    val pinnedCount: Int,
    val folderCount: Int,
    val inkStrokeCount: Int,
    val createdAtEpochMillis: Long,
    val skippedItems: List<String>,
)

enum class AuditAction(val title: String) {
    APP_LAUNCH("App gestartet"),
    APP_SHORTCUT("App-Shortcut gestartet"),
    COMMAND("Lokaler Befehl"),
    DIALER("Telefon-Wähler geöffnet"),
    CONTACT_PICKER("Kontakt gewählt"),
    DOCUMENT_INSPECT("Dokument lokal geprüft"),
    SYSTEM_PANEL("Systembereich geöffnet"),
    SCENE_SWITCH("Szene gewechselt"),
    LAYOUT_CHANGE("Layout geändert"),
    WIDGET_CHANGE("Widget geändert"),
    BACKUP_EXPORT("Backup exportiert"),
    BACKUP_IMPORT("Backup importiert"),
    AUDIT_EXPORT("Audit exportiert"),
    AUDIT_CLEAR("Audit gelöscht"),
    PEN_SAVE("Pen-Inhalt gespeichert"),
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
