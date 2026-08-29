package cloud.kosch.aiandroid.data

import cloud.kosch.aiandroid.model.AccessibilitySettings
import cloud.kosch.aiandroid.model.AdvancedSettings
import cloud.kosch.aiandroid.model.AiRoutingMode
import cloud.kosch.aiandroid.model.AiSettings
import cloud.kosch.aiandroid.model.AppSpaceSettings
import cloud.kosch.aiandroid.model.AssistantAnchor
import cloud.kosch.aiandroid.model.AssistantWakeMode
import cloud.kosch.aiandroid.model.AutomationSettings
import cloud.kosch.aiandroid.model.BackupSettings
import cloud.kosch.aiandroid.model.BadgeMode
import cloud.kosch.aiandroid.model.DockSettings
import cloud.kosch.aiandroid.model.FolderSettings
import cloud.kosch.aiandroid.model.GestureAction
import cloud.kosch.aiandroid.model.GestureBinding
import cloud.kosch.aiandroid.model.GestureSettings
import cloud.kosch.aiandroid.model.GestureTrigger
import cloud.kosch.aiandroid.model.HapticProfile
import cloud.kosch.aiandroid.model.HomeSettings
import cloud.kosch.aiandroid.model.LAUNCHER_SETTINGS_SCHEMA_VERSION
import cloud.kosch.aiandroid.model.LabelMode
import cloud.kosch.aiandroid.model.LauncherAssistantSettings
import cloud.kosch.aiandroid.model.LauncherSettingsDocument
import cloud.kosch.aiandroid.model.MAX_GESTURE_BINDINGS
import cloud.kosch.aiandroid.model.MAX_PROVIDER_SETTINGS
import cloud.kosch.aiandroid.model.MotionProfile
import cloud.kosch.aiandroid.model.NotificationSettings
import cloud.kosch.aiandroid.model.PageSettings
import cloud.kosch.aiandroid.model.PageTransition
import cloud.kosch.aiandroid.model.PenSettings
import cloud.kosch.aiandroid.model.PrivacySettings
import cloud.kosch.aiandroid.model.ProviderSettings
import cloud.kosch.aiandroid.model.ProviderTransport
import cloud.kosch.aiandroid.model.SearchHistoryMode
import cloud.kosch.aiandroid.model.SearchSettings
import cloud.kosch.aiandroid.model.SystemIntegrationSettings
import cloud.kosch.aiandroid.model.ThemeMode
import cloud.kosch.aiandroid.model.ThemeSettings
import cloud.kosch.aiandroid.model.VoiceSettings
import cloud.kosch.aiandroid.model.WallpaperMode
import cloud.kosch.aiandroid.model.WidgetSettings
import cloud.kosch.aiandroid.model.WidgetStackSwitchMode
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.TreeMap

/**
 * Deterministic, dependency-free portable codec for LauncherSettingsDocument.
 *
 * Values are UTF-8/Base64URL encoded and keys are sorted. Unknown keys are ignored on decode, enabling additive
 * forward evolution. Secret values are structurally absent from LauncherSettingsDocument and therefore cannot be
 * emitted by this codec; provider credentials are represented only by opaque credential aliases.
 */
