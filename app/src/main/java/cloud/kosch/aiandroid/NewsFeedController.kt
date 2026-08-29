package cloud.kosch.aiandroid

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cloud.kosch.aiandroid.news.NewsCategory
import cloud.kosch.aiandroid.news.NewsFeedClient
import cloud.kosch.aiandroid.news.NewsItem
import cloud.kosch.aiandroid.news.NewsSource
import cloud.kosch.aiandroid.news.NewsSourceCatalog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant

/**
 * User-driven news surface. Opening/refreshing the page is the only network trigger; there is no background polling.
 * The global privacy network switch must already be on before any feed request is attempted.
 */
class NewsFeedController(
    context: Context,
    private val scope: CoroutineScope,
    private val client: NewsFeedClient = NewsFeedClient(),
) {
    private val preferences = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var visible by mutableStateOf(false)
        private set
    var loading by mutableStateOf(false)
        private set
    var items by mutableStateOf<List<NewsItem>>(emptyList())
        private set
    var enabledSourceIds by mutableStateOf(loadEnabledSourceIds())
        private set
    var selectedCategory by mutableStateOf<NewsCategory?>(null)
        private set
    var notice by mutableStateOf<String?>(null)
        private set
    var lastUpdatedAt by mutableStateOf<Instant?>(null)
        private set

    val sources: List<NewsSource>
        get() = NewsSourceCatalog.sources

    val visibleItems: List<NewsItem>
        get() = items.filter { item -> selectedCategory == null || item.category == selectedCategory }

    fun open(networkAllowed: Boolean) {
        visible = true
        if (items.isEmpty()) refresh(networkAllowed)
    }

    fun close() {
        visible = false
    }

    fun selectCategory(category: NewsCategory?) {
        selectedCategory = category
    }

    fun setSourceEnabled(sourceId: String, enabled: Boolean) {
        if (NewsSourceCatalog.byId(sourceId) == null) return
        enabledSourceIds = if (enabled) enabledSourceIds + sourceId else enabledSourceIds - sourceId
        preferences.edit().putStringSet(KEY_ENABLED_SOURCES, enabledSourceIds).apply()
        if (!enabled) items = items.filterNot { it.sourceId == sourceId }
        notice = "News-Quelle ${if (enabled) "aktiviert" else "deaktiviert"}. Aktualisieren lädt den neuen Stand."
    }

    fun refresh(networkAllowed: Boolean) {
        if (loading) return
        if (!networkAllowed) {
            notice = "Netzwerkfeatures sind aus. News lädt erst nach deiner Freigabe unter Datenschutz & Sicherheit."
            return
        }
        val enabledSources = sources.filter { it.id in enabledSourceIds }
        if (enabledSources.isEmpty()) {
            items = emptyList()
            notice = "Aktiviere mindestens eine News-Quelle."
            return
        }
        loading = true
        notice = null
        scope.launch {
            val results = withContext(Dispatchers.IO) {
                enabledSources.map { source -> source to client.fetch(source) }
            }
            val loaded = results.flatMap { (_, result) -> result.getOrDefault(emptyList()) }
                .distinctBy(NewsItem::url)
                .sortedWith(
                    compareByDescending<NewsItem> { it.publishedAt ?: Instant.EPOCH }
                        .thenBy(NewsItem::sourceTitle)
                        .thenBy(NewsItem::title),
                )
                .take(MAX_TOTAL_ITEMS)
            val failures = results.count { (_, result) -> result.isFailure }
            items = loaded
            lastUpdatedAt = Instant.now()
            loading = false
            notice = when {
                loaded.isEmpty() && failures > 0 -> "News konnten gerade nicht geladen werden. Prüfe Verbindung oder Quellen."
                failures > 0 -> "${loaded.size} Meldungen geladen · $failures Quelle${if (failures == 1) "" else "n"} nicht erreichbar."
                else -> "${loaded.size} Meldungen aktualisiert."
            }
        }
    }

    fun consumeNotice() {
        notice = null
    }

    private fun loadEnabledSourceIds(): Set<String> {
        val saved = preferences.getStringSet(KEY_ENABLED_SOURCES, null)
        return saved?.filterTo(mutableSetOf()) { NewsSourceCatalog.byId(it) != null }
            ?: sources.filter(NewsSource::enabledByDefault).mapTo(mutableSetOf(), NewsSource::id)
    }

    private companion object {
        const val PREFS_NAME = "kal_news_preferences"
        const val KEY_ENABLED_SOURCES = "enabled_source_ids_v1"
        const val MAX_TOTAL_ITEMS = 120
    }
}
