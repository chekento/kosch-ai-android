package cloud.kosch.aiandroid.ai

sealed interface UniversalQueryResult {
    val score: Int

    data class Utility(
        val result: LocalSearchUtilityResult,
        override val score: Int = UTILITY_SCORE,
    ) : UniversalQueryResult

    data class Entity(
        val ranked: RankedUniversalSearchEntry,
        override val score: Int = ranked.score,
    ) : UniversalQueryResult

    companion object {
        private const val UTILITY_SCORE = 1_350
    }
}

/**
 * Query-time layer for the command/search palette. The static bounded entity index stays reusable while calculations
 * and conversions are evaluated only for the current query and are never persisted as search history by this core.
 */
object UniversalSearchQueryEngine {
    fun query(
        query: String,
        entries: List<UniversalSearchEntry>,
        limit: Int = 24,
    ): List<UniversalQueryResult> {
        val safeLimit = limit.coerceIn(1, MAX_RESULTS)
        val utility = LocalSearchUtilityEngine.evaluate(query)?.let(UniversalQueryResult::Utility)
        val entities = UniversalSearchIndex.rank(query, entries).map(UniversalQueryResult::Entity)
        return buildList {
            utility?.let(::add)
            addAll(entities)
        }
            .sortedWith(
                compareByDescending<UniversalQueryResult> { it.score }
                    .thenBy { result -> if (result is UniversalQueryResult.Utility) 0 else 1 },
            )
            .take(safeLimit)
    }

    private const val MAX_RESULTS = 100
}