object LauncherSettingsCodec {
    fun encode(document: LauncherSettingsDocument): String {
        val d = document.normalized()
        val w = WireWriter()
        w.int("schema", d.schemaVersion)

        with(d.home) {
            w.int("home.gridColumns", gridColumns)
            w.int("home.gridRows", gridRows)
            w.int("home.horizontalGapDp", horizontalGapDp)
            w.int("home.verticalGapDp", verticalGapDp)
            w.float("home.iconScale", iconScale)
            w.enum("home.labelMode", labelMode)
            w.bool("home.lockLayout", lockLayout)
            w.bool("home.autoFillEmptyCells", autoFillEmptyCells)
            w.bool("home.showPageIndicator", showPageIndicator)
        }
        with(d.pages) {
            w.bool("pages.loopingEnabled", loopingEnabled)
            w.enum("pages.transition", transition)
            w.int("pages.transitionDurationMs", transitionDurationMs)
            w.bool("pages.rememberLastPage", rememberLastPage)
            w.bool("pages.allowPerPageWallpaper", allowPerPageWallpaper)
            w.bool("pages.allowPerPageGridOverride", allowPerPageGridOverride)
        }
        with(d.apps) {
            w.bool("apps.showLabels", showLabels)
            w.bool("apps.showWorkProfileBadges", showWorkProfileBadges)
            w.bool("apps.hideSystemApps", hideSystemApps)
            w.int("apps.drawerColumnsPortrait", drawerColumnsPortrait)
            w.int("apps.drawerColumnsLandscape", drawerColumnsLandscape)
            w.bool("apps.smartRankingEnabled", smartRankingEnabled)
            w.bool("apps.alphabeticalIndexEnabled", alphabeticalIndexEnabled)
        }
        with(d.dock) {
            w.bool("dock.enabled", enabled)
            w.int("dock.maxItems", maxItems)
            w.float("dock.iconScale", iconScale)
            w.bool("dock.adaptiveSuggestions", adaptiveSuggestions)
            w.bool("dock.showAskButton", showAskButton)
            w.float("dock.backgroundOpacity", backgroundOpacity)
        }
        with(d.folders) {
            w.bool("folders.openAsSheet", openAsSheet)
            w.int("folders.columns", columns)
            w.bool("folders.showLabels", showLabels)
            w.bool("folders.smartFoldersEnabled", smartFoldersEnabled)
            w.bool("folders.closeAfterLaunch", closeAfterLaunch)
        }
        with(d.widgets) {
            w.bool("widgets.allowFreeResize", allowFreeResize)
            w.int("widgets.defaultColumnSpan", defaultColumnSpan)
            w.int("widgets.defaultRowSpan", defaultRowSpan)
            w.enum("widgets.stackSwitchMode", stackSwitchMode)
            w.int("widgets.stackAutoCycleSeconds", stackAutoCycleSeconds)
            w.enum("widgets.haptics", haptics)
            w.bool("widgets.showMissingProviderPlaceholder", showMissingProviderPlaceholder)
        }
        with(d.appearance) {
            w.enum("appearance.mode", mode)
            w.enum("appearance.motionProfile", motionProfile)
            w.enum("appearance.wallpaperMode", wallpaperMode)
            w.float("appearance.blurStrength", blurStrength)
            w.float("appearance.surfaceOpacity", surfaceOpacity)
            w.float("appearance.cornerScale", cornerScale)
            w.float("appearance.contentScale", contentScale)
            w.bool("appearance.useMaterialYouAccents", useMaterialYouAccents)
            w.nullable("appearance.iconPackPackage", iconPackPackage)
        }
        with(d.theme) {
            w.string("theme.activeThemeId", activeThemeId)
            w.bool("theme.allowThemeLayoutOverrides", allowThemeLayoutOverrides)
            w.bool("theme.allowThemeAssistantAssets", allowThemeAssistantAssets)
            w.bool("theme.allowThemeSounds", allowThemeSounds)
            w.bool("theme.includeWallpaperInThemeExport", includeWallpaperInThemeExport)
            w.bool("theme.includeLayoutInThemeExport", includeLayoutInThemeExport)
        }
        with(d.assistant) {
            w.bool("assistant.enabled", enabled)
            w.string("assistant.assistantId", assistantId)
            w.enum("assistant.anchor", anchor)
            w.float("assistant.scale", scale)
            w.float("assistant.opacity", opacity)
            w.enum("assistant.wakeMode", wakeMode)
            w.bool("assistant.idleMotionEnabled", idleMotionEnabled)
            w.bool("assistant.portalAnimationEnabled", portalAnimationEnabled)
            w.bool("assistant.gazeTrackingEnabled", gazeTrackingEnabled)
            w.bool("assistant.emotionAnimationEnabled", emotionAnimationEnabled)
            w.bool("assistant.visemeLipSyncEnabled", visemeLipSyncEnabled)
            w.bool("assistant.liveChatEnabled", liveChatEnabled)
            w.bool("assistant.voiceInputEnabled", voiceInputEnabled)
            w.bool("assistant.speechOutputEnabled", speechOutputEnabled)
            w.bool("assistant.hideOutsideAssistantPages", hideOutsideAssistantPages)
        }
        with(d.ai) {
            w.enum("ai.routingMode", routingMode)
            w.nullable("ai.defaultProviderId", defaultProviderId)
            w.bool("ai.networkProvidersEnabled", networkProvidersEnabled)
            w.bool("ai.localCommandPlannerEnabled", localCommandPlannerEnabled)
            w.bool("ai.localModelEnabled", localModelEnabled)
            w.int("ai.providers.count", providers.size)
            providers.forEachIndexed { index, provider ->
                val p = "ai.providers.$index"
                w.string("$p.providerId", provider.providerId)
                w.bool("$p.enabled", provider.enabled)
                w.enum("$p.transport", provider.transport)
                w.nullable("$p.endpoint", provider.endpoint)
                w.nullable("$p.modelId", provider.modelId)
                w.nullable("$p.credentialAlias", provider.credentialAlias)
                w.bool("$p.sendContextByDefault", provider.sendContextByDefault)
            }
        }
        with(d.voice) {
            w.bool("voice.inputEnabled", inputEnabled)
            w.bool("voice.outputEnabled", outputEnabled)
            w.float("voice.speechRate", speechRate)
            w.float("voice.pitch", pitch)
            w.nullable("voice.preferredLocaleTag", preferredLocaleTag)
            w.bool("voice.interruptSpeechOnUserInput", interruptSpeechOnUserInput)
        }
        with(d.gestures) {
            w.bool("gestures.enabled", enabled)
            w.enum("gestures.haptics", haptics)
            w.int("gestures.bindings.count", bindings.size)
            bindings.forEachIndexed { index, binding ->
                val p = "gestures.bindings.$index"
                w.enum("$p.trigger", binding.trigger)
                w.enum("$p.action", binding.action)
                w.nullable("$p.customTarget", binding.customTarget)
            }
        }
        with(d.search) {
            w.bool("search.fuzzySearchEnabled", fuzzySearchEnabled)
            w.bool("search.searchApps", searchApps)
            w.bool("search.searchShortcuts", searchShortcuts)
            w.bool("search.searchFilesOnlyWhenExplicitlyGranted", searchFilesOnlyWhenExplicitlyGranted)
            w.bool("search.showCommandSuggestions", showCommandSuggestions)
            w.enum("search.historyMode", historyMode)
        }
        with(d.notifications) {
            w.enum("notifications.badgeMode", badgeMode)
            w.bool("notifications.showBadgesOnDock", showBadgesOnDock)
            w.bool("notifications.showBadgesInFolders", showBadgesInFolders)
            w.bool("notifications.copyNotificationContent", copyNotificationContent)
        }
        with(d.pen) {
            w.bool("pen.penSpaceEnabled", penSpaceEnabled)
            w.bool("pen.hoverPreviewEnabled", hoverPreviewEnabled)
            w.bool("pen.pressureEnabled", pressureEnabled)
            w.bool("pen.tiltEnabled", tiltEnabled)
            w.bool("pen.ignoreFingerInPenSpace", ignoreFingerInPenSpace)
            w.enum("pen.primaryButtonAction", primaryButtonAction)
            w.enum("pen.secondaryButtonAction", secondaryButtonAction)
        }
        with(d.automation) {
            w.bool("automation.contextualSuggestionsEnabled", contextualSuggestionsEnabled)
            w.bool("automation.automaticLayoutChangesAllowed", automaticLayoutChangesAllowed)
            w.bool("automation.sceneSuggestionsEnabled", sceneSuggestionsEnabled)
            w.bool("automation.batteryAwareEffects", batteryAwareEffects)
            w.bool("automation.timeAwareSuggestions", timeAwareSuggestions)
        }
        with(d.accessibility) {
            w.bool("accessibility.reducedMotion", reducedMotion)
            w.bool("accessibility.highContrast", highContrast)
            w.bool("accessibility.largeTouchTargets", largeTouchTargets)
            w.bool("accessibility.announcePageChanges", announcePageChanges)
            w.bool("accessibility.preferTextAlongsideIcons", preferTextAlongsideIcons)
        }
        with(d.privacy) {
            w.bool("privacy.localUsageLearningEnabled", localUsageLearningEnabled)
            w.bool("privacy.notificationAccessEnabledByUser", notificationAccessEnabledByUser)
            w.bool("privacy.auditEnabled", auditEnabled)
            w.int("privacy.auditRetentionDays", auditRetentionDays)
            w.bool("privacy.allowNetworkFeatures", allowNetworkFeatures)
            w.bool("privacy.requireContextPreviewBeforeProviderHandoff", requireContextPreviewBeforeProviderHandoff)
        }
        with(d.backup) {
            w.bool("backup.includeLauncherSettings", includeLauncherSettings)
            w.bool("backup.includeWorkspaceLayout", includeWorkspaceLayout)
            w.bool("backup.includeThemes", includeThemes)
            w.bool("backup.includeAssistantPreferences", includeAssistantPreferences)
            w.bool("backup.includeUsageLearning", includeUsageLearning)
            w.bool("backup.excludeSecretsAlways", excludeSecretsAlways)
            w.bool("backup.excludeWidgetHostIdsAlways", excludeWidgetHostIdsAlways)
        }
        with(d.system) {
            w.bool("system.dynamicColorEnabled", dynamicColorEnabled)
            w.bool("system.workProfileIntegrationEnabled", workProfileIntegrationEnabled)
            w.bool("system.notificationDotsEnabled", notificationDotsEnabled)
            w.bool("system.systemHomeEscapeVisible", systemHomeEscapeVisible)
            w.bool("system.followSystemFontScale", followSystemFontScale)
        }
        with(d.advanced) {
            w.bool("advanced.diagnosticsEnabled", diagnosticsEnabled)
            w.bool("advanced.showPerformanceOverlay", showPerformanceOverlay)
            w.bool("advanced.logUiTimingLocally", logUiTimingLocally)
            w.bool("advanced.experimentalFeaturesEnabled", experimentalFeaturesEnabled)
        }
        return w.build()
    }

