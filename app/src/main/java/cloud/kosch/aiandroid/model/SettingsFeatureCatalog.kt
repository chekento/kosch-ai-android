package cloud.kosch.aiandroid.model

/**
 * Product-wide registry for configurable launcher capabilities.
 *
 * This is intentionally broader than the currently live UI. Its purpose is to prevent whole feature domains
 * from disappearing while implementation proceeds section by section. Every entry has a stable id, supported
 * scope, portability boundary and maturity level. Device-local permissions, grants, Android host ids and actual
 * credentials must never become portable merely because they are visible in Settings Center.
 */
enum class SettingScope { GLOBAL, PAGE, OBJECT, DEVICE, SESSION }

enum class SettingPortability {
    PORTABLE,
    DEVICE_LOCAL,
    SESSION_ONLY,
    SENSITIVE_REFERENCE,
}

enum class SettingMaturity {
    LIVE,
    CORE_READY,
    PLANNED,
    EXPERIMENTAL,
}

data class SettingsFeatureDefinition(
    val id: String,
    val section: SettingsSection,
    val title: String,
    val scopes: Set<SettingScope>,
    val portability: SettingPortability,
    val maturity: SettingMaturity,
    val keywords: Set<String> = emptySet(),
)

object SettingsFeatureCatalog {
    private val G = setOf(SettingScope.GLOBAL)
    private val GP = setOf(SettingScope.GLOBAL, SettingScope.PAGE)
    private val GPO = setOf(SettingScope.GLOBAL, SettingScope.PAGE, SettingScope.OBJECT)
    private val GD = setOf(SettingScope.GLOBAL, SettingScope.DEVICE)
    private val D = setOf(SettingScope.DEVICE)
    private val S = setOf(SettingScope.SESSION)

