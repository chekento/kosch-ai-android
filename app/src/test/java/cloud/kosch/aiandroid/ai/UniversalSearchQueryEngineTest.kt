package cloud.kosch.aiandroid.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UniversalSearchQueryEngineTest {
    @Test
    fun `calculation appears before matching launcher entities`() {
        val entries = listOf(
            UniversalSearchEntry(
                id = "action:2",
                kind = UniversalSearchKind.CUSTOM_ACTION,
                title = "2 plus 2 notes",
                target = UniversalSearchTarget.CustomAction("2"),
            ),
        )

        val results = UniversalSearchQueryEngine.query("2 + 2", entries)

        assertTrue(results.first() is UniversalQueryResult.Utility)
        val utility = (results.first() as UniversalQueryResult.Utility).result as LocalSearchUtilityResult.Calculation
        assertEquals(4.0, utility.value, 0.000001)
    }

    @Test
    fun `ordinary text search contains no synthetic utility result`() {
        val entries = listOf(
            UniversalSearchEntry(
                id = "folder:work",
                kind = UniversalSearchKind.FOLDER,
                title = "Arbeit",
                target = UniversalSearchTarget.Folder("work"),
            ),
        )

        val results = UniversalSearchQueryEngine.query("arbeit", entries)

        assertEquals(1, results.size)
        assertTrue(results.single() is UniversalQueryResult.Entity)
    }

    @Test
    fun `result limit is bounded`() {
        val entries = (1..500).map { index ->
            UniversalSearchEntry(
                id = "app:$index",
                kind = UniversalSearchKind.APP,
                title = "Mail $index",
                target = UniversalSearchTarget.App(index.toString()),
            )
        }

        assertEquals(100, UniversalSearchQueryEngine.query("mail", entries, limit = Int.MAX_VALUE).size)
    }
}