    fun decode(payload: String): LauncherSettingsDocument {
        val r = WireReader(payload)
        val schema = r.int("schema", LAUNCHER_SETTINGS_SCHEMA_VERSION)
        require(schema == LAUNCHER_SETTINGS_SCHEMA_VERSION) { "Unsupported launcher settings schema $schema" }

        val defaults = LauncherSettingsDocument()
        val home = defaults.home.let { d ->
            HomeSettings(
                gridColumns = r.int("home.gridColumns", d.gridColumns),
                gridRows = r.int("home.gridRows", d.gridRows),
                horizontalGapDp = r.int("home.horizontalGapDp", d.horizontalGapDp),
                verticalGapDp = r.int("home.verticalGapDp", d.verticalGapDp),
                iconScale = r.float("home.iconScale", d.iconScale),
                labelMode = r.enum("home.labelMode", d.labelMode),
                lockLayout = r.bool("home.lockLayout", d.lockLayout),
                autoFillEmptyCells = r.bool("home.autoFillEmptyCells", d.autoFillEmptyCells),
                showPageIndicator = r.bool("home.showPageIndicator", d.showPageIndicator),
            )
        }
        val pages = defaults.pages.let { d ->
            PageSettings(
                loopingEnabled = r.bool("pages.loopingEnabled", d.loopingEnabled),
                transition = r.enum("pages.transition", d.transition),
                transitionDurationMs = r.int("pages.transitionDurationMs", d.transitionDurationMs),
                rememberLastPage = r.bool("pages.rememberLastPage", d.rememberLastPage),
                allowPerPageWallpaper = r.bool("pages.allowPerPageWallpaper", d.allowPerPageWallpaper),
                allowPerPageGridOverride = r.bool("pages.allowPerPageGridOverride", d.allowPerPageGridOverride),
            )
        }
        val apps = defaults.apps.let { d ->
            AppSpaceSettings(
                showLabels = r.bool("apps.showLabels", d.showLabels),
                showWorkProfileBadges = r.bool("apps.showWorkProfileBadges", d.showWorkProfileBadges),
                hideSystemApps = r.bool("apps.hideSystemApps", d.hideSystemApps),
                drawerColumnsPortrait = r.int("apps.drawerColumnsPortrait", d.drawerColumnsPortrait),
                drawerColumnsLandscape = r.int("apps.drawerColumnsLandscape", d.drawerColumnsLandscape),
                smartRankingEnabled = r.bool("apps.smartRankingEnabled", d.smartRankingEnabled),
                alphabeticalIndexEnabled = r.bool("apps.alphabeticalIndexEnabled", d.alphabeticalIndexEnabled),
            )
        }
        val dock = defaults.dock.let { d ->
            DockSettings(
                enabled = r.bool("dock.enabled", d.enabled),
                maxItems = r.int("dock.maxItems", d.maxItems),
                iconScale = r.float("dock.iconScale", d.iconScale),
                adaptiveSuggestions = r.bool("dock.adaptiveSuggestions", d.adaptiveSuggestions),
                showAskButton = r.bool("dock.showAskButton", d.showAskButton),
                backgroundOpacity = r.float("dock.backgroundOpacity", d.backgroundOpacity),
            )
        }
        val folders = defaults.folders.let { d ->
            FolderSettings(
                openAsSheet = r.bool("folders.openAsSheet", d.openAsSheet),
                columns = r.int("folders.columns", d.columns),
                showLabels = r.bool("folders.showLabels", d.showLabels),
                smartFoldersEnabled = r.bool("folders.smartFoldersEnabled", d.smartFoldersEnabled),
                closeAfterLaunch = r.bool("folders.closeAfterLaunch", d.closeAfterLaunch),
            )
        }
        val widgets = defaults.widgets.let { d ->
            WidgetSettings(
                allowFreeResize = r.bool("widgets.allowFreeResize", d.allowFreeResize),
                defaultColumnSpan = r.int("widgets.defaultColumnSpan", d.defaultColumnSpan),
                defaultRowSpan = r.int("widgets.defaultRowSpan", d.defaultRowSpan),
                stackSwitchMode = r.enum("widgets.stackSwitchMode", d.stackSwitchMode),
                stackAutoCycleSeconds = r.int("widgets.stackAutoCycleSeconds", d.stackAutoCycleSeconds),
                haptics = r.enum("widgets.haptics", d.haptics),
                showMissingProviderPlaceholder = r.bool("widgets.showMissingProviderPlaceholder", d.showMissingProviderPlaceholder),
            )
        }
        val appearance = defaults.appearance.let { d ->
            cloud.kosch.aiandroid.model.AppearanceSettings(
                mode = r.enum("appearance.mode", d.mode),
                motionProfile = r.enum("appearance.motionProfile", d.motionProfile),
                wallpaperMode = r.enum("appearance.wallpaperMode", d.wallpaperMode),
                blurStrength = r.float("appearance.blurStrength", d.blurStrength),
                surfaceOpacity = r.float("appearance.surfaceOpacity", d.surfaceOpacity),
                cornerScale = r.float("appearance.cornerScale", d.cornerScale),
                contentScale = r.float("appearance.contentScale", d.contentScale),
                useMaterialYouAccents = r.bool("appearance.useMaterialYouAccents", d.useMaterialYouAccents),
                iconPackPackage = r.nullable("appearance.iconPackPackage"),
            )
        }
        val theme = defaults.theme.let { d ->
            ThemeSettings(
                activeThemeId = r.string("theme.activeThemeId", d.activeThemeId),
                allowThemeLayoutOverrides = r.bool("theme.allowThemeLayoutOverrides", d.allowThemeLayoutOverrides),
                allowThemeAssistantAssets = r.bool("theme.allowThemeAssistantAssets", d.allowThemeAssistantAssets),
                allowThemeSounds = r.bool("theme.allowThemeSounds", d.allowThemeSounds),
                includeWallpaperInThemeExport = r.bool("theme.includeWallpaperInThemeExport", d.includeWallpaperInThemeExport),
                includeLayoutInThemeExport = r.bool("theme.includeLayoutInThemeExport", d.includeLayoutInThemeExport),
            )
        }
        val assistant = defaults.assistant.let { d ->
            LauncherAssistantSettings(
                enabled = r.bool("assistant.enabled", d.enabled),
                assistantId = r.string("assistant.assistantId", d.assistantId),
                anchor = r.enum("assistant.anchor", d.anchor),
                scale = r.float("assistant.scale", d.scale),
                opacity = r.float("assistant.opacity", d.opacity),
                wakeMode = r.enum("assistant.wakeMode", d.wakeMode),
                idleMotionEnabled = r.bool("assistant.idleMotionEnabled", d.idleMotionEnabled),
                portalAnimationEnabled = r.bool("assistant.portalAnimationEnabled", d.portalAnimationEnabled),
                gazeTrackingEnabled = r.bool("assistant.gazeTrackingEnabled", d.gazeTrackingEnabled),
                emotionAnimationEnabled = r.bool("assistant.emotionAnimationEnabled", d.emotionAnimationEnabled),
                visemeLipSyncEnabled = r.bool("assistant.visemeLipSyncEnabled", d.visemeLipSyncEnabled),
                liveChatEnabled = r.bool("assistant.liveChatEnabled", d.liveChatEnabled),
                voiceInputEnabled = r.bool("assistant.voiceInputEnabled", d.voiceInputEnabled),
                speechOutputEnabled = r.bool("assistant.speechOutputEnabled", d.speechOutputEnabled),
                hideOutsideAssistantPages = r.bool("assistant.hideOutsideAssistantPages", d.hideOutsideAssistantPages),
            )
        }
        val providerCount = r.int("ai.providers.count", 0).coerceIn(0, MAX_PROVIDER_SETTINGS)
        val providers = (0 until providerCount).mapNotNull { index ->
            val p = "ai.providers.$index"
            val id = r.string("$p.providerId", "").trim()
            if (id.isBlank()) return@mapNotNull null
            ProviderSettings(
                providerId = id,
                enabled = r.bool("$p.enabled", false),
                transport = r.enum("$p.transport", ProviderTransport.APP_HANDOFF),
                endpoint = r.nullable("$p.endpoint"),
                modelId = r.nullable("$p.modelId"),
                credentialAlias = r.nullable("$p.credentialAlias"),
                sendContextByDefault = r.bool("$p.sendContextByDefault", false),
            )
        }
        val ai = defaults.ai.let { d ->
            AiSettings(
                routingMode = r.enum("ai.routingMode", d.routingMode),
                defaultProviderId = r.nullable("ai.defaultProviderId"),
                networkProvidersEnabled = r.bool("ai.networkProvidersEnabled", d.networkProvidersEnabled),
                localCommandPlannerEnabled = r.bool("ai.localCommandPlannerEnabled", d.localCommandPlannerEnabled),
                localModelEnabled = r.bool("ai.localModelEnabled", d.localModelEnabled),
                providers = providers,
            )
        }
        val voice = defaults.voice.let { d ->
            VoiceSettings(
                inputEnabled = r.bool("voice.inputEnabled", d.inputEnabled),
                outputEnabled = r.bool("voice.outputEnabled", d.outputEnabled),
                speechRate = r.float("voice.speechRate", d.speechRate),
                pitch = r.float("voice.pitch", d.pitch),
                preferredLocaleTag = r.nullable("voice.preferredLocaleTag"),
                interruptSpeechOnUserInput = r.bool("voice.interruptSpeechOnUserInput", d.interruptSpeechOnUserInput),
            )
        }
        val bindingCount = r.int("gestures.bindings.count", 0).coerceIn(0, MAX_GESTURE_BINDINGS)
        val bindings = (0 until bindingCount).mapNotNull { index ->
            val p = "gestures.bindings.$index"
            val trigger = r.enumOrNull<GestureTrigger>("$p.trigger") ?: return@mapNotNull null
            GestureBinding(
                trigger = trigger,
                action = r.enum("$p.action", GestureAction.NONE),
                customTarget = r.nullable("$p.customTarget"),
            )
        }
        val gestures = defaults.gestures.let { d ->
            GestureSettings(
                enabled = r.bool("gestures.enabled", d.enabled),
                haptics = r.enum("gestures.haptics", d.haptics),
                bindings = if (r.has("gestures.bindings.count")) bindings else d.bindings,
            )
        }
        val search = defaults.search.let { d ->
            SearchSettings(
                fuzzySearchEnabled = r.bool("search.fuzzySearchEnabled", d.fuzzySearchEnabled),
                searchApps = r.bool("search.searchApps", d.searchApps),
                searchShortcuts = r.bool("search.searchShortcuts", d.searchShortcuts),
                searchFilesOnlyWhenExplicitlyGranted = r.bool("search.searchFilesOnlyWhenExplicitlyGranted", d.searchFilesOnlyWhenExplicitlyGranted),
                showCommandSuggestions = r.bool("search.showCommandSuggestions", d.showCommandSuggestions),
                historyMode = r.enum("search.historyMode", d.historyMode),
            )
        }
        val notifications = defaults.notifications.let { d ->
            NotificationSettings(
                badgeMode = r.enum("notifications.badgeMode", d.badgeMode),
                showBadgesOnDock = r.bool("notifications.showBadgesOnDock", d.showBadgesOnDock),
                showBadgesInFolders = r.bool("notifications.showBadgesInFolders", d.showBadgesInFolders),
                copyNotificationContent = r.bool("notifications.copyNotificationContent", d.copyNotificationContent),
            )
        }
        val pen = defaults.pen.let { d ->
            PenSettings(
                penSpaceEnabled = r.bool("pen.penSpaceEnabled", d.penSpaceEnabled),
                hoverPreviewEnabled = r.bool("pen.hoverPreviewEnabled", d.hoverPreviewEnabled),
                pressureEnabled = r.bool("pen.pressureEnabled", d.pressureEnabled),
                tiltEnabled = r.bool("pen.tiltEnabled", d.tiltEnabled),
                ignoreFingerInPenSpace = r.bool("pen.ignoreFingerInPenSpace", d.ignoreFingerInPenSpace),
                primaryButtonAction = r.enum("pen.primaryButtonAction", d.primaryButtonAction),
                secondaryButtonAction = r.enum("pen.secondaryButtonAction", d.secondaryButtonAction),
            )
        }
        val automation = defaults.automation.let { d ->
            AutomationSettings(
                contextualSuggestionsEnabled = r.bool("automation.contextualSuggestionsEnabled", d.contextualSuggestionsEnabled),
                automaticLayoutChangesAllowed = r.bool("automation.automaticLayoutChangesAllowed", d.automaticLayoutChangesAllowed),
                sceneSuggestionsEnabled = r.bool("automation.sceneSuggestionsEnabled", d.sceneSuggestionsEnabled),
                batteryAwareEffects = r.bool("automation.batteryAwareEffects", d.batteryAwareEffects),
                timeAwareSuggestions = r.bool("automation.timeAwareSuggestions", d.timeAwareSuggestions),
            )
        }
        val accessibility = defaults.accessibility.let { d ->
            AccessibilitySettings(
                reducedMotion = r.bool("accessibility.reducedMotion", d.reducedMotion),
                highContrast = r.bool("accessibility.highContrast", d.highContrast),
                largeTouchTargets = r.bool("accessibility.largeTouchTargets", d.largeTouchTargets),
                announcePageChanges = r.bool("accessibility.announcePageChanges", d.announcePageChanges),
                preferTextAlongsideIcons = r.bool("accessibility.preferTextAlongsideIcons", d.preferTextAlongsideIcons),
            )
        }
        val privacy = defaults.privacy.let { d ->
            PrivacySettings(
                localUsageLearningEnabled = r.bool("privacy.localUsageLearningEnabled", d.localUsageLearningEnabled),
                notificationAccessEnabledByUser = r.bool("privacy.notificationAccessEnabledByUser", d.notificationAccessEnabledByUser),
                auditEnabled = r.bool("privacy.auditEnabled", d.auditEnabled),
                auditRetentionDays = r.int("privacy.auditRetentionDays", d.auditRetentionDays),
                allowNetworkFeatures = r.bool("privacy.allowNetworkFeatures", d.allowNetworkFeatures),
                requireContextPreviewBeforeProviderHandoff = r.bool("privacy.requireContextPreviewBeforeProviderHandoff", d.requireContextPreviewBeforeProviderHandoff),
            )
        }
        val backup = defaults.backup.let { d ->
            BackupSettings(
                includeLauncherSettings = r.bool("backup.includeLauncherSettings", d.includeLauncherSettings),
                includeWorkspaceLayout = r.bool("backup.includeWorkspaceLayout", d.includeWorkspaceLayout),
                includeThemes = r.bool("backup.includeThemes", d.includeThemes),
                includeAssistantPreferences = r.bool("backup.includeAssistantPreferences", d.includeAssistantPreferences),
                includeUsageLearning = r.bool("backup.includeUsageLearning", d.includeUsageLearning),
                excludeSecretsAlways = r.bool("backup.excludeSecretsAlways", true),
                excludeWidgetHostIdsAlways = r.bool("backup.excludeWidgetHostIdsAlways", true),
            )
        }.copy(excludeSecretsAlways = true, excludeWidgetHostIdsAlways = true)
        val system = defaults.system.let { d ->
            SystemIntegrationSettings(
                dynamicColorEnabled = r.bool("system.dynamicColorEnabled", d.dynamicColorEnabled),
                workProfileIntegrationEnabled = r.bool("system.workProfileIntegrationEnabled", d.workProfileIntegrationEnabled),
                notificationDotsEnabled = r.bool("system.notificationDotsEnabled", d.notificationDotsEnabled),
                systemHomeEscapeVisible = r.bool("system.systemHomeEscapeVisible", d.systemHomeEscapeVisible),
                followSystemFontScale = r.bool("system.followSystemFontScale", d.followSystemFontScale),
            )
        }
        val advanced = defaults.advanced.let { d ->
            AdvancedSettings(
                diagnosticsEnabled = r.bool("advanced.diagnosticsEnabled", d.diagnosticsEnabled),
                showPerformanceOverlay = r.bool("advanced.showPerformanceOverlay", d.showPerformanceOverlay),
                logUiTimingLocally = r.bool("advanced.logUiTimingLocally", d.logUiTimingLocally),
                experimentalFeaturesEnabled = r.bool("advanced.experimentalFeaturesEnabled", d.experimentalFeaturesEnabled),
            )
        }

        return LauncherSettingsDocument(
            schemaVersion = schema,
            home = home,
            pages = pages,
            apps = apps,
            dock = dock,
            folders = folders,
            widgets = widgets,
            appearance = appearance,
            theme = theme,
            assistant = assistant,
            ai = ai,
            voice = voice,
            gestures = gestures,
            search = search,
            notifications = notifications,
            pen = pen,
            automation = automation,
            accessibility = accessibility,
            privacy = privacy,
            backup = backup,
            system = system,
            advanced = advanced,
        ).normalized()
    }

