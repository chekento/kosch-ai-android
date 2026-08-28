package cloud.kosch.aiandroid.model

/**
 * Presentation-only bridge between [AdaptiveLauncherPlanner] and the Unified Home UI.
 *
 * This policy never mutates workspace content, pages, profiles or user preferences. It only converts a tested
 * capability/window plan into bounded visual budgets that a Compose surface may choose to render.
 */
data class AdaptiveHomePresentation(
    val horizontalPaddingDp: Int,
    val verticalPaddingDp: Int,
    val verticalGapDp: Int,
    val dockPinnedAppLimit: Int,
    val showEdgePowerRail: Boolean,
    val showPenShortcut: Boolean,
    val emphasizePenShortcut: Boolean,
)

object AdaptiveHomePresentationPolicy {
    fun from(
        plan: AdaptiveLauncherPlan,
        hasStylus: Boolean,
    ): AdaptiveHomePresentation {
        val horizontalPadding = when (plan.density) {
            AdaptiveUiDensity.COMFORTABLE -> 14
            AdaptiveUiDensity.BALANCED -> 12
            AdaptiveUiDensity.PRODUCTIVITY -> 10
        }
        val verticalPadding = when (plan.heightClass) {
            AdaptiveHeightClass.COMPACT -> 6
            AdaptiveHeightClass.MEDIUM -> 8
            AdaptiveHeightClass.EXPANDED -> 10
        }
        val verticalGap = when {
            plan.heightClass == AdaptiveHeightClass.COMPACT -> 6
            plan.density == AdaptiveUiDensity.PRODUCTIVITY -> 7
            plan.density == AdaptiveUiDensity.BALANCED -> 8
            else -> 10
        }

        return AdaptiveHomePresentation(
            horizontalPaddingDp = horizontalPadding,
            verticalPaddingDp = verticalPadding,
            verticalGapDp = verticalGap,
            dockPinnedAppLimit = plan.maxVisibleQuickActions.coerceIn(2, 10),
            showEdgePowerRail = plan.useEdgePowerRail,
            showPenShortcut = hasStylus,
            emphasizePenShortcut = hasStylus && plan.prioritizePenSpaceEntry,
        )
    }
}
