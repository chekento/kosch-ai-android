package cloud.kosch.aiandroid

import cloud.kosch.aiandroid.ai.UniversalQueryResult
import cloud.kosch.aiandroid.ai.UniversalSearchSources
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UniversalSearchControllerTest {
    @Test
    fun `close clears query results and session index`() {
        var providerCalls = 0
        val controller = UniversalSearchController {
            providerCalls += 1
            UniversalSearchSources(
                aiRoutes = listOf(
                    cloud.kosch.aiandroid.ai.UniversalAiRouteSource("local", "Lokale KI", keywords = listOf("privat")),
                ),
            )
        }

        controller.open("privat")
        assertTrue(controller.visible)
        assertTrue(controller.results.isNotEmpty())
        assertEquals(1, providerCalls)

        controller.close()
        assertFalse(controller.visible)
        assertEquals("", controller.query)
        assertTrue(controller.results.isEmpty())

        controller.open("privat")
        assertEquals(2, providerCalls)
    }

    @Test
    fun `typing reuses session index and explicit refresh rebuilds current local sources`() {
        var title = "Alpha"
        var providerCalls = 0
        val controller = UniversalSearchController {
            providerCalls += 1
            UniversalSearchSources(
                aiRoutes = listOf(cloud.kosch.aiandroid.ai.UniversalAiRouteSource("route", title)),
            )
        }

        controller.open("alpha")
        val first = (controller.results.single() as UniversalQueryResult.Entity).ranked.entry
        assertEquals("Alpha", first.title)
        assertEquals(1, providerCalls)

        title = "Beta"
        controller.updateQuery("beta")
        assertTrue(controller.results.isEmpty())
        assertEquals(1, providerCalls)

        controller.refresh()
        val second = (controller.results.single() as UniversalQueryResult.Entity).ranked.entry
        assertEquals("Beta", second.title)
        assertEquals(2, providerCalls)
    }

    @Test
    fun `utility result does not require an entity source`() {
        val controller = UniversalSearchController { UniversalSearchSources() }
        controller.open("2 + 3 * 4")

        assertTrue(controller.results.first() is UniversalQueryResult.Utility)
    }
}
