package cloud.kosch.aiandroid.system

import cloud.kosch.aiandroid.model.GestureAction
import cloud.kosch.aiandroid.model.GestureSettings
import cloud.kosch.aiandroid.model.GestureTrigger
import kotlin.math.abs
import kotlin.math.hypot

/**
 * Geometry-only launcher gesture classifier.
 *
 * Android/Compose owns pointer dispatch and decides whether a child consumed the stream. This core only receives an
 * unconsumed observation and deliberately uses conservative thresholds so app taps, widget scrolls and Home Studio
 * drags win over launcher-level gestures.
 */
data class LauncherGestureObservation(
    val startX: Float,
    val startY: Float,
    val endX: Float,
    val endY: Float,
    val surfaceWidth: Float,
    val durationMillis: Long,
    val maxPointerCount: Int = 1,
    val initialSpanPx: Float? = null,
    val finalSpanPx: Float? = null,
    val consumedByChild: Boolean = false,
)

data class LauncherGestureThresholds(
    val tapSlopPx: Float,
    val swipeDistancePx: Float,
    val edgeWidthPx: Float,
    val longPressMillis: Long = 550L,
    val maxSwipeMillis: Long = 850L,
    val maxTwoFingerTapMillis: Long = 320L,
    val directionDominance: Float = 1.25f,
    val pinchInRatio: Float = 0.74f,
    val pinchOutRatio: Float = 1.35f,
)

object LauncherGestureClassifier {
    fun classify(
        observation: LauncherGestureObservation,
        thresholds: LauncherGestureThresholds,
    ): GestureTrigger? {
        if (observation.consumedByChild) return null
        if (observation.surfaceWidth <= 0f || observation.durationMillis < 0L) return null

        val dx = observation.endX - observation.startX
        val dy = observation.endY - observation.startY
        val distance = hypot(dx.toDouble(), dy.toDouble()).toFloat()
        val absX = abs(dx)
        val absY = abs(dy)

        if (observation.maxPointerCount >= 2) {
            val initialSpan = observation.initialSpanPx
            val finalSpan = observation.finalSpanPx
            if (initialSpan != null && finalSpan != null && initialSpan > thresholds.tapSlopPx) {
                val ratio = finalSpan / initialSpan
                if (ratio <= thresholds.pinchInRatio) return GestureTrigger.PINCH_IN
                if (ratio >= thresholds.pinchOutRatio) return GestureTrigger.PINCH_OUT
            }
            if (
                observation.maxPointerCount == 2 &&
                observation.durationMillis <= thresholds.maxTwoFingerTapMillis &&
                distance <= thresholds.tapSlopPx
            ) {
                return GestureTrigger.TWO_FINGER_TAP
            }
            return null
        }

        if (distance <= thresholds.tapSlopPx && observation.durationMillis >= thresholds.longPressMillis) {
            return GestureTrigger.LONG_PRESS
        }
        if (observation.durationMillis > thresholds.maxSwipeMillis) return null

        val horizontal = absX >= thresholds.swipeDistancePx && absX >= absY * thresholds.directionDominance
        val vertical = absY >= thresholds.swipeDistancePx && absY >= absX * thresholds.directionDominance

        if (horizontal) {
            if (observation.startX <= thresholds.edgeWidthPx && dx > 0f) return GestureTrigger.EDGE_LEFT
            if (
                observation.startX >= observation.surfaceWidth - thresholds.edgeWidthPx &&
                dx < 0f
            ) return GestureTrigger.EDGE_RIGHT
            return if (dx < 0f) GestureTrigger.SWIPE_LEFT else GestureTrigger.SWIPE_RIGHT
        }
        if (vertical) return if (dy < 0f) GestureTrigger.SWIPE_UP else GestureTrigger.SWIPE_DOWN
        return null
    }
}

/** A binding lookup never invents a fallback action for an unbound trigger. */
object LauncherGestureBindingResolver {
    fun actionFor(settings: GestureSettings, trigger: GestureTrigger): GestureAction {
        if (!settings.enabled) return GestureAction.NONE
        return settings.normalized().bindings
            .firstOrNull { it.trigger == trigger }
            ?.action
            ?: GestureAction.NONE
    }

    fun customTargetFor(settings: GestureSettings, trigger: GestureTrigger): String? {
        if (!settings.enabled) return null
        return settings.normalized().bindings
            .firstOrNull { it.trigger == trigger }
            ?.takeIf { it.action == GestureAction.CUSTOM_SHORTCUT }
            ?.customTarget
            ?.trim()
            ?.take(512)
            ?.ifBlank { null }
    }
}
