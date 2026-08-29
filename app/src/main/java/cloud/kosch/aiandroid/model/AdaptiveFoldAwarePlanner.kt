package cloud.kosch.aiandroid.model

enum class AdaptiveFoldLayoutMode {
    NONE,
    BOOK_DUAL_PANE,
    TABLETOP_SPLIT,
    SEPARATING_VERTICAL,
    SEPARATING_HORIZONTAL,
    GENERIC_SEPARATING,
}

data class AdaptiveFoldAwarePlan(
    val base: AdaptiveLauncherPlan,
    val foldLayoutMode: AdaptiveFoldLayoutMode,
    val avoidFoldOcclusion: Boolean,
    val suppressGenericTwoPaneAcrossHinge: Boolean,
)

/**
 * Fold-specific layer on top of the size/input planner. Keeping posture separate lets legacy callers migrate without
 * inventing a fold from window width while new WindowManager callers can provide real FoldingFeature geometry.
 */
object AdaptiveFoldAwarePlanner {
    fun plan(
        environment: AdaptiveLauncherEnvironment,
        posture: AdaptiveFoldPosture,
        externalWorkspacePreference: ExternalDisplayWorkspaceMode = ExternalDisplayWorkspaceMode.INDEPENDENT,
    ): AdaptiveFoldAwarePlan {
        val base = AdaptiveLauncherPlanner.plan(
            environment = environment.copy(
                hasSeparatingFold = environment.hasSeparatingFold || posture.separating,
            ),
            externalWorkspacePreference = externalWorkspacePreference,
        )
        val mode = when {
            posture.isBookPosture -> AdaptiveFoldLayoutMode.BOOK_DUAL_PANE
            posture.isTabletopPosture -> AdaptiveFoldLayoutMode.TABLETOP_SPLIT
            posture.separating && posture.orientation == AdaptiveFoldOrientation.VERTICAL ->
                AdaptiveFoldLayoutMode.SEPARATING_VERTICAL
            posture.separating && posture.orientation == AdaptiveFoldOrientation.HORIZONTAL ->
                AdaptiveFoldLayoutMode.SEPARATING_HORIZONTAL
            environment.hasSeparatingFold || posture.separating -> AdaptiveFoldLayoutMode.GENERIC_SEPARATING
            else -> AdaptiveFoldLayoutMode.NONE
        }
        return AdaptiveFoldAwarePlan(
            base = base,
            foldLayoutMode = mode,
            avoidFoldOcclusion = posture.occluding || posture.separating,
            suppressGenericTwoPaneAcrossHinge = posture.separating || environment.hasSeparatingFold,
        )
    }
}
