package cloud.kosch.aiandroid

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cloud.kosch.aiandroid.ai.UniversalQueryResult
import cloud.kosch.aiandroid.ai.UniversalSearchIndex
import cloud.kosch.aiandroid.ai.UniversalSearchQueryEngine
import cloud.kosch.aiandroid.ai.UniversalSearchSources

/**
 * Memory-only Universal Search session state.
 *
 * The controller never persists queries or result selections. Sources are rebuilt from current launcher snapshots on
 * every query so installed apps, pages, settings and portable actions cannot become stale authorization decisions.
 * Results remain typed targets; execution belongs to the existing launcher/AI/capability routes.
 */
class UniversalSearchController(
    private val sourceProvider: () -> UniversalSearchSources,
) {
    var visible by mutableStateOf(false)
        private set
    var query by mutableStateOf("")
        private set
    var results by mutableStateOf<List<UniversalQueryResult>>(emptyList())
        private set

    fun open(initialQuery: String = "") {
        visible = true
        updateQuery(initialQuery)
    }

    fun close() {
        visible = false
        query = ""
        results = emptyList()
    }

    fun updateQuery(value: String) {
        query = value.take(MAX_QUERY_CHARS)
        results = if (query.isBlank()) {
            emptyList()
        } else {
            val entries = UniversalSearchIndex.build(sourceProvider())
            UniversalSearchQueryEngine.query(query, entries, MAX_RESULTS)
        }
    }

    /** Re-evaluates the current query after a local catalog/profile/page change without retaining search history. */
    fun refresh() {
        if (!visible || query.isBlank()) return
        updateQuery(query)
    }

    private companion object {
        const val MAX_QUERY_CHARS = 240
        const val MAX_RESULTS = 32
    }
}
