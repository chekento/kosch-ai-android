package cloud.kosch.aiandroid.model

/** Android WindowSizeClass V2-compatible launcher breakpoints, based on available window space rather than model ids. */
enum class AdaptiveWidthClass { COMPACT, MEDIUM, EXPANDED, LARGE, EXTRA_LARGE }
enum class AdaptiveHeightClass { COMPACT, MEDIUM, EXPANDED }
enum class AdaptiveInputMode { TOUCH, HYBRID, PRECISE_POINTER }
enum class AdaptiveSurfaceMode { PHONE, LARGE_TOUCH, DESKTOP_WINDOW, EXTERNAL_DESKTOP }
enum class AdaptiveUiDensity { COMFORTABLE, BALANCED, PRODUCTIVITY }
enum class ExternalDisplayWorkspaceMode { INDEPENDENT, MIRROR_CURRENT, FOLLOW_PROFILE }

enum class AdaptiveInteractionProfile {
    THUMB_FIRST,
    LARGE_TOUCH,
    PEN_CANVAS,
    DESKTOP_PRO,
    FOLD_DUAL_SURFACE,
    PRESENTATION,
}

enum class AdaptiveWorkspaceProfileHint {
    PERSONAL,
    WORK,
    CREATIVE,
    PRESENTATION,
}

data class AdaptiveLauncherEnvironment(
    val widthDp: Int,
    val heightDp: Int,
    val isExternalDisplay: Boolean = false,
    val isDesktopWindowing: Boolean = false,
    val hasPrecisePointer: Boolean = false,
    val hasHardwareKeyboard: Boolean = false,
    val hasSeparatingFold: Boolean = false,
    val hasStylus: Boolean = false,
    val isPresentationDisplay: Boolean = false,
) {
    init {
        require(widthDp > 0 && heightDp > 0) { "Available launcher window must be positive" }
    }
}

data class AdaptiveLauncherPlan(
    val widthClass: AdaptiveWidthClass,
    val heightClass: AdaptiveHeightClass,
    val inputMode: AdaptiveInputMode,
    val surfaceMode: AdaptiveSurfaceMode,
    val density: AdaptiveUiDensity,
    val useTwoPaneSettings: Boolean,
    val usePersistentCommandRail: Boolean,
    val enableHoverAffordances: Boolean,
    val preferWindowedAssistantDock: Boolean,
    val externalWorkspaceMode: ExternalDisplayWorkspaceMode,
    val interactionProfile: AdaptiveInteractionProfile = AdaptiveInteractionProfile.THUMB_FIRST,
    val workspaceProfileHint: AdaptiveWorkspaceProfileHint = AdaptiveWorkspaceProfileHint.PERSONAL,
    val useEdgePowerRail: Boolean = false,
    val preferDenseAppGrid: Boolean = false,
    val maxVisibleQuickActions: Int = 4,
    val showKeyboardShortcutHints: Boolean = false,
    val prioritizePenSpaceEntry: Boolean = false,
)

/**
 * Pure decision core for phones, foldables, split-screen, desktop windows and connected displays.
 * The caller supplies live window/capability signals; device model allowlists are deliberately not part of the API.
 *
 * The returned profile is a presentation/recommendation plan only. It never mutates a workspace, changes a profile,
 * starts an app or grants a permission. The user-facing layer decides whether to preview or apply a suggestion.
 */
