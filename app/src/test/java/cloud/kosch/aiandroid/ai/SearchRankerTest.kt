package cloud.kosch.aiandroid.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchRankerTest {
    private val documents = listOf(
        SearchDocument("camera", "Kamera", listOf("com.android.camera")),
        SearchDocument("calendar", "Kalender", listOf("Termine", "Agenda")),
        SearchDocument("calculator", "Taschenrechner", listOf("Rechnen")),
        SearchDocument("mail", "E-Mail", listOf("Postfach")),
        SearchDocument("google-calendar", "Google Calendar", listOf("Meetings", "Agenda")),
    )

    @Test
    fun `exact title outranks package substring and explains why`() {
        val result = SearchRanker.rankDetailed("Kamera", documents)
        assertEquals("camera", result.first().document.id)
        assertEquals(SearchMatchReason.EXACT, result.first().reason)
    }

    @Test
    fun `accents and punctuation do not block matching`() {
        val result = SearchRanker.rank("email", documents)
        assertEquals("mail", result.first().id)
    }

    @Test
    fun `punctuation and spaces are equivalent in both directions`() {
        assertEquals("mail", SearchRanker.rank("E Mail", documents).first().id)
        assertEquals("mail", SearchRanker.rank("E-Mail", documents).first().id)
    }

    @Test
    fun `keywords are searchable`() {
        val result = SearchRanker.rank("agenda", documents)
        assertTrue(result.first().id in setOf("calendar", "google-calendar"))
    }

    @Test
    fun `multi token prefixes strongly match professional app names`() {
        val result = SearchRanker.rankDetailed("goo cal", documents)
        assertEquals("google-calendar", result.first().document.id)
        assertEquals(SearchMatchReason.TOKEN_PREFIX, result.first().reason)
    }

    @Test
    fun `acronym finds multi word app without cloud intelligence`() {
        val result = SearchRanker.rankDetailed("gc", documents)
        assertEquals("google-calendar", result.first().document.id)
        assertEquals(SearchMatchReason.ACRONYM, result.first().reason)
    }

    @Test
    fun `single edit typo recovery stays bounded`() {
        val result = SearchRanker.rankDetailed("kalendr", documents)
        assertEquals("calendar", result.first().document.id)
        assertEquals(SearchMatchReason.TYPO, result.first().reason)
    }

    @Test
    fun `unrelated short query returns no guesses`() {
        assertTrue(SearchRanker.rank("zz", documents).isEmpty())
    }

    @Test
    fun `smart collections understand german professional labels locally`() {
        assertTrue(
            LocalAppClassifier.belongsTo(
                SmartCollection.WORK,
                label = "Projekt Kalender",
                packageName = "cloud.kosch.planner",
                recentPackages = emptyList(),
                providerPackages = emptySet(),
            ),
        )
        assertTrue(
            LocalAppClassifier.belongsTo(
                SmartCollection.TOOLS,
                label = "Datei Werkzeug",
                packageName = "cloud.kosch.files",
                recentPackages = emptyList(),
                providerPackages = emptySet(),
            ),
        )
    }
}
