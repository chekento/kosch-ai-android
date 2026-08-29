package cloud.kosch.aiandroid

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cloud.kosch.aiandroid.ai.UniversalQueryResult
import cloud.kosch.aiandroid.ai.UniversalSearchEntry
import cloud.kosch.aiandroid.ai.UniversalSearchIndex
import cloud.kosch.aiandroid.ai.UniversalSearchQueryEngine
import cloud.kosch.aiandroid.ai.UniversalSearchSources

/**
 * Memory-only Universal Search session state.
 *
 * Queries and selections are never persisted. The bounded entity index is built once when the palette opens and on
 * explicit refresh, not on every keystroke. This keeps large app inventories responsive while execution still
 * re-resolves every typed target against current runtime state before acting.
 */
class UniversalSearchController(
    private val sourceProvider: () -> UniversalSearchSources,
) {
    private var sessionEntries: List<UniversalSearchEntry> = emptyList()

    var visible by mutableStateOf(false)
        private set
    var query by mutableStateOf("")
        private set
    var results by mutableStateOf<List<UniversalQueryResult>>(emptyList())
        private set

    fun open(initialQuery: String = "") {
        sessionEntries = UniversalSearchIndex.build(sourceProvider())
        visible = true
        updateQuery(initialQuery)
    }

    fun close() {
        visible = false
        query = ""
        results = emptyList()
        sessionEntries = emptyList()
    }

    fun updateQuery(value: String) {
        query = value.take(MAX_QUERY_CHARS)
        results = evaluate(query)
    }

    /** Rebuilds from current local snapshots and re-evaluates the current query without retaining any search history. */
    fun refresh() {
        if (!visible) return
        sessionEntries = UniversalSearchIndex.build(sourceProvider())
        results = evaluate(query)
    }

    private fun evaluate(value: String): List<UniversalQueryResult> = if (value.isBlank()) {
        emptyList()
    } else {
        UniversalSearchQueryEngine.query(value, sessionEntries, MAX_RESULTS)
    }

    private companion object {
        const val MAX_QUERY_CHARS = 240
        const val MAX_RESULTS = 32
    }
}
