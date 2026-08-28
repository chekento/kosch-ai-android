package cloud.kosch.aiandroid.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import cloud.kosch.aiandroid.model.GestureSettings
import cloud.kosch.aiandroid.model.GestureTrigger
import cloud.kosch.aiandroid.system.LauncherGestureClassifier
import cloud.kosch.aiandroid.system.LauncherGestureObservation
import cloud.kosch.aiandroid.system.LauncherGestureThresholds
import kotlin.math.hypot

/**
 * Parent-level gesture listener for launcher surfaces.
 *
 * The stream is inspected in the Final pointer pass. If a child consumed any event, the launcher gesture is dropped.
 * This is the important conflict rule: widgets, buttons, scrollables and Home Studio drag targets always win.
 */
fun Modifier.launcherGestureSurface(
    settings: GestureSettings,
    enabled: Boolean,
    onTrigger: (GestureTrigger) -> Unit,
): Modifier {
    if (!enabled || !settings.enabled) return this
    return pointerInput(settings) {
        val thresholds = gestureThresholds(this)
        var previousTapUptime = Long.MIN_VALUE
        var previousTapPosition = Offset.Unspecified

        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            val start = down.position
            val startTime = down.uptimeMillis
            var end = start
            var endTime = startTime
            var maxPointerCount = 1
            var initialSpan: Float? = null
            var finalSpan: Float? = null
            var consumed = down.isConsumed

            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Final)
                if (event.changes.any { it.isConsumed }) consumed = true
                maxPointerCount = maxOf(maxPointerCount, event.changes.count { it.pressed })

                val pressed = event.changes.filter { it.pressed }
                if (pressed.size >= 2) {
                    val span = spanBetween(pressed[0].position, pressed[1].position)
                    if (initialSpan == null) initialSpan = span
                    finalSpan = span
                }

                val primary = event.changes.firstOrNull { it.id == down.id } ?: event.changes.firstOrNull()
                if (primary != null) {
                    end = primary.position
                    endTime = primary.uptimeMillis
                }
                if (event.changes.none { it.pressed }) break
            }

            val duration = (endTime - startTime).coerceAtLeast(0L)
            val movement = spanBetween(start, end)
            val observation = LauncherGestureObservation(
                startX = start.x,
                startY = start.y,
                endX = end.x,
                endY = end.y,
                surfaceWidth = size.width.toFloat(),
                durationMillis = duration,
                maxPointerCount = maxPointerCount,
                initialSpanPx = initialSpan,
                finalSpanPx = finalSpan,
                consumedByChild = consumed,
            )
            val classified = LauncherGestureClassifier.classify(observation, thresholds)
            if (classified != null) {
                previousTapUptime = Long.MIN_VALUE
                previousTapPosition = Offset.Unspecified
                onTrigger(classified)
                return@awaitEachGesture
            }

            // Double tap is intentionally evaluated only for unconsumed, short, nearly stationary single-finger taps.
            val tap = !consumed && maxPointerCount == 1 && duration <= DOUBLE_TAP_SINGLE_TAP_MAX_MS &&
                movement <= thresholds.tapSlopPx
            if (!tap) {
                previousTapUptime = Long.MIN_VALUE
                previousTapPosition = Offset.Unspecified
                return@awaitEachGesture
            }

            val closeToPrevious = previousTapPosition != Offset.Unspecified &&
                spanBetween(previousTapPosition, end) <= DOUBLE_TAP_DISTANCE_DP.dp.toPx()
            if (
                previousTapUptime != Long.MIN_VALUE &&
                endTime - previousTapUptime in 1..DOUBLE_TAP_WINDOW_MS &&
                closeToPrevious
            ) {
                previousTapUptime = Long.MIN_VALUE
                previousTapPosition = Offset.Unspecified
                onTrigger(GestureTrigger.DOUBLE_TAP)
            } else {
                previousTapUptime = endTime
                previousTapPosition = end
            }
        }
    }
}

private fun gestureThresholds(density: Density): LauncherGestureThresholds = with(density) {
    LauncherGestureThresholds(
        tapSlopPx = 18.dp.toPx(),
        swipeDistancePx = 64.dp.toPx(),
        edgeWidthPx = 28.dp.toPx(),
    )
}

private fun spanBetween(first: Offset, second: Offset): Float =
    hypot((second.x - first.x).toDouble(), (second.y - first.y).toDouble()).toFloat()

private const val DOUBLE_TAP_WINDOW_MS = 330L
private const val DOUBLE_TAP_SINGLE_TAP_MAX_MS = 240L
private const val DOUBLE_TAP_DISTANCE_DP = 48
