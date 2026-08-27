package cloud.kosch.aiandroid.model

/**
 * Portable, versioned launcher preferences.
 *
 * Secrets, Android host IDs, URI grants and other device-bound capabilities NEVER belong in this document.
 * Provider credentials are referenced only by an opaque credentialAlias that resolves through the local vault.
 */
const val LAUNCHER_SETTINGS_SCHEMA_VERSION = 1

const val MIN_GRID_COLUMNS = 4
const val MAX_GRID_COLUMNS = 24
const val MIN_GRID_ROWS = 4
const val MAX_GRID_ROWS = 32
const val MAX_PROVIDER_SETTINGS = 24
const val MAX_GESTURE_BINDINGS = 48

enum class SettingsSection(val title: String) {
    HOME("Home & Raster"),
    PAGES("Seiten & Räume"),
    APPS("Apps & Drawer"),
    DOCK("Dock & Schnellzugriff"),
    FOLDERS("Ordner & Smart Groups"),
    WIDGETS("Widgets & Stacks"),
    APPEARANCE("Darstellung"),
    THEMES("Themes, Import & Export"),
    ASSISTANT("Assistent"),
    AI("KI & Modelle"),
    API("APIs & Provider"),
    VOICE("Sprache & Audio"),
    GESTURES("Gesten & Eingabe"),
    SEARCH("Suche & Command Palette"),
    NOTIFICATIONS("Benachrichtigungen & Badges"),
    PEN("Smartpen & Pen Space"),
    AUTOMATION("Automationen & Kontext"),
    ACCESSIBILITY("Barrierefreiheit"),
    PRIVACY("Datenschutz & Sicherheit"),
    BACKUP("Backup, Restore & Migration"),
    SYSTEM("Android & Systemintegration"),
    ADVANCED("Erweitert & Diagnose"),
}

enum class LabelMode { ALWAYS, SMART, NEVER }
enum class PageTransition { NONE, SLIDE, DEPTH, FADE, PARALLAX }
enum class MotionProfile { OFF, REDUCED, BALANCED, EXPRESSIVE }
enum class ThemeMode { SYSTEM, LIGHT, DARK, THEME_DEFINED }
enum class WallpaperMode { SYSTEM, THEME, PAGE_SPECIFIC, SOLID }
enum class AssistantAnchor { LEFT, CENTER, RIGHT, FREE }
enum class AssistantWakeMode { MANUAL, VOICE_BUTTON, CONTEXTUAL }
enum class AiRoutingMode { LOCAL_FIRST, ASK_EVERY_TIME, DEFAULT_PROVIDER }
enum class ProviderTransport { APP_HANDOFF, WEB, OPENAI_COMPATIBLE, CUSTOM_HTTP, LOCAL_RUNTIME }
enum class GestureTrigger {
    SWIPE_UP,
    SWIPE_DOWN,
    SWIPE_LEFT,
    SWIPE_RIGHT,
    DOUBLE_TAP,
    LONG_PRESS,
    TWO_FINGER_TAP,
    PINCH_IN,
    PINCH_OUT,
    EDGE_LEFT,
    EDGE_RIGHT,
    STYLUS_BUTTON_PRIMARY,
    STYLUS_BUTTON_SECONDARY,
}
enum class GestureAction {
    NONE,
    OPEN_DRAWER,
    OPEN_SEARCH,
    OPEN_COMMAND_PALETTE,
    OPEN_HOME_STUDIO,
    OPEN_SETTINGS,
    OPEN_ASSISTANT,
    OPEN_NOTIFICATIONS,
    PREVIOUS_PAGE,
    NEXT_PAGE,
    LOCK_DEVICE_ROUTE,
    SYSTEM_QUICK_SETTINGS,
    CUSTOM_SHORTCUT,
}
enum class BadgeMode { OFF, DOT, COUNT }
enum class HapticProfile { OFF, LIGHT, STANDARD, STRONG }
enum class SearchHistoryMode { OFF, SESSION_ONLY, LOCAL_PERSISTENT }
enum class WidgetStackSwitchMode { SWIPE, TAP_EDGE, AUTO_CYCLE }