    val all: List<SettingsFeatureDefinition> = buildList {
        // Home & grid
        add(f("home.grid.columns", SettingsSection.HOME, "Rasterspalten", G, SettingMaturity.LIVE))
        add(f("home.grid.rows", SettingsSection.HOME, "Rasterzeilen", G, SettingMaturity.LIVE))
        add(f("home.grid.orientation", SettingsSection.HOME, "Raster getrennt nach Hoch-/Querformat", G, SettingMaturity.PLANNED))
        add(f("home.grid.gaps", SettingsSection.HOME, "Horizontale und vertikale Abstände", G, SettingMaturity.CORE_READY))
        add(f("home.page.padding", SettingsSection.HOME, "Seitenränder und Safe-Area-Abstände", GP, SettingMaturity.PLANNED))
        add(f("home.icon.scale", SettingsSection.HOME, "Globale Icon-Größe", GPO, SettingMaturity.CORE_READY))
        add(f("home.label.mode", SettingsSection.HOME, "Label-Modus", GPO, SettingMaturity.CORE_READY))
        add(f("home.label.typography", SettingsSection.HOME, "Label-Größe, Schrift und Kontrast", GPO, SettingMaturity.PLANNED))
        add(f("home.layout.lock", SettingsSection.HOME, "Layout sperren", GP, SettingMaturity.LIVE))
        add(f("home.layout.autofill", SettingsSection.HOME, "Freie Zellen automatisch füllen", GP, SettingMaturity.LIVE))
        add(f("home.page.indicator", SettingsSection.HOME, "Seitenindikator", GP, SettingMaturity.LIVE))
        add(f("home.one_handed_zone", SettingsSection.HOME, "Einhand-Bedienzone", G, SettingMaturity.PLANNED))

        // Pages, spaces and profiles
        add(f("pages.create", SettingsSection.PAGES, "Seiten erstellen", GP, SettingMaturity.LIVE))
        add(f("pages.duplicate", SettingsSection.PAGES, "Seiten duplizieren", GP, SettingMaturity.LIVE))
        add(f("pages.delete", SettingsSection.PAGES, "Seiten löschen", GP, SettingMaturity.LIVE))
        add(f("pages.reorder", SettingsSection.PAGES, "Seitenreihenfolge", GP, SettingMaturity.LIVE))
        add(f("pages.hide", SettingsSection.PAGES, "Seiten aus Navigation ausblenden", GP, SettingMaturity.PLANNED))
        add(f("pages.looping", SettingsSection.PAGES, "Seiten-Looping", G, SettingMaturity.CORE_READY))
        add(f("pages.transition", SettingsSection.PAGES, "Übergang und Dauer", GP, SettingMaturity.CORE_READY))
        add(f("pages.grid_override", SettingsSection.PAGES, "Raster pro Seite", GP, SettingMaturity.PLANNED))
        add(f("pages.wallpaper_override", SettingsSection.PAGES, "Wallpaper pro Seite", GP, SettingMaturity.PLANNED))
        add(f("pages.dock_override", SettingsSection.PAGES, "Dock pro Seite", GP, SettingMaturity.PLANNED))
        add(f("pages.assistant_visibility", SettingsSection.PAGES, "Assistant-Sichtbarkeit pro Seite", GP, SettingMaturity.PLANNED))
        add(f("pages.profiles", SettingsSection.PAGES, "Arbeits-/Privat-/Kreativprofile", GP, SettingMaturity.PLANNED, setOf("scene", "profile", "workspace")))
        add(f("pages.cross_page_drag", SettingsSection.PAGES, "Elemente zwischen Seiten ziehen", GPO, SettingMaturity.LIVE))

        // Apps & drawer
        add(f("apps.drawer.columns", SettingsSection.APPS, "Drawer-Spalten nach Orientierung", G, SettingMaturity.CORE_READY))
        add(f("apps.drawer.direction", SettingsSection.APPS, "Vertikaler/horizontaler Drawer", G, SettingMaturity.PLANNED))
        add(f("apps.sort.mode", SettingsSection.APPS, "A–Z, Nutzung, zuletzt, manuell", G, SettingMaturity.PLANNED))
        add(f("apps.smart_ranking", SettingsSection.APPS, "Smart Ranking", G, SettingMaturity.CORE_READY))
        add(f("apps.categories", SettingsSection.APPS, "Tabs und Kategorien", G, SettingMaturity.PLANNED))
        add(f("apps.hidden", SettingsSection.APPS, "Apps ausblenden", G, SettingMaturity.PLANNED))
        add(f("apps.system_visibility", SettingsSection.APPS, "System-Apps anzeigen", G, SettingMaturity.CORE_READY))
        add(f("apps.work_profile_badges", SettingsSection.APPS, "Work-/Private-Profile-Badges", GD, SettingMaturity.CORE_READY))
        add(f("apps.missing_placeholder", SettingsSection.APPS, "Nicht verfügbare Apps als Platzhalter", GPO, SettingMaturity.PLANNED))
        add(f("apps.actions", SettingsSection.APPS, "App Info, Shortcuts und Aktionen", GPO, SettingMaturity.CORE_READY))
        add(f("apps.item_icon_override", SettingsSection.APPS, "Icon pro App überschreiben", GPO, SettingMaturity.PLANNED))
        add(f("apps.item_label_override", SettingsSection.APPS, "Name/Label pro App überschreiben", GPO, SettingMaturity.PLANNED))
        add(f("apps.custom_links", SettingsSection.APPS, "Eigene Links und sichere Verknüpfungen", GPO, SettingMaturity.PLANNED, setOf("deep link", "http", "intent", "shortcut")))

        // Dock
        add(f("dock.enabled", SettingsSection.DOCK, "Dock aktivieren", GP, SettingMaturity.CORE_READY))
        add(f("dock.slots", SettingsSection.DOCK, "Anzahl und Reihenfolge der Slots", GP, SettingMaturity.CORE_READY))
        add(f("dock.adaptive", SettingsSection.DOCK, "Adaptive Vorschläge", GP, SettingMaturity.CORE_READY))
        add(f("dock.ask", SettingsSection.DOCK, "Ask/AI-Einstieg", GP, SettingMaturity.CORE_READY))
        add(f("dock.search", SettingsSection.DOCK, "Suche/Command-Einstieg", GP, SettingMaturity.PLANNED))
        add(f("dock.opacity", SettingsSection.DOCK, "Hintergrund und Deckkraft", GP, SettingMaturity.CORE_READY))
        add(f("dock.scroll_pages", SettingsSection.DOCK, "Mehrseitiges/scrollbares Dock", GP, SettingMaturity.PLANNED))
        add(f("dock.item_actions", SettingsSection.DOCK, "Swipe-/Long-Press-Aktionen pro Slot", GPO, SettingMaturity.PLANNED))

        // Folders
        add(f("folders.presentation", SettingsSection.FOLDERS, "Sheet, Popup oder Vollbild", GPO, SettingMaturity.CORE_READY))
        add(f("folders.grid", SettingsSection.FOLDERS, "Ordner-Raster", GPO, SettingMaturity.CORE_READY))
        add(f("folders.smart", SettingsSection.FOLDERS, "Smart Groups", GPO, SettingMaturity.CORE_READY))
        add(f("folders.sort", SettingsSection.FOLDERS, "Sortierung", GPO, SettingMaturity.PLANNED))
        add(f("folders.cover", SettingsSection.FOLDERS, "Cover/Icon-Stack", GPO, SettingMaturity.PLANNED))
        add(f("folders.close_after_launch", SettingsSection.FOLDERS, "Nach App-Start schließen", GPO, SettingMaturity.CORE_READY))
        add(f("folders.gestures", SettingsSection.FOLDERS, "Ordnergesten", GPO, SettingMaturity.PLANNED))
        add(f("folders.mixed_actions", SettingsSection.FOLDERS, "Apps, Links und Actions gemeinsam", GPO, SettingMaturity.PLANNED))

        // Widgets
        add(f("widgets.first_class_home", SettingsSection.WIDGETS, "Android-Widgets als V7-Home-Element", GPO, SettingMaturity.LIVE))
        add(f("widgets.picker", SettingsSection.WIDGETS, "System-Widget-Picker", D, SettingMaturity.LIVE, portability = SettingPortability.DEVICE_LOCAL))
        add(f("widgets.resize", SettingsSection.WIDGETS, "Freies Resize", GPO, SettingMaturity.LIVE))
        add(f("widgets.provider_hints", SettingsSection.WIDGETS, "Provider-Size-Hints", GPO, SettingMaturity.CORE_READY))
        add(f("widgets.padding_scale", SettingsSection.WIDGETS, "Padding, Crop und Scale", GPO, SettingMaturity.PLANNED))
        add(f("widgets.stacks", SettingsSection.WIDGETS, "Widget Stacks", GPO, SettingMaturity.PLANNED))
        add(f("widgets.smart_stacks", SettingsSection.WIDGETS, "Kontextabhängige Smart Stacks", GPO, SettingMaturity.PLANNED))
        add(f("widgets.stack_cycle", SettingsSection.WIDGETS, "Stack-Wechsel und Auto-Cycle", GPO, SettingMaturity.CORE_READY))
        add(f("widgets.missing_provider", SettingsSection.WIDGETS, "Missing-Provider-Platzhalter und Remap", GPO, SettingMaturity.LIVE))
        add(f("widgets.host_recovery", SettingsSection.WIDGETS, "Process-Death-/Orphan-Recovery", D, SettingMaturity.LIVE, portability = SettingPortability.DEVICE_LOCAL))
        add(f("widgets.host_id", SettingsSection.WIDGETS, "Android AppWidgetHost-ID", D, SettingMaturity.CORE_READY, portability = SettingPortability.DEVICE_LOCAL))

        // Appearance & iconography
        add(f("appearance.theme_mode", SettingsSection.APPEARANCE, "System/Light/Dark/Theme", G, SettingMaturity.CORE_READY))
        add(f("appearance.material_you", SettingsSection.APPEARANCE, "Material-You-Akzente", G, SettingMaturity.LIVE))
        add(f("appearance.icon_pack", SettingsSection.APPEARANCE, "Icon Pack global", G, SettingMaturity.CORE_READY))
        add(f("appearance.icon_shape", SettingsSection.APPEARANCE, "Adaptive Icon-Maske und Form", GPO, SettingMaturity.PLANNED))
        add(f("appearance.monochrome_icons", SettingsSection.APPEARANCE, "Monochrome/Themed Icons", GPO, SettingMaturity.PLANNED))
        add(f("appearance.blur", SettingsSection.APPEARANCE, "Blur-Stärke", G, SettingMaturity.LIVE))
        add(f("appearance.opacity", SettingsSection.APPEARANCE, "Oberflächen-Deckkraft", G, SettingMaturity.LIVE))
        add(f("appearance.corner_scale", SettingsSection.APPEARANCE, "Ecken-Skalierung", G, SettingMaturity.LIVE))
        add(f("appearance.content_scale", SettingsSection.APPEARANCE, "Inhalts-Skalierung", G, SettingMaturity.LIVE))
        add(f("appearance.motion_profile", SettingsSection.APPEARANCE, "Motion Profile", G, SettingMaturity.LIVE))
        add(f("appearance.parallax", SettingsSection.APPEARANCE, "Parallax/Tiefenwirkung", GP, SettingMaturity.PLANNED))
        add(f("appearance.wallpaper_treatment", SettingsSection.APPEARANCE, "Wallpaper Dim/Blur/Farbextraktion", GP, SettingMaturity.PLANNED))

        // Themes / creator
        add(f("themes.select", SettingsSection.THEMES, "Theme wählen", G, SettingMaturity.CORE_READY))
        add(f("themes.preview", SettingsSection.THEMES, "Live Preview", G, SettingMaturity.PLANNED))
        add(f("themes.import", SettingsSection.THEMES, "Theme importieren", G, SettingMaturity.PLANNED))
        add(f("themes.export", SettingsSection.THEMES, "Theme exportieren", G, SettingMaturity.PLANNED))
        add(f("themes.partial_components", SettingsSection.THEMES, "Teilkomponenten ein-/ausschließen", G, SettingMaturity.PLANNED))
        add(f("themes.rollback", SettingsSection.THEMES, "Rollback und Verlauf", G, SettingMaturity.PLANNED))
        add(f("themes.creator", SettingsSection.THEMES, "Creator-Modus und Paket-Metadaten", G, SettingMaturity.PLANNED))
        add(f("themes.assistant_assets", SettingsSection.THEMES, "Assistant-Asset-Packs", G, SettingMaturity.CORE_READY))
        add(f("themes.sounds", SettingsSection.THEMES, "Theme-Sounds optional", G, SettingMaturity.PLANNED))

        // Assistant / agent. Behavioral truth belongs to Assistant runtimes, not the portable launcher document.
        add(f("assistant.enabled", SettingsSection.ASSISTANT, "Assistant aktiv", GD, SettingMaturity.LIVE, portability = SettingPortability.DEVICE_LOCAL))
        add(f("assistant.character", SettingsSection.ASSISTANT, "Charakterprofil", GD, SettingMaturity.LIVE, portability = SettingPortability.DEVICE_LOCAL))
        add(f("assistant.name", SettingsSection.ASSISTANT, "Rufname", GD, SettingMaturity.LIVE, portability = SettingPortability.DEVICE_LOCAL))
        add(f("assistant.presence", SettingsSection.ASSISTANT, "Portal/Ambient/Floating/Full/Agent", GD, SettingMaturity.CORE_READY, portability = SettingPortability.DEVICE_LOCAL))
        add(f("assistant.anchor", SettingsSection.ASSISTANT, "Position/Anchor", GP, SettingMaturity.LIVE))
        add(f("assistant.scale", SettingsSection.ASSISTANT, "Größe", GP, SettingMaturity.LIVE))
        add(f("assistant.opacity", SettingsSection.ASSISTANT, "Deckkraft", GP, SettingMaturity.LIVE))
        add(f("assistant.portal_spawn", SettingsSection.ASSISTANT, "Portal-/Spawn-Animation", GP, SettingMaturity.CORE_READY))
        add(f("assistant.idle_motion", SettingsSection.ASSISTANT, "Idle-Bewegung", GP, SettingMaturity.CORE_READY))
        add(f("assistant.gaze", SettingsSection.ASSISTANT, "Blicksteuerung", GP, SettingMaturity.CORE_READY))
        add(f("assistant.emotion", SettingsSection.ASSISTANT, "Emotionen", GP, SettingMaturity.CORE_READY))
        add(f("assistant.viseme", SettingsSection.ASSISTANT, "Viseme/Lippensynchronisation", GP, SettingMaturity.CORE_READY))
        add(f("assistant.wake_word", SettingsSection.ASSISTANT, "Wake Word: Aus/Computer/Name/Custom", GD, SettingMaturity.CORE_READY, portability = SettingPortability.DEVICE_LOCAL))
        add(f("assistant.wake_sensitivity", SettingsSection.ASSISTANT, "Wake-Word-Sensitivität", D, SettingMaturity.PLANNED, portability = SettingPortability.DEVICE_LOCAL))
        add(f("assistant.push_to_talk", SettingsSection.ASSISTANT, "Push-to-talk-Fallback", GD, SettingMaturity.PLANNED, portability = SettingPortability.DEVICE_LOCAL))
        add(f("assistant.screen_awareness", SettingsSection.ASSISTANT, "Screen Awareness Capability", D, SettingMaturity.LIVE, portability = SettingPortability.DEVICE_LOCAL))
        add(f("assistant.camera_awareness", SettingsSection.ASSISTANT, "Camera Awareness Capability", D, SettingMaturity.LIVE, portability = SettingPortability.DEVICE_LOCAL))
        add(f("assistant.screen_session", SettingsSection.ASSISTANT, "Aktive Screen-Share-Session", S, SettingMaturity.LIVE, portability = SettingPortability.SESSION_ONLY))
        add(f("assistant.camera_session", SettingsSection.ASSISTANT, "Aktive Kamera-Session", S, SettingMaturity.LIVE, portability = SettingPortability.SESSION_ONLY))
        add(f("assistant.visual_context", SettingsSection.ASSISTANT, "One-Shot Visual Context", S, SettingMaturity.LIVE, portability = SettingPortability.SESSION_ONLY))
        add(f("assistant.context_redaction", SettingsSection.ASSISTANT, "Crop/Redaction vor Transfer", GD, SettingMaturity.PLANNED, portability = SettingPortability.DEVICE_LOCAL))
        add(f("assistant.agent_actions", SettingsSection.ASSISTANT, "Agent-Aktionen ausführen", D, SettingMaturity.LIVE, portability = SettingPortability.DEVICE_LOCAL))
        add(f("assistant.external_confirmation", SettingsSection.ASSISTANT, "Externe Aktionen bestätigen", D, SettingMaturity.LIVE, portability = SettingPortability.DEVICE_LOCAL))
        add(f("assistant.action_log", SettingsSection.ASSISTANT, "Aktionsprotokoll und Undo", S, SettingMaturity.PLANNED, portability = SettingPortability.SESSION_ONLY))
        add(f("assistant.recorder", SettingsSection.ASSISTANT, "Capture Studio/Recorder separat", D, SettingMaturity.PLANNED, portability = SettingPortability.DEVICE_LOCAL))

        // AI & models
        add(f("ai.routing", SettingsSection.AI, "Local-first/Ask/Default Provider", G, SettingMaturity.CORE_READY))
        add(f("ai.local_model", SettingsSection.AI, "Lokales Modell", GD, SettingMaturity.CORE_READY, portability = SettingPortability.DEVICE_LOCAL))
        add(f("ai.task_routing", SettingsSection.AI, "Modell pro Aufgabe", G, SettingMaturity.PLANNED))
        add(f("ai.fallback_chain", SettingsSection.AI, "Provider-/Modell-Fallback-Kette", G, SettingMaturity.PLANNED))
        add(f("ai.context_sources", SettingsSection.AI, "Kontextquellen pro Anfrage", G, SettingMaturity.PLANNED))
        add(f("ai.context_preview", SettingsSection.AI, "Kontext vor Versand anzeigen", G, SettingMaturity.PLANNED))
        add(f("ai.vision", SettingsSection.AI, "Vision-Fähigkeit pro Modell", G, SettingMaturity.PLANNED))
        add(f("ai.tools", SettingsSection.AI, "Tool-/Agent-Fähigkeit pro Modell", G, SettingMaturity.PLANNED))
        add(f("ai.memory_policy", SettingsSection.AI, "Memory-/History-Policy", G, SettingMaturity.PLANNED))
        add(f("ai.cost_budget", SettingsSection.AI, "Kosten-/Tokenbudget", G, SettingMaturity.PLANNED))
        add(f("ai.offline_mode", SettingsSection.AI, "Strikter Offline-Modus", GD, SettingMaturity.PLANNED, portability = SettingPortability.DEVICE_LOCAL))

        // Provider/API
        add(f("api.provider_enabled", SettingsSection.API, "Provider aktivieren", G, SettingMaturity.CORE_READY))
        add(f("api.transport", SettingsSection.API, "Transport: App/Web/OpenAI-kompatibel/Custom/Local", G, SettingMaturity.CORE_READY))
        add(f("api.endpoint", SettingsSection.API, "Endpoint", G, SettingMaturity.CORE_READY))
        add(f("api.model_id", SettingsSection.API, "Model ID", G, SettingMaturity.CORE_READY))
        add(f("api.credential_alias", SettingsSection.API, "Vault-Referenz", GD, SettingMaturity.CORE_READY, portability = SettingPortability.SENSITIVE_REFERENCE))
        add(f("api.credential_secret", SettingsSection.API, "API-Key/Secret selbst", D, SettingMaturity.CORE_READY, portability = SettingPortability.DEVICE_LOCAL))
        add(f("api.connection_test", SettingsSection.API, "Verbindung testen", S, SettingMaturity.PLANNED, portability = SettingPortability.SESSION_ONLY))
        add(f("api.timeout_retry", SettingsSection.API, "Timeout/Retry/Backoff", G, SettingMaturity.PLANNED))
        add(f("api.context_default", SettingsSection.API, "Kontext standardmäßig senden", G, SettingMaturity.CORE_READY))
        add(f("api.rate_limits", SettingsSection.API, "Rate-/Parallelitätslimits", G, SettingMaturity.PLANNED))

        // Voice & audio
        add(f("voice.input", SettingsSection.VOICE, "Spracheingabe", GD, SettingMaturity.LIVE, portability = SettingPortability.DEVICE_LOCAL))
        add(f("voice.output", SettingsSection.VOICE, "Sprachausgabe", GD, SettingMaturity.LIVE, portability = SettingPortability.DEVICE_LOCAL))
        add(f("voice.stt_engine", SettingsSection.VOICE, "STT Engine/Modus", D, SettingMaturity.PLANNED, portability = SettingPortability.DEVICE_LOCAL))
        add(f("voice.tts_engine", SettingsSection.VOICE, "TTS Engine", D, SettingMaturity.PLANNED, portability = SettingPortability.DEVICE_LOCAL))
        add(f("voice.assignment", SettingsSection.VOICE, "Device-lokale Stimmenzuordnung", D, SettingMaturity.LIVE, portability = SettingPortability.DEVICE_LOCAL))
        add(f("voice.locale", SettingsSection.VOICE, "Sprache/Locale", GD, SettingMaturity.CORE_READY, portability = SettingPortability.DEVICE_LOCAL))
        add(f("voice.rate_pitch", SettingsSection.VOICE, "Sprechrate und Pitch", GD, SettingMaturity.CORE_READY, portability = SettingPortability.DEVICE_LOCAL))
        add(f("voice.audio_focus", SettingsSection.VOICE, "Audio-Fokus und Ducking", D, SettingMaturity.PLANNED, portability = SettingPortability.DEVICE_LOCAL))
        add(f("voice.interrupt", SettingsSection.VOICE, "Sprachausgabe bei Nutzereingabe unterbrechen", GD, SettingMaturity.CORE_READY, portability = SettingPortability.DEVICE_LOCAL))
        add(f("voice.bluetooth", SettingsSection.VOICE, "Bluetooth-/Headset-Verhalten", D, SettingMaturity.PLANNED, portability = SettingPortability.DEVICE_LOCAL))
        add(f("voice.vad", SettingsSection.VOICE, "Voice Activity Detection", D, SettingMaturity.PLANNED, portability = SettingPortability.DEVICE_LOCAL))

        // Gestures and input
        add(f("gestures.swipe", SettingsSection.GESTURES, "Swipe-Richtungen", GPO, SettingMaturity.CORE_READY))
        add(f("gestures.double_tap", SettingsSection.GESTURES, "Double Tap", GPO, SettingMaturity.CORE_READY))
        add(f("gestures.long_press", SettingsSection.GESTURES, "Long Press", GPO, SettingMaturity.CORE_READY))
        add(f("gestures.pinch", SettingsSection.GESTURES, "Pinch In/Out", GPO, SettingMaturity.CORE_READY))
        add(f("gestures.edge", SettingsSection.GESTURES, "Edge-Gesten", GPO, SettingMaturity.CORE_READY))
        add(f("gestures.multi_finger", SettingsSection.GESTURES, "Multi-Finger-Gesten", GPO, SettingMaturity.PLANNED))
        add(f("gestures.page_override", SettingsSection.GESTURES, "Gesten pro Seite", GP, SettingMaturity.PLANNED))
        add(f("gestures.item_override", SettingsSection.GESTURES, "Gesten pro Objekt", GPO, SettingMaturity.PLANNED))
        add(f("gestures.sensitivity", SettingsSection.GESTURES, "Sensitivität und Dead Zones", GPO, SettingMaturity.PLANNED))
        add(f("gestures.haptics", SettingsSection.GESTURES, "Haptik-Profil", GD, SettingMaturity.CORE_READY, portability = SettingPortability.DEVICE_LOCAL))
        add(f("gestures.keyboard", SettingsSection.GESTURES, "Tastatur-Shortcuts", GD, SettingMaturity.CORE_READY, portability = SettingPortability.DEVICE_LOCAL))
        add(f("gestures.mouse", SettingsSection.GESTURES, "Maus-/Trackpad-Aktionen", GD, SettingMaturity.PLANNED, portability = SettingPortability.DEVICE_LOCAL))

        // Search and command palette
        add(f("search.apps", SettingsSection.SEARCH, "Apps durchsuchen", G, SettingMaturity.CORE_READY))
        add(f("search.shortcuts", SettingsSection.SEARCH, "Shortcuts/Links durchsuchen", G, SettingMaturity.CORE_READY))
        add(f("search.settings", SettingsSection.SEARCH, "Settings durchsuchen", G, SettingMaturity.LIVE))
        add(f("search.contacts", SettingsSection.SEARCH, "Kontakte nur nach Nutzerfreigabe", GD, SettingMaturity.PLANNED, portability = SettingPortability.DEVICE_LOCAL))
        add(f("search.files", SettingsSection.SEARCH, "Dateien nur mit SAF-Grant", GD, SettingMaturity.CORE_READY, portability = SettingPortability.DEVICE_LOCAL))
        add(f("search.fuzzy", SettingsSection.SEARCH, "Fuzzy Search", G, SettingMaturity.CORE_READY))
        add(f("search.ranking", SettingsSection.SEARCH, "Ranking-Regeln", G, SettingMaturity.PLANNED))
        add(f("search.history", SettingsSection.SEARCH, "History Off/Session/Local", GD, SettingMaturity.CORE_READY, portability = SettingPortability.DEVICE_LOCAL))
        add(f("search.calculator", SettingsSection.SEARCH, "Lokaler Rechner", G, SettingMaturity.PLANNED))
        add(f("search.command_palette", SettingsSection.SEARCH, "Command Palette", G, SettingMaturity.CORE_READY))

        // Notifications
        add(f("notifications.badge_mode", SettingsSection.NOTIFICATIONS, "Badge Off/Dot/Count", GD, SettingMaturity.CORE_READY, portability = SettingPortability.DEVICE_LOCAL))
        add(f("notifications.dock", SettingsSection.NOTIFICATIONS, "Badges im Dock", GD, SettingMaturity.CORE_READY, portability = SettingPortability.DEVICE_LOCAL))
        add(f("notifications.folders", SettingsSection.NOTIFICATIONS, "Badges in Ordnern", GD, SettingMaturity.CORE_READY, portability = SettingPortability.DEVICE_LOCAL))
        add(f("notifications.access", SettingsSection.NOTIFICATIONS, "Notification-Listener-Zugriff", D, SettingMaturity.CORE_READY, portability = SettingPortability.DEVICE_LOCAL))
        add(f("notifications.content_copy", SettingsSection.NOTIFICATIONS, "Inhalte kopieren standardmäßig aus", D, SettingMaturity.CORE_READY, portability = SettingPortability.DEVICE_LOCAL))
        add(f("notifications.privacy_redaction", SettingsSection.NOTIFICATIONS, "Sensitive Inhalte redigieren", D, SettingMaturity.PLANNED, portability = SettingPortability.DEVICE_LOCAL))
        add(f("notifications.quick_actions", SettingsSection.NOTIFICATIONS, "Launcher-Schnellaktionen", D, SettingMaturity.PLANNED, portability = SettingPortability.DEVICE_LOCAL))

        // Smartpen / Pen Space
        add(f("pen.space", SettingsSection.PEN, "Pen Space", GD, SettingMaturity.CORE_READY, portability = SettingPortability.DEVICE_LOCAL))
        add(f("pen.hover", SettingsSection.PEN, "Hover", D, SettingMaturity.CORE_READY, portability = SettingPortability.DEVICE_LOCAL))
        add(f("pen.pressure", SettingsSection.PEN, "Druck", D, SettingMaturity.CORE_READY, portability = SettingPortability.DEVICE_LOCAL))
        add(f("pen.tilt", SettingsSection.PEN, "Neigung", D, SettingMaturity.CORE_READY, portability = SettingPortability.DEVICE_LOCAL))
        add(f("pen.palm_rejection", SettingsSection.PEN, "Finger ignorieren/Palm Rejection", D, SettingMaturity.CORE_READY, portability = SettingPortability.DEVICE_LOCAL))
        add(f("pen.buttons", SettingsSection.PEN, "Stift-Tasten frei belegen", GD, SettingMaturity.CORE_READY, portability = SettingPortability.DEVICE_LOCAL))
        add(f("pen.tool", SettingsSection.PEN, "Standardwerkzeug", GD, SettingMaturity.PLANNED, portability = SettingPortability.DEVICE_LOCAL))
        add(f("pen.autosave", SettingsSection.PEN, "Autosave", D, SettingMaturity.PLANNED, portability = SettingPortability.DEVICE_LOCAL))
        add(f("pen.svg_export", SettingsSection.PEN, "SVG Export", D, SettingMaturity.CORE_READY, portability = SettingPortability.DEVICE_LOCAL))
        add(f("pen.handwriting_prompt", SettingsSection.PEN, "Handschrift zu Prompt", D, SettingMaturity.PLANNED, portability = SettingPortability.DEVICE_LOCAL))
        add(f("pen.circle_to_ask", SettingsSection.PEN, "Circle-to-Ask/Lasso-to-Ask", D, SettingMaturity.PLANNED, portability = SettingPortability.DEVICE_LOCAL))

        // Automation and context
        add(f("automation.time", SettingsSection.AUTOMATION, "Zeitbasierter Kontext", GD, SettingMaturity.CORE_READY, portability = SettingPortability.DEVICE_LOCAL))
        add(f("automation.battery", SettingsSection.AUTOMATION, "Akku/Ladezustand", GD, SettingMaturity.CORE_READY, portability = SettingPortability.DEVICE_LOCAL))
        add(f("automation.bluetooth", SettingsSection.AUTOMATION, "Bluetooth-Gerätekontext", D, SettingMaturity.PLANNED, portability = SettingPortability.DEVICE_LOCAL))
        add(f("automation.wifi", SettingsSection.AUTOMATION, "WLAN-Kontext ohne Inhaltsmitschnitt", D, SettingMaturity.PLANNED, portability = SettingPortability.DEVICE_LOCAL))
        add(f("automation.location", SettingsSection.AUTOMATION, "Standort nur nach expliziter Freigabe", D, SettingMaturity.PLANNED, portability = SettingPortability.DEVICE_LOCAL))
        add(f("automation.audio", SettingsSection.AUTOMATION, "Audio-/Headset-Kontext", D, SettingMaturity.PLANNED, portability = SettingPortability.DEVICE_LOCAL))
        add(f("automation.fold_display", SettingsSection.AUTOMATION, "Fold-/Display-Kontext", D, SettingMaturity.PLANNED, portability = SettingPortability.DEVICE_LOCAL))
        add(f("automation.scene_suggestions", SettingsSection.AUTOMATION, "Szenen-/Profilvorschläge", GD, SettingMaturity.CORE_READY, portability = SettingPortability.DEVICE_LOCAL))
        add(f("automation.layout_preview", SettingsSection.AUTOMATION, "Layout-Änderungen erst als Preview", GD, SettingMaturity.CORE_READY, portability = SettingPortability.DEVICE_LOCAL))
        add(f("automation.dry_run", SettingsSection.AUTOMATION, "Rule Dry Run", GD, SettingMaturity.PLANNED, portability = SettingPortability.DEVICE_LOCAL))
        add(f("automation.confirmation", SettingsSection.AUTOMATION, "Bestätigung nach Risikostufe", GD, SettingMaturity.PLANNED, portability = SettingPortability.DEVICE_LOCAL))
        add(f("automation.logs", SettingsSection.AUTOMATION, "Regelprotokoll", D, SettingMaturity.PLANNED, portability = SettingPortability.DEVICE_LOCAL))

        // Accessibility
        add(f("accessibility.reduced_motion", SettingsSection.ACCESSIBILITY, "Reduced Motion", GD, SettingMaturity.CORE_READY, portability = SettingPortability.DEVICE_LOCAL))
        add(f("accessibility.high_contrast", SettingsSection.ACCESSIBILITY, "High Contrast", GD, SettingMaturity.PLANNED, portability = SettingPortability.DEVICE_LOCAL))
        add(f("accessibility.large_targets", SettingsSection.ACCESSIBILITY, "Große Touch-Ziele", GD, SettingMaturity.PLANNED, portability = SettingPortability.DEVICE_LOCAL))
        add(f("accessibility.text_scale", SettingsSection.ACCESSIBILITY, "Große Textskalierung", GD, SettingMaturity.PLANNED, portability = SettingPortability.DEVICE_LOCAL))
        add(f("accessibility.talkback", SettingsSection.ACCESSIBILITY, "TalkBack-Semantik", D, SettingMaturity.CORE_READY, portability = SettingPortability.DEVICE_LOCAL))
        add(f("accessibility.switch_access", SettingsSection.ACCESSIBILITY, "Switch Access", D, SettingMaturity.PLANNED, portability = SettingPortability.DEVICE_LOCAL))
        add(f("accessibility.color_profiles", SettingsSection.ACCESSIBILITY, "Farbsehprofile", GD, SettingMaturity.PLANNED, portability = SettingPortability.DEVICE_LOCAL))
        add(f("accessibility.focus_order", SettingsSection.ACCESSIBILITY, "Fokusreihenfolge", GPO, SettingMaturity.PLANNED))
        add(f("accessibility.cues", SettingsSection.ACCESSIBILITY, "Haptische/Akustische Hinweise", GD, SettingMaturity.PLANNED, portability = SettingPortability.DEVICE_LOCAL))

        // Privacy & security
        add(f("privacy.usage_learning", SettingsSection.PRIVACY, "Usage Learning", D, SettingMaturity.PLANNED, portability = SettingPortability.DEVICE_LOCAL))
        add(f("privacy.retention", SettingsSection.PRIVACY, "Lokale Aufbewahrungsdauer", D, SettingMaturity.PLANNED, portability = SettingPortability.DEVICE_LOCAL))
        add(f("privacy.audit", SettingsSection.PRIVACY, "Audit-Protokoll", D, SettingMaturity.CORE_READY, portability = SettingPortability.DEVICE_LOCAL))
        add(f("privacy.network_master", SettingsSection.PRIVACY, "Netzwerkfeatures Master-Gate", D, SettingMaturity.PLANNED, portability = SettingPortability.DEVICE_LOCAL))
        add(f("privacy.context_preview", SettingsSection.PRIVACY, "Kontext vor externem Versand", D, SettingMaturity.PLANNED, portability = SettingPortability.DEVICE_LOCAL))
        add(f("privacy.redaction", SettingsSection.PRIVACY, "Sensitive Daten redigieren", D, SettingMaturity.PLANNED, portability = SettingPortability.DEVICE_LOCAL))
        add(f("privacy.clipboard", SettingsSection.PRIVACY, "Clipboard-Zugriffspolitik", D, SettingMaturity.PLANNED, portability = SettingPortability.DEVICE_LOCAL))
        add(f("privacy.capture_exclusions", SettingsSection.PRIVACY, "Sensitive Apps/Flächen von Capture ausschließen", D, SettingMaturity.PLANNED, portability = SettingPortability.DEVICE_LOCAL))
        add(f("privacy.vault", SettingsSection.PRIVACY, "Credential Vault", D, SettingMaturity.CORE_READY, portability = SettingPortability.DEVICE_LOCAL))
        add(f("privacy.purge", SettingsSection.PRIVACY, "Lokale Daten gezielt löschen", D, SettingMaturity.PLANNED, portability = SettingPortability.DEVICE_LOCAL))
        add(f("privacy.sensor_indicators", SettingsSection.PRIVACY, "MIC/SCREEN/CAM/ACTING-Indikatoren", S, SettingMaturity.LIVE, portability = SettingPortability.SESSION_ONLY))

        // Backup, restore, migration
        add(f("backup.encrypted_export", SettingsSection.BACKUP, "Verschlüsselter Export", D, SettingMaturity.CORE_READY, portability = SettingPortability.DEVICE_LOCAL))
        add(f("backup.import", SettingsSection.BACKUP, "Import", D, SettingMaturity.CORE_READY, portability = SettingPortability.DEVICE_LOCAL))
        add(f("backup.partial", SettingsSection.BACKUP, "Teilbereiche auswählen", G, SettingMaturity.PLANNED))
        add(f("backup.preview_diff", SettingsSection.BACKUP, "Restore Preview/Diff", D, SettingMaturity.PLANNED, portability = SettingPortability.DEVICE_LOCAL))
        add(f("backup.conflicts", SettingsSection.BACKUP, "Konfliktstrategie", D, SettingMaturity.PLANNED, portability = SettingPortability.DEVICE_LOCAL))
        add(f("backup.rollback", SettingsSection.BACKUP, "Restore-Rollback", D, SettingMaturity.PLANNED, portability = SettingPortability.DEVICE_LOCAL))
        add(f("backup.schema_migration", SettingsSection.BACKUP, "Schema-Migration", G, SettingMaturity.CORE_READY))
        add(f("backup.exclude_secrets", SettingsSection.BACKUP, "Secrets ausschließen", D, SettingMaturity.CORE_READY, portability = SettingPortability.DEVICE_LOCAL))
        add(f("backup.exclude_device_ids", SettingsSection.BACKUP, "Widget-/Grant-/Voice-Geräte-IDs ausschließen", D, SettingMaturity.LIVE, portability = SettingPortability.DEVICE_LOCAL))

        // System & device integration
        add(f("system.default_home", SettingsSection.SYSTEM, "Default-Home-Rolle", D, SettingMaturity.LIVE, portability = SettingPortability.DEVICE_LOCAL))
        add(f("system.rotation", SettingsSection.SYSTEM, "Rotation/Orientierung", GD, SettingMaturity.PLANNED, portability = SettingPortability.DEVICE_LOCAL))
        add(f("system.foldables", SettingsSection.SYSTEM, "Foldable-/Hinge-Layout", D, SettingMaturity.PLANNED, portability = SettingPortability.DEVICE_LOCAL))
        add(f("system.external_display", SettingsSection.SYSTEM, "Externe Displays", D, SettingMaturity.PLANNED, portability = SettingPortability.DEVICE_LOCAL))
        add(f("system.windowing", SettingsSection.SYSTEM, "Desktop-/Fenstermodus", D, SettingMaturity.PLANNED, portability = SettingPortability.DEVICE_LOCAL))
        add(f("system.keyboard_mouse", SettingsSection.SYSTEM, "Keyboard/Maus-Erkennung", D, SettingMaturity.CORE_READY, portability = SettingPortability.DEVICE_LOCAL))
        add(f("system.stylus_detection", SettingsSection.SYSTEM, "Smartpen-/Stylus-Erkennung", D, SettingMaturity.CORE_READY, portability = SettingPortability.DEVICE_LOCAL))
        add(f("system.work_profiles", SettingsSection.SYSTEM, "Work/Private Profile Integration", D, SettingMaturity.PLANNED, portability = SettingPortability.DEVICE_LOCAL))
        add(f("system.permissions_status", SettingsSection.SYSTEM, "Widget/Notification/Camera/Projection Status", D, SettingMaturity.PLANNED, portability = SettingPortability.DEVICE_LOCAL))
        add(f("system.battery_saver", SettingsSection.SYSTEM, "Battery-Saver-Reaktion", D, SettingMaturity.PLANNED, portability = SettingPortability.DEVICE_LOCAL))

        // Advanced / diagnostics / performance
        add(f("advanced.safe_mode", SettingsSection.ADVANCED, "Launcher Safe Mode", D, SettingMaturity.PLANNED, portability = SettingPortability.DEVICE_LOCAL))
        add(f("advanced.section_reset", SettingsSection.ADVANCED, "Einzelnen Bereich zurücksetzen", GD, SettingMaturity.PLANNED, portability = SettingPortability.DEVICE_LOCAL))
        add(f("advanced.full_reset", SettingsSection.ADVANCED, "Gesamtrücksetzung", D, SettingMaturity.PLANNED, portability = SettingPortability.DEVICE_LOCAL))
        add(f("advanced.diagnostics_export", SettingsSection.ADVANCED, "Diagnoseexport", D, SettingMaturity.PLANNED, portability = SettingPortability.DEVICE_LOCAL))
        add(f("advanced.performance_stats", SettingsSection.ADVANCED, "Render-/Start-/Memory-Statistik", D, SettingMaturity.PLANNED, portability = SettingPortability.DEVICE_LOCAL))
        add(f("advanced.render_quality", SettingsSection.ADVANCED, "Renderqualität/Effektbudget", GD, SettingMaturity.PLANNED, portability = SettingPortability.DEVICE_LOCAL))
        add(f("advanced.refresh_motion", SettingsSection.ADVANCED, "Refresh-/Motion-Qualitätsprofil", D, SettingMaturity.PLANNED, portability = SettingPortability.DEVICE_LOCAL))
        add(f("advanced.cache", SettingsSection.ADVANCED, "Cache verwalten", D, SettingMaturity.PLANNED, portability = SettingPortability.DEVICE_LOCAL))
        add(f("advanced.experimental", SettingsSection.ADVANCED, "Experimentelle Flags", D, SettingMaturity.PLANNED, portability = SettingPortability.DEVICE_LOCAL))
        add(f("advanced.developer", SettingsSection.ADVANCED, "Developer Mode", D, SettingMaturity.PLANNED, portability = SettingPortability.DEVICE_LOCAL))
        add(f("advanced.crash_recovery", SettingsSection.ADVANCED, "Crash-/Process-Death-Recovery", D, SettingMaturity.CORE_READY, portability = SettingPortability.DEVICE_LOCAL))
        add(f("advanced.compatibility", SettingsSection.ADVANCED, "Kompatibilitätsmodus", D, SettingMaturity.PLANNED, portability = SettingPortability.DEVICE_LOCAL))
    }

    fun forSection(section: SettingsSection): List<SettingsFeatureDefinition> = all.filter { it.section == section }

    fun search(query: String): List<SettingsFeatureDefinition> {
        val needle = query.trim().lowercase()
        if (needle.isBlank()) return all
        return all.filter { feature ->
            feature.id.contains(needle) ||
                feature.title.lowercase().contains(needle) ||
                feature.section.title.lowercase().contains(needle) ||
                feature.keywords.any { it.lowercase().contains(needle) }
        }
    }

    private fun f(
        id: String,
        section: SettingsSection,
        title: String,
        scopes: Set<SettingScope>,
        maturity: SettingMaturity,
        keywords: Set<String> = emptySet(),
        portability: SettingPortability = SettingPortability.PORTABLE,
    ) = SettingsFeatureDefinition(
        id = id,
        section = section,
        title = title,
        scopes = scopes,
        portability = portability,
        maturity = maturity,
        keywords = keywords,
    )
}
