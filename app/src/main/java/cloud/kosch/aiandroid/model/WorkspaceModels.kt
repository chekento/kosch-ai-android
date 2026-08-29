package cloud.kosch.aiandroid.model

enum class SceneId(
    val title: String,
    val subtitle: String,
) {
    AI("AI & Assistant", "Assistant, AI Hub und Modelle"),
    WORK("Work", "Fokus, Dateien und Arbeitswerkzeuge"),
    STUDIO("Media & Create", "Medien, Pen und kreative Werkzeuge"),
    SOCIAL("Kommunikation", "Telefon, Kontakte und Austausch"),
    EVENING("Tools & System", "Widgets, Einstellungen und Systemwege"),
}

enum class WorkspaceMode {
    PLAY,
    EDIT,
}

enum class HomePage(val title: String) {
    PRO_DESK("Pro Desk"),
    WORKSPACE("Workspace"),
    SMART_SPACE("Smart Space"),
    PEN_SPACE("Pen Space"),
}

enum class FolderKind(val title: String, val glyph: String) {
    COMMUNICATION("Kommunikation", "◎"),
    WORK("Arbeit", "▣"),
    MEDIA("Medien", "◉"),
    TOOLS("Werkzeuge", "⌁"),
    AI("KI", "✦"),
    OTHER("Weitere", "▦"),
}

data class LauncherFolder(
    val id: String,
    val title: String,
    val kind: FolderKind,
    val appKeys: List<String>,
    val generatedLocally: Boolean = true,
)

data class TilePosition(
    val x: Float,
    val y: Float,
) {
    fun clamped(): TilePosition = TilePosition(
        x = x.coerceIn(0f, 1f),
        y = y.coerceIn(0f, 1f),
    )
}

enum class TileAction {
    ASK,
    APPS,
    CONTEXT,
    PROVIDERS,
    FOCUS,
    MEDIA,
    COMMUNICATION,
    TOOLS,
}

data class WorkspaceTile(
    val id: String,
    val title: String,
    val subtitle: String,
    val glyph: String,
    val action: TileAction,
    val defaultPosition: TilePosition,
)

data class PositionedTile(
    val tile: WorkspaceTile,
    val position: TilePosition,
)

/**
 * Compatibility tiles are now presented as first-class KAL function spaces. Stable ids/actions remain unchanged so
 * existing layouts and backups keep working while the reference UI replaces the old dashboard visuals around them.
 */
object DefaultWorkspace {
    fun tiles(scene: SceneId): List<WorkspaceTile> = when (scene) {
        SceneId.AI -> listOf(
            tile("ask", "KAL Assistant", "Fragen und lokale Befehle", "✦", TileAction.ASK, 0.04f, 0.04f),
            tile("apps", "AI Apps", "Installierte KI-Werkzeuge", "▦", TileAction.APPS, 0.54f, 0.04f),
            tile("context", "Kontext", "Lokale Signale und Awareness", "◌", TileAction.CONTEXT, 0.04f, 0.52f),
            tile("providers", "AI Hub", "Provider, Modelle und Routing", "↗", TileAction.PROVIDERS, 0.54f, 0.52f),
        )

        SceneId.WORK -> listOf(
            tile("work-ask", "Arbeitsauftrag", "Mit KAL planen oder weitergeben", "✦", TileAction.ASK, 0.04f, 0.04f),
            tile("work-apps", "Work Apps", "Arbeits-Apps lokal gruppiert", "▦", TileAction.FOCUS, 0.54f, 0.04f),
            tile("work-context", "Arbeitskontext", "Lokale Fokus- und Gerätesignale", "◌", TileAction.CONTEXT, 0.04f, 0.52f),
            tile("work-tools", "Work Tools", "Dateien und Systemwerkzeuge", "⌁", TileAction.TOOLS, 0.54f, 0.52f),
        )

        SceneId.STUDIO -> listOf(
            tile("studio-ask", "Creative AI", "Ideen, Prompts und Planung", "✦", TileAction.ASK, 0.04f, 0.04f),
            tile("studio-media", "Media Apps", "Kamera, Bild, Audio und Video", "◉", TileAction.MEDIA, 0.54f, 0.04f),
            tile("studio-context", "Studio-Kontext", "Lokale Medien- und Gerätesignale", "◌", TileAction.CONTEXT, 0.04f, 0.52f),
            tile("studio-providers", "Kreative Modelle", "Bewusst über den AI Hub", "↗", TileAction.PROVIDERS, 0.54f, 0.52f),
        )

        SceneId.SOCIAL -> listOf(
            tile("social-ask", "Kommunikationshilfe", "Entwurf zuerst prüfen", "✦", TileAction.ASK, 0.04f, 0.04f),
            tile("social-apps", "Telefon & Chat", "Kommunikations-Apps lokal gruppiert", "◎", TileAction.COMMUNICATION, 0.54f, 0.04f),
            tile("social-context", "Erreichbarkeit", "Nur lokale Kontextsignale", "◌", TileAction.CONTEXT, 0.04f, 0.52f),
            tile("social-providers", "Text-KI", "Nur nach bewusster Providerwahl", "↗", TileAction.PROVIDERS, 0.54f, 0.52f),
        )

        SceneId.EVENING -> listOf(
            tile("evening-ask", "KAL Tools", "Launcher-Funktionen finden", "✦", TileAction.ASK, 0.04f, 0.04f),
            tile("evening-media", "Apps & Medien", "Installierte Werkzeuge öffnen", "▦", TileAction.APPS, 0.54f, 0.04f),
            tile("evening-context", "Systemkontext", "Lokale Geräteinformationen", "◌", TileAction.CONTEXT, 0.04f, 0.52f),
            tile("evening-apps", "Kontrollzentrum", "Widgets, Einstellungen und sichere Wege", "⌁", TileAction.TOOLS, 0.54f, 0.52f),
        )
    }

    private fun tile(
        id: String,
        title: String,
        subtitle: String,
        glyph: String,
        action: TileAction,
        x: Float,
        y: Float,
    ) = WorkspaceTile(
        id = id,
        title = title,
        subtitle = subtitle,
        glyph = glyph,
        action = action,
        defaultPosition = TilePosition(x, y),
    )
}

data class ContextSnapshot(
    val hour: Int,
    val minute: Int,
    val batteryPercent: Int?,
    val isCharging: Boolean,
    val hasNetwork: Boolean,
    val hasPersonalAudioOutput: Boolean,
    val suggestedScene: SceneId,
    val reasons: List<String>,
)