data class LauncherSettingsDocument(
    val schemaVersion: Int = LAUNCHER_SETTINGS_SCHEMA_VERSION,
    val home: HomeSettings = HomeSettings(),
    val pages: PageSettings = PageSettings(),
    val apps: AppSpaceSettings = AppSpaceSettings(),
    val dock: DockSettings = DockSettings(),
    val folders: FolderSettings = FolderSettings(),
    val widgets: WidgetSettings = WidgetSettings(),
    val appearance: AppearanceSettings = AppearanceSettings(),
    val theme: ThemeSettings = ThemeSettings(),
    val assistant: LauncherAssistantSettings = LauncherAssistantSettings(),
    val ai: AiSettings = AiSettings(),
    val voice: VoiceSettings = VoiceSettings(),
    val gestures: GestureSettings = GestureSettings(),
    val search: SearchSettings = SearchSettings(),
    val notifications: NotificationSettings = NotificationSettings(),
    val pen: PenSettings = PenSettings(),
    val automation: AutomationSettings = AutomationSettings(),
    val accessibility: AccessibilitySettings = AccessibilitySettings(),
    val privacy: PrivacySettings = PrivacySettings(),
    val backup: BackupSettings = BackupSettings(),
    val system: SystemIntegrationSettings = SystemIntegrationSettings(),
    val advanced: AdvancedSettings = AdvancedSettings(),
) {
    fun normalized(): LauncherSettingsDocument {
        require(schemaVersion == LAUNCHER_SETTINGS_SCHEMA_VERSION) {
            "Unsupported launcher settings schema $schemaVersion"
        }
        return copy(
            home = home.normalized(),
            dock = dock.normalized(),
            widgets = widgets.normalized(),
            appearance = appearance.normalized(),
            assistant = assistant.normalized(),
            ai = ai.normalized(),
            gestures = gestures.normalized(),
            privacy = privacy.normalized(),
        )
    }
}

data class HomeSettings(
    val gridColumns: Int = 12,
    val gridRows: Int = 12,
    val horizontalGapDp: Int = 6,
    val verticalGapDp: Int = 6,
    val iconScale: Float = 1f,
    val labelMode: LabelMode = LabelMode.SMART,
    val lockLayout: Boolean = false,
    val autoFillEmptyCells: Boolean = false,
    val showPageIndicator: Boolean = true,
) {
    fun normalized() = copy(
        gridColumns = gridColumns.coerceIn(MIN_GRID_COLUMNS, MAX_GRID_COLUMNS),
        gridRows = gridRows.coerceIn(MIN_GRID_ROWS, MAX_GRID_ROWS),
        horizontalGapDp = horizontalGapDp.coerceIn(0, 32),
        verticalGapDp = verticalGapDp.coerceIn(0, 32),
        iconScale = iconScale.coerceIn(0.5f, 1.75f),
    )
}

data class PageSettings(
    val loopingEnabled: Boolean = false,
    val transition: PageTransition = PageTransition.DEPTH,
    val transitionDurationMs: Int = 280,
    val rememberLastPage: Boolean = true,
    val allowPerPageWallpaper: Boolean = true,
    val allowPerPageGridOverride: Boolean = true,
)

data class AppSpaceSettings(
    val showLabels: Boolean = true,
    val showWorkProfileBadges: Boolean = true,
    val hideSystemApps: Boolean = false,
    val drawerColumnsPortrait: Int = 5,
    val drawerColumnsLandscape: Int = 8,
    val smartRankingEnabled: Boolean = true,
    val alphabeticalIndexEnabled: Boolean = true,
)

data class DockSettings(
    val enabled: Boolean = true,
    val maxItems: Int = 7,
    val iconScale: Float = 1f,
    val adaptiveSuggestions: Boolean = true,
    val showAskButton: Boolean = true,
    val backgroundOpacity: Float = 0.92f,
) {
    fun normalized() = copy(
        maxItems = maxItems.coerceIn(0, 12),
        iconScale = iconScale.coerceIn(0.5f, 1.75f),
        backgroundOpacity = backgroundOpacity.coerceIn(0f, 1f),
    )
}

data class FolderSettings(
    val openAsSheet: Boolean = true,
    val columns: Int = 4,
    val showLabels: Boolean = true,
    val smartFoldersEnabled: Boolean = true,
    val closeAfterLaunch: Boolean = true,
)

data class WidgetSettings(
    val allowFreeResize: Boolean = true,
    val defaultColumnSpan: Int = 4,
    val defaultRowSpan: Int = 3,
    val stackSwitchMode: WidgetStackSwitchMode = WidgetStackSwitchMode.SWIPE,
    val stackAutoCycleSeconds: Int = 0,
    val haptics: HapticProfile = HapticProfile.LIGHT,
    val showMissingProviderPlaceholder: Boolean = true,
) {
    fun normalized() = copy(
        defaultColumnSpan = defaultColumnSpan.coerceIn(1, MAX_GRID_COLUMNS),
        defaultRowSpan = defaultRowSpan.coerceIn(1, MAX_GRID_ROWS),
        stackAutoCycleSeconds = stackAutoCycleSeconds.coerceIn(0, 3_600),
    )
}