    private class WireWriter {
        private val values = TreeMap<String, String>()

        fun string(key: String, value: String) { values[key] = value }
        fun nullable(key: String, value: String?) { if (value != null) values[key] = value }
        fun bool(key: String, value: Boolean) = string(key, value.toString())
        fun int(key: String, value: Int) = string(key, value.toString())
        fun float(key: String, value: Float) = string(key, value.toString())
        fun enum(key: String, value: Enum<*>) = string(key, value.name)

        fun build(): String = values.entries.joinToString("\n") { (key, value) ->
            "$key=${encodeValue(value)}"
        }
    }

    private class WireReader(payload: String) {
        private val values = payload.lineSequence()
            .mapNotNull { line ->
                val separator = line.indexOf('=')
                if (separator <= 0) return@mapNotNull null
                val key = line.substring(0, separator)
                val encoded = line.substring(separator + 1)
                runCatching { key to decodeValue(encoded) }.getOrNull()
            }
            .toMap()

        fun has(key: String): Boolean = key in values
        fun string(key: String, default: String): String = values[key] ?: default
        fun nullable(key: String): String? = values[key]
        fun bool(key: String, default: Boolean): Boolean = values[key]?.toBooleanStrictOrNull() ?: default
        fun int(key: String, default: Int): Int = values[key]?.toIntOrNull() ?: default
        fun float(key: String, default: Float): Float = values[key]?.toFloatOrNull() ?: default

        inline fun <reified E : Enum<E>> enum(key: String, default: E): E =
            enumOrNull<E>(key) ?: default

        inline fun <reified E : Enum<E>> enumOrNull(key: String): E? =
            values[key]?.let { raw -> enumValues<E>().firstOrNull { it.name == raw } }
    }

    private fun encodeValue(value: String): String = Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(value.toByteArray(StandardCharsets.UTF_8))

    private fun decodeValue(value: String): String = String(
        Base64.getUrlDecoder().decode(value),
        StandardCharsets.UTF_8,
    )
}
