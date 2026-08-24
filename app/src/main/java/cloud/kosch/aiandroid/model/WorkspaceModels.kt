package cloud.kosch.aiandroid.model

enum class SceneId(
    val title: String,
    val subtitle: String,
) {
    AI("AI", "Fragen, planen, handeln"),
    WORK("Work", "Fokus und Werkzeuge"),
    STUDIO("Studio", "Medien und Ideen"),
    SOCIAL("Social", "Menschen und Austausch"),
    EVENING("Evening", "Ruhiger Tagesausklang"),
}

enum class WorkspaceMode {
    PLAY,
    EDIT,
}

enum class HomePage(val title: String) {
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

object DefaultWorkspace {
    fun tiles(scene: SceneId): List<WorkspaceTile> = when (scene) {
        SceneId.AI -> listOf(
            tile("ask", "⌘ Ask", "Ein Eingang für alles", "✦", TileAction.ASK, 0.04f, 0.04f),
            tile("apps", "Apps", "Lokal durchsuchen", "▦", TileAction.APPS, 0.54f, 0.04f),
            tile("context", "Kontext", "Privat auf dem Gerät", "◌", TileAction.CONTEXT, 0.04f, 0.52f),
            tile("providers", "KI-Anbieter", "Du entscheidest jedes Mal", "↗", TileAction.PROVIDERS, 0.54f, 0.52f),
        )

        SceneId.WORK -> listOf(
            tile("work-ask", "Arbeitsauftrag", "Planen oder weitergeben", "✦", TileAction.ASK, 0.04f, 0.04f),
            tile("work-apps", "Work Apps", "Lokal gruppiert", "▦", TileAction.FOCUS, 0.54f, 0.04f),
            tile("work-context", "Fokuslage", "Zeit, Akku, Verbindung", "◌", TileAction.CONTEXT, 0.04f, 0.52f),
            tile("work-tools", "Werkzeuge", "Schneller Zugriff", "⌁", TileAction.TOOLS, 0.54f, 0.52f),
        )

        SceneId.STUDIO -> listOf(
            tile("studio-ask", "Ideenraum", "Prompt oder Aktion", "✦", TileAction.ASK, 0.04f, 0.04f),
            tile("studio-media", "Medien", "Kamera, Bild, Audio", "◉", TileAction.MEDIA, 0.54f, 0.04f),
            tile("studio-context", "Studio-Lage", "Audioausgabe erkennen", "◌", TileAction.CONTEXT, 0.04f, 0.52f),
            tile("studio-providers", "Kreative KI", "Anbieter auswählen", "↗", TileAction.PROVIDERS, 0.54f, 0.52f),
        )

        SceneId.SOCIAL -> listOf(
            tile("social-ask", "Nachricht planen", "Erst prüfen, dann teilen", "✦", TileAction.ASK, 0.04f, 0.04f),
            tile("social-apps", "Kontakte & Chat", "Lokal gruppiert", "◎", TileAction.COMMUNICATION, 0.54f, 0.04f),
            tile("social-context", "Erreichbarkeit", "Nur lokale Signale", "◌", TileAction.CONTEXT, 0.04f, 0.52f),
            tile("social-providers", "KI-Hilfe", "Text bewusst übergeben", "↗", TileAction.PROVIDERS, 0.54f, 0.52f),
        )

        SceneId.EVENING -> listOf(
            tile("evening-ask", "Tagesausklang", "Ordnen und reflektieren", "✦", TileAction.ASK, 0.04f, 0.04f),
            tile("evening-media", "Entspannung", "Musik und Medien", "◉", TileAction.MEDIA, 0.54f, 0.04f),
            tile("evening-context", "Abendmodus", "Zeit und Akkustand", "◌", TileAction.CONTEXT, 0.04f, 0.52f),
            tile("evening-apps", "Alle Apps", "Wenn du sie brauchst", "▦", TileAction.APPS, 0.54f, 0.52f),
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
