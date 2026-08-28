package cloud.kosch.aiandroid.ai

import cloud.kosch.aiandroid.model.WidgetStack
import cloud.kosch.aiandroid.model.WidgetStackMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetStackPolicyTest {
    @Test
    fun `normalization removes invalid duplicate ids and bounds stack size`() {
        val normalized = WidgetStackPolicy.normalize(
            WidgetStack(
                id = "stack",
                title = "  My widgets  ",
                appWidgetIds = listOf(-1, 1, 1) + (2..30),
                activeIndex = 999,
            ),
        )!!

        assertEquals("My widgets", normalized.title)
        assertEquals(WidgetStackPolicy.MAX_WIDGETS_PER_STACK, normalized.appWidgetIds.size)
        assertEquals(normalized.appWidgetIds.distinct(), normalized.appWidgetIds)
        assertEquals(normalized.appWidgetIds.lastIndex, normalized.activeIndex)
    }

    @Test
    fun `auto cycle is bounded and disabled for manual modes`() {
        val auto = WidgetStackPolicy.normalize(
            WidgetStack(
                id = "auto",
                title = "Auto",
                appWidgetIds = listOf(1, 2),
                mode = WidgetStackMode.AUTO_CYCLE,
                autoCycleSeconds = 1,
            ),
        )!!
        val manual = WidgetStackPolicy.normalize(auto.copy(mode = WidgetStackMode.MANUAL))!!

        assertEquals(WidgetStackPolicy.MIN_AUTO_CYCLE_SECONDS, auto.autoCycleSeconds)
        assertEquals(0, manual.autoCycleSeconds)
    }

    @Test
    fun `repair removes orphan host ids and preserves active widget when possible`() {
        val repaired = WidgetStackPolicy.repair(
            listOf(
                WidgetStack(
                    id = "stack",
                    title = "Stack",
                    appWidgetIds = listOf(10, 20, 30),
                    activeIndex = 1,
                ),
            ),
            validWidgetIds = setOf(20, 30),
        ).single()

        assertEquals(listOf(20, 30), repaired.appWidgetIds)
        assertEquals(20, repaired.activeWidgetId)
    }

    @Test
    fun `manual navigation wraps in both directions`() {
        val stack = WidgetStack("s", "Stack", listOf(1, 2, 3), activeIndex = 0)

        assertEquals(2, WidgetStackPolicy.next(stack)!!.activeWidgetId)
        assertEquals(3, WidgetStackPolicy.previous(stack)!!.activeWidgetId)
    }

    @Test
    fun `removing final widget removes the stack`() {
        assertNull(
            WidgetStackPolicy.removeWidget(
                WidgetStack("s", "Stack", listOf(7)),
                7,
            ),
        )
    }

    @Test
    fun `stack collection is bounded`() {
        val stacks = (0 until 40).map {
            WidgetStack("stack-$it", "Stack $it", listOf(it + 1))
        }

        assertEquals(WidgetStackPolicy.MAX_STACKS, WidgetStackPolicy.normalizeAll(stacks).size)
        assertTrue(WidgetStackPolicy.normalizeAll(stacks).all { it.appWidgetIds.isNotEmpty() })
    }
}
