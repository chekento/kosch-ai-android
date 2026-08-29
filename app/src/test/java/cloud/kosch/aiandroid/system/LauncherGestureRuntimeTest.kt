package cloud.kosch.aiandroid.system

import cloud.kosch.aiandroid.model.GestureAction
import cloud.kosch.aiandroid.model.GestureBinding
import cloud.kosch.aiandroid.model.GestureSettings
import cloud.kosch.aiandroid.model.GestureTrigger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LauncherGestureRuntimeTest {
    private val thresholds = LauncherGestureThresholds(
        tapSlopPx = 18f,
        swipeDistancePx = 64f,
        edgeWidthPx = 28f,
    )

    @Test
    fun childConsumptionAlwaysWins() {
        val trigger = LauncherGestureClassifier.classify(
            observation(dx = 0f, dy = -160f, consumed = true),
            thresholds,
        )
        assertNull(trigger)
    }

    @Test
    fun cardinalSwipesNeedClearDominance() {
        assertEquals(
            GestureTrigger.SWIPE_UP,
            LauncherGestureClassifier.classify(observation(dx = 8f, dy = -120f), thresholds),
        )
        assertEquals(
            GestureTrigger.SWIPE_RIGHT,
            LauncherGestureClassifier.classify(observation(dx = 130f, dy = 12f), thresholds),
        )
        assertNull(LauncherGestureClassifier.classify(observation(dx = 72f, dy = 68f), thresholds))
    }

    @Test
    fun edgeGesturesAreDistinguishedFromOrdinaryHorizontalSwipes() {
        assertEquals(
            GestureTrigger.EDGE_LEFT,
            LauncherGestureClassifier.classify(
                observation(startX = 10f, dx = 120f, dy = 4f),
                thresholds,
            ),
        )
        assertEquals(
            GestureTrigger.EDGE_RIGHT,
            LauncherGestureClassifier.classify(
                observation(startX = 990f, dx = -120f, dy = 4f),
                thresholds,
            ),
        )
    }

    @Test
    fun longPressRequiresStationarySinglePointer() {
        assertEquals(
            GestureTrigger.LONG_PRESS,
            LauncherGestureClassifier.classify(
                observation(dx = 3f, dy = 4f, duration = 700L),
                thresholds,
            ),
        )
        assertNull(
            LauncherGestureClassifier.classify(
                observation(dx = 30f, dy = 0f, duration = 700L),
                thresholds,
            ),
        )
    }

    @Test
    fun pinchAndTwoFingerTapStaySeparate() {
        assertEquals(
            GestureTrigger.PINCH_IN,
            LauncherGestureClassifier.classify(
                observation(pointerCount = 2, initialSpan = 180f, finalSpan = 100f, duration = 300L),
                thresholds,
            ),
        )
        assertEquals(
            GestureTrigger.PINCH_OUT,
            LauncherGestureClassifier.classify(
                observation(pointerCount = 2, initialSpan = 100f, finalSpan = 160f, duration = 300L),
                thresholds,
            ),
        )
        assertEquals(
            GestureTrigger.TWO_FINGER_TAP,
            LauncherGestureClassifier.classify(
                observation(pointerCount = 2, dx = 2f, dy = 2f, duration = 180L),
                thresholds,
            ),
        )
    }

    @Test
    fun bindingsNeverInventAnAction() {
        val settings = GestureSettings(
            bindings = listOf(GestureBinding(GestureTrigger.SWIPE_UP, GestureAction.OPEN_DRAWER)),
        )
        assertEquals(GestureAction.OPEN_DRAWER, LauncherGestureBindingResolver.actionFor(settings, GestureTrigger.SWIPE_UP))
        assertEquals(GestureAction.NONE, LauncherGestureBindingResolver.actionFor(settings, GestureTrigger.SWIPE_DOWN))
        assertEquals(
            GestureAction.NONE,
            LauncherGestureBindingResolver.actionFor(settings.copy(enabled = false), GestureTrigger.SWIPE_UP),
        )
    }

    @Test
    fun customTargetIsOnlyExposedForCustomShortcutBinding() {
        val settings = GestureSettings(
            bindings = listOf(
                GestureBinding(GestureTrigger.EDGE_LEFT, GestureAction.CUSTOM_SHORTCUT, "  action:work  "),
                GestureBinding(GestureTrigger.EDGE_RIGHT, GestureAction.OPEN_DRAWER, "ignored"),
            ),
        )
        assertEquals("action:work", LauncherGestureBindingResolver.customTargetFor(settings, GestureTrigger.EDGE_LEFT))
        assertNull(LauncherGestureBindingResolver.customTargetFor(settings, GestureTrigger.EDGE_RIGHT))
    }

    private fun observation(
        startX: Float = 500f,
        dx: Float = 0f,
        dy: Float = 0f,
        duration: Long = 250L,
        pointerCount: Int = 1,
        initialSpan: Float? = null,
        finalSpan: Float? = null,
        consumed: Boolean = false,
    ) = LauncherGestureObservation(
        startX = startX,
        startY = 500f,
        endX = startX + dx,
        endY = 500f + dy,
        surfaceWidth = 1_000f,
        durationMillis = duration,
        maxPointerCount = pointerCount,
        initialSpanPx = initialSpan,
        finalSpanPx = finalSpan,
        consumedByChild = consumed,
    )
}
