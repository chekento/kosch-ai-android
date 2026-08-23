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
    )

    @Test
    fun `exact title outranks package substring`() {
        val result = SearchRanker.rank("Kamera", documents)
        assertEquals("camera", result.first().id)
    }

    @Test
    fun `accents and punctuation do not block matching`() {
        val result = SearchRanker.rank("email", documents)
        assertEquals("mail", result.first().id)
    }

    @Test
    fun `keywords are searchable`() {
        val result = SearchRanker.rank("agenda", documents)
        assertEquals("calendar", result.first().id)
    }

    @Test
    fun `unrelated short query returns no guesses`() {
        assertTrue(SearchRanker.rank("zz", documents).isEmpty())
    }
}

