package cloud.kosch.aiandroid.model

enum class AdaptiveFoldOrientation { NONE, VERTICAL, HORIZONTAL }
enum class AdaptiveFoldState { NONE, FLAT, HALF_OPENED }

data class AdaptiveFoldFeatureSignal(
    val separating: Boolean,
    val orientation: AdaptiveFoldOrientation,
    val state: AdaptiveFoldState,
    val occluding: Boolean,
)

data class AdaptiveFoldPosture(
    val present: Boolean = false,
    val separating: Boolean = false,
    val orientation: AdaptiveFoldOrientation = AdaptiveFoldOrientation.NONE,
    val state: AdaptiveFoldState = AdaptiveFoldState.NONE,
    val occluding: Boolean = false,
) {
    val isBookPosture: Boolean
        get() = present && state == AdaptiveFoldState.HALF_OPENED &&
            orientation == AdaptiveFoldOrientation.VERTICAL

    val isTabletopPosture: Boolean
        get() = present && state == AdaptiveFoldState.HALF_OPENED &&
            orientation == AdaptiveFoldOrientation.HORIZONTAL
}

/**
 * Reduces WindowManager folding features to the minimum geometry needed by launcher presentation.
 * No manufacturer/model identifiers enter adaptive policy.
 */
object AdaptiveFoldPosturePolicy {
    fun summarize(features: List<AdaptiveFoldFeatureSignal>): AdaptiveFoldPosture {
        if (features.isEmpty()) return AdaptiveFoldPosture()

        val preferred = features
            .sortedWith(
                compareByDescending<AdaptiveFoldFeatureSignal> { it.separating }
                    .thenByDescending { it.state == AdaptiveFoldState.HALF_OPENED }
                    .thenByDescending { it.occluding },
            )
            .first()

        return AdaptiveFoldPosture(
            present = true,
            separating = features.any(AdaptiveFoldFeatureSignal::separating),
            orientation = preferred.orientation,
            state = preferred.state,
            occluding = features.any(AdaptiveFoldFeatureSignal::occluding),
        )
    }
}