data class AppearanceSettings(
    val mode: ThemeMode = ThemeMode.THEME_DEFINED,
    val motionProfile: MotionProfile = MotionProfile.BALANCED,
    val wallpaperMode: WallpaperMode = WallpaperMode.THEME,
    val blurStrength: Float = 0.45f,
    val surfaceOpacity: Float = 0.92f,
    val cornerScale: Float = 1f,
    val contentScale: Float = 1f,
    val useMaterialYouAccents: Boolean = true,
    val iconPackPackage: String? = null,
) {
    fun normalized() = copy(
        blurStrength = blurStrength.coerceIn(0f, 1f),
        surfaceOpacity = surfaceOpacity.coerceIn(0.25f, 1f),
        cornerScale = cornerScale.coerceIn(0.5f, 1.8f),
        contentScale = contentScale.coerceIn(0.75f, 1.5f),
        iconPackPackage = iconPackPackage?.trim()?.take(240)?.ifBlank { null },
    )
}

data class ThemeSettings(
    val activeThemeId: String = "neural-glass",
    val allowThemeLayoutOverrides: Boolean = true,
    val allowThemeAssistantAssets: Boolean = true,
    val allowThemeSounds: Boolean = false,
    val includeWallpaperInThemeExport: Boolean = true,
    val includeLayoutInThemeExport: Boolean = true,
)

data class LauncherAssistantSettings(
    val enabled: Boolean = false,
    val assistantId: String = "default",
    val anchor: AssistantAnchor = AssistantAnchor.RIGHT,
    val scale: Float = 1f,
    val opacity: Float = 1f,
    val wakeMode: AssistantWakeMode = AssistantWakeMode.MANUAL,
    val idleMotionEnabled: Boolean = true,
    val portalAnimationEnabled: Boolean = true,
    val gazeTrackingEnabled: Boolean = true,
    val emotionAnimationEnabled: Boolean = true,
    val visemeLipSyncEnabled: Boolean = true,
    val liveChatEnabled: Boolean = true,
    val voiceInputEnabled: Boolean = true,
    val speechOutputEnabled: Boolean = false,
    val hideOutsideAssistantPages: Boolean = false,
) {
    fun normalized() = copy(
        assistantId = assistantId.trim().take(160).ifBlank { "default" },
        scale = scale.coerceIn(0.35f, 2.5f),
        opacity = opacity.coerceIn(0.2f, 1f),
    )
}

data class AiSettings(
    val routingMode: AiRoutingMode = AiRoutingMode.LOCAL_FIRST,
    val defaultProviderId: String? = null,
    val networkProvidersEnabled: Boolean = false,
    val localCommandPlannerEnabled: Boolean = true,
    val localModelEnabled: Boolean = false,
    val providers: List<ProviderSettings> = emptyList(),
) {
    fun normalized(): AiSettings {
        val unique = providers
            .map(ProviderSettings::normalized)
            .filter { it.providerId.isNotBlank() }
            .distinctBy(ProviderSettings::providerId)
            .take(MAX_PROVIDER_SETTINGS)
        val validDefault = defaultProviderId?.takeIf { id -> unique.any { it.providerId == id } }
        return copy(
            defaultProviderId = validDefault,
            providers = unique,
            routingMode = if (routingMode == AiRoutingMode.DEFAULT_PROVIDER && validDefault == null) {
                AiRoutingMode.ASK_EVERY_TIME
            } else routingMode,
        )
    }
}

data class ProviderSettings(
    val providerId: String,
    val enabled: Boolean = false,
    val transport: ProviderTransport = ProviderTransport.APP_HANDOFF,
    val endpoint: String? = null,
    val modelId: String? = null,
    val credentialAlias: String? = null,
    val sendContextByDefault: Boolean = false,
) {
    fun normalized() = copy(
        providerId = providerId.trim().take(120),
        endpoint = endpoint?.trim()?.take(1_024)?.ifBlank { null },
        modelId = modelId?.trim()?.take(240)?.ifBlank { null },
        credentialAlias = credentialAlias?.trim()?.take(160)?.ifBlank { null },
    )
}

