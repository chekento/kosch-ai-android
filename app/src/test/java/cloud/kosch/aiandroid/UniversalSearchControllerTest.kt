package cloud.kosch.aiandroid

import cloud.kosch.aiandroid.ai.UniversalQueryResult
import cloud.kosch.aiandroid.ai.UniversalSearchEntry
import cloud.kosch.aiandroid.ai.UniversalSearchKind
import cloud.kosch.aiandroid.ai.UniversalSearchSources
import cloud.kosch.aiandroid.ai.UniversalSearchTarget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UniversalSearchControllerTest {
    @Test
    fun `close clears query and results`() {
        val controller = UniversalSearchController {
            UniversalSearchSources(
                aiRoutes = listOf(
                    cloud.kosch.aiandroid.ai.UniversalAiRouteSource("local", "Lokale KI", keywords = listOf("privat")),
                ),
            )
        }

        controller.open("privat")
        assertTrue(controller.visible)
        assertTrue(controller.results.isNotEmpty())

        controller.close()
        assertFalse(controller.visible)
        assertEquals("", controller.query)
        assertTrue(controller.results.isEmpty())
    }

    @Test
    fun `refresh rebuilds from current local sources`() {
        var title = "Alpha"
        val controller = UniversalSearchController {
            UniversalSearchSources(
                aiRoutes = listOf(cloud.kosch.aiandroid.ai.UniversalAiRouteSource("route", title)),
            )
        }

        controller.open("alpha")
        val first = (controller.results.single() as UniversalQueryResult.Entity).ranked.entry
        assertEquals("Alpha", first.title)

        title = "Beta"
        controller.updateQuery("beta")
        val second = (controller.results.single() as UniversalQueryResult.Entity).ranked.entry
        assertEquals("Beta", second.title)
    }

    @Test
    fun `utility result does not require an entity source`() {
        val controller = UniversalSearchController { UniversalSearchSources() }
        controller.open("2 + 3 * 4")

        assertTrue(controller.results.first() is UniversalQueryResult.Utility)
    }
}
