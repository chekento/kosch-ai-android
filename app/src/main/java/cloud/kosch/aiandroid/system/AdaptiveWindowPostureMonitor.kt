package cloud.kosch.aiandroid.system

import android.app.Activity
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import cloud.kosch.aiandroid.model.AdaptiveFoldFeatureSignal
import cloud.kosch.aiandroid.model.AdaptiveFoldOrientation
import cloud.kosch.aiandroid.model.AdaptiveFoldPosture
import cloud.kosch.aiandroid.model.AdaptiveFoldPosturePolicy
import cloud.kosch.aiandroid.model.AdaptiveFoldState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * Lifecycle-owner controlled WindowManager observer. The runtime exports fold geometry only; it does not retain device
 * model/manufacturer identifiers or infer a fold from display width.
 */
class AdaptiveWindowPostureMonitor(
    private val activity: Activity,
    private val scope: CoroutineScope,
    private val onChanged: (AdaptiveFoldPosture) -> Unit,
) {
    private var job: Job? = null

    fun start() {
        if (job?.isActive == true) return
        job = scope.launch {
            WindowInfoTracker.getOrCreate(activity)
                .windowLayoutInfo(activity)
                .collect { info ->
                    val features = info.displayFeatures
                        .filterIsInstance<FoldingFeature>()
                        .map { feature ->
                            AdaptiveFoldFeatureSignal(
                                separating = feature.isSeparating,
                                orientation = when (feature.orientation) {
                                    FoldingFeature.Orientation.VERTICAL -> AdaptiveFoldOrientation.VERTICAL
                                    FoldingFeature.Orientation.HORIZONTAL -> AdaptiveFoldOrientation.HORIZONTAL
                                    else -> AdaptiveFoldOrientation.NONE
                                },
                                state = when (feature.state) {
                                    FoldingFeature.State.FLAT -> AdaptiveFoldState.FLAT
                                    FoldingFeature.State.HALF_OPENED -> AdaptiveFoldState.HALF_OPENED
                                    else -> AdaptiveFoldState.NONE
                                },
                                occluding = feature.occlusionType == FoldingFeature.OcclusionType.FULL,
                            )
                        }
                    onChanged(AdaptiveFoldPosturePolicy.summarize(features))
                }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }
}