data class VoiceSettings(
    val inputEnabled: Boolean = true,
    val outputEnabled: Boolean = false,
    val speechRate: Float = 1f,
    val pitch: Float = 1f,
    val preferredLocaleTag: String? = null,
    val interruptSpeechOnUserInput: Boolean = true,
)

data class GestureSettings(
    val enabled: Boolean = true,
    val haptics: HapticProfile = HapticProfile.LIGHT,
    val bindings: List<GestureBinding> = defaultGestureBindings(),
) {
    fun normalized() = copy(
        bindings = bindings
            .distinctBy(GestureBinding::trigger)
            .take(MAX_GESTURE_BINDINGS),
    )
}

data class GestureBinding(
    val trigger: GestureTrigger,
    val action: GestureAction,
    val customTarget: String? = null,
)

data class SearchSettings(
    val fuzzySearchEnabled: Boolean = true,
    val searchApps: Boolean = true,
    val searchShortcuts: Boolean = true,
    val searchFilesOnlyWhenExplicitlyGranted: Boolean = true,
    val showCommandSuggestions: Boolean = true,
    val historyMode: SearchHistoryMode = SearchHistoryMode.SESSION_ONLY,
)

data class NotificationSettings(
    val badgeMode: BadgeMode = BadgeMode.DOT,
    val showBadgesOnDock: Boolean = true,
    val showBadgesInFolders: Boolean = true,
    val copyNotificationContent: Boolean = false,
)

data class PenSettings(
    val penSpaceEnabled: Boolean = true,
    val hoverPreviewEnabled: Boolean = true,
    val pressureEnabled: Boolean = true,
    val tiltEnabled: Boolean = true,
    val ignoreFingerInPenSpace: Boolean = true,
    val primaryButtonAction: GestureAction = GestureAction.OPEN_COMMAND_PALETTE,
    val secondaryButtonAction: GestureAction = GestureAction.OPEN_ASSISTANT,
)

data class AutomationSettings(
    val contextualSuggestionsEnabled: Boolean = true,
    val automaticLayoutChangesAllowed: Boolean = false,
    val sceneSuggestionsEnabled: Boolean = true,
    val batteryAwareEffects: Boolean = true,
    val timeAwareSuggestions: Boolean = true,
)

data class AccessibilitySettings(
    val reducedMotion: Boolean = false,
    val highContrast: Boolean = false,
    val largeTouchTargets: Boolean = false,
    val announcePageChanges: Boolean = true,
    val preferTextAlongsideIcons: Boolean = false,
)

data class PrivacySettings(
    val localUsageLearningEnabled: Boolean = true,
    val notificationAccessEnabledByUser: Boolean = false,
    val auditEnabled: Boolean = true,
    val auditRetentionDays: Int = 90,
    val allowNetworkFeatures: Boolean = false,
    val requireContextPreviewBeforeProviderHandoff: Boolean = true,
) {
    fun normalized() = copy(auditRetentionDays = auditRetentionDays.coerceIn(1, 365))
}

data class BackupSettings(
    val includeLauncherSettings: Boolean = true,
    val includeWorkspaceLayout: Boolean = true,
    val includeThemes: Boolean = true,
    val includeAssistantPreferences: Boolean = true,
    val includeUsageLearning: Boolean = false,
    val excludeSecretsAlways: Boolean = true,
    val excludeWidgetHostIdsAlways: Boolean = true,
)

data class SystemIntegrationSettings(
    val dynamicColorEnabled: Boolean = true,
    val workProfileIntegrationEnabled: Boolean = true,
    val notificationDotsEnabled: Boolean = false,
    val systemHomeEscapeVisible: Boolean = true,
    val followSystemFontScale: Boolean = true,
)

data class AdvancedSettings(
    val diagnosticsEnabled: Boolean = false,
    val showPerformanceOverlay: Boolean = false,
    val logUiTimingLocally: Boolean = false,
    val experimentalFeaturesEnabled: Boolean = false,
)

fun defaultGestureBindings(): List<GestureBinding> = listOf(
    GestureBinding(GestureTrigger.SWIPE_UP, GestureAction.OPEN_DRAWER),
    GestureBinding(GestureTrigger.SWIPE_DOWN, GestureAction.OPEN_SEARCH),
    GestureBinding(GestureTrigger.DOUBLE_TAP, GestureAction.OPEN_COMMAND_PALETTE),
    GestureBinding(GestureTrigger.LONG_PRESS, GestureAction.OPEN_HOME_STUDIO),
)