object AdaptiveLauncherPlanner {
    fun plan(
        environment: AdaptiveLauncherEnvironment,
        externalWorkspacePreference: ExternalDisplayWorkspaceMode = ExternalDisplayWorkspaceMode.INDEPENDENT,
    ): AdaptiveLauncherPlan {
        val width = widthClass(environment.widthDp)
        val height = heightClass(environment.heightDp)
        val input = when {
            environment.hasPrecisePointer -> AdaptiveInputMode.PRECISE_POINTER
            environment.hasHardwareKeyboard -> AdaptiveInputMode.HYBRID
            else -> AdaptiveInputMode.TOUCH
        }
        val surface = when {
            environment.isExternalDisplay -> AdaptiveSurfaceMode.EXTERNAL_DESKTOP
            environment.isDesktopWindowing -> AdaptiveSurfaceMode.DESKTOP_WINDOW
            width >= AdaptiveWidthClass.MEDIUM -> AdaptiveSurfaceMode.LARGE_TOUCH
            else -> AdaptiveSurfaceMode.PHONE
        }
        val density = when {
            input == AdaptiveInputMode.PRECISE_POINTER && width >= AdaptiveWidthClass.EXPANDED -> AdaptiveUiDensity.PRODUCTIVITY
            width >= AdaptiveWidthClass.MEDIUM -> AdaptiveUiDensity.BALANCED
            else -> AdaptiveUiDensity.COMFORTABLE
        }
        val enoughHeightForTwoPane = height != AdaptiveHeightClass.COMPACT
        val twoPane = width >= AdaptiveWidthClass.EXPANDED && enoughHeightForTwoPane && !environment.hasSeparatingFold
        val commandRail = width >= AdaptiveWidthClass.LARGE ||
            (width >= AdaptiveWidthClass.EXPANDED && input == AdaptiveInputMode.PRECISE_POINTER)

        val interactionProfile = when {
            environment.isPresentationDisplay -> AdaptiveInteractionProfile.PRESENTATION
            environment.hasSeparatingFold -> AdaptiveInteractionProfile.FOLD_DUAL_SURFACE
            environment.hasStylus && width >= AdaptiveWidthClass.MEDIUM -> AdaptiveInteractionProfile.PEN_CANVAS
            input == AdaptiveInputMode.PRECISE_POINTER && width >= AdaptiveWidthClass.EXPANDED -> AdaptiveInteractionProfile.DESKTOP_PRO
            width >= AdaptiveWidthClass.MEDIUM -> AdaptiveInteractionProfile.LARGE_TOUCH
            else -> AdaptiveInteractionProfile.THUMB_FIRST
        }
        val workspaceHint = when (interactionProfile) {
            AdaptiveInteractionProfile.PRESENTATION -> AdaptiveWorkspaceProfileHint.PRESENTATION
            AdaptiveInteractionProfile.PEN_CANVAS -> AdaptiveWorkspaceProfileHint.CREATIVE
            AdaptiveInteractionProfile.DESKTOP_PRO -> AdaptiveWorkspaceProfileHint.WORK
            AdaptiveInteractionProfile.FOLD_DUAL_SURFACE,
            AdaptiveInteractionProfile.LARGE_TOUCH,
            AdaptiveInteractionProfile.THUMB_FIRST -> AdaptiveWorkspaceProfileHint.PERSONAL
        }
        val quickActions = when (width) {
            AdaptiveWidthClass.COMPACT -> 4
            AdaptiveWidthClass.MEDIUM -> 6
            AdaptiveWidthClass.EXPANDED -> 8
            AdaptiveWidthClass.LARGE, AdaptiveWidthClass.EXTRA_LARGE -> 10
        }

        return AdaptiveLauncherPlan(
            widthClass = width,
            heightClass = height,
            inputMode = input,
            surfaceMode = surface,
            density = density,
            useTwoPaneSettings = twoPane,
            usePersistentCommandRail = commandRail,
            enableHoverAffordances = input == AdaptiveInputMode.PRECISE_POINTER,
            preferWindowedAssistantDock = surface == AdaptiveSurfaceMode.DESKTOP_WINDOW ||
                surface == AdaptiveSurfaceMode.EXTERNAL_DESKTOP,
            externalWorkspaceMode = if (environment.isExternalDisplay) {
                externalWorkspacePreference
            } else {
                ExternalDisplayWorkspaceMode.MIRROR_CURRENT
            },
            interactionProfile = interactionProfile,
            workspaceProfileHint = workspaceHint,
            useEdgePowerRail = width >= AdaptiveWidthClass.EXPANDED && height != AdaptiveHeightClass.COMPACT,
            preferDenseAppGrid = density == AdaptiveUiDensity.PRODUCTIVITY,
            maxVisibleQuickActions = quickActions,
            showKeyboardShortcutHints = environment.hasHardwareKeyboard,
            prioritizePenSpaceEntry = interactionProfile == AdaptiveInteractionProfile.PEN_CANVAS,
        )
    }

    fun widthClass(widthDp: Int): AdaptiveWidthClass = when {
        widthDp < 600 -> AdaptiveWidthClass.COMPACT
        widthDp < 840 -> AdaptiveWidthClass.MEDIUM
        widthDp < 1_200 -> AdaptiveWidthClass.EXPANDED
        widthDp < 1_600 -> AdaptiveWidthClass.LARGE
        else -> AdaptiveWidthClass.EXTRA_LARGE
    }

    fun heightClass(heightDp: Int): AdaptiveHeightClass = when {
        heightDp < 480 -> AdaptiveHeightClass.COMPACT
        heightDp < 900 -> AdaptiveHeightClass.MEDIUM
        else -> AdaptiveHeightClass.EXPANDED
    }
}
