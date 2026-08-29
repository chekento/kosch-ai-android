package cloud.kosch.aiandroid.news

import java.time.Instant

enum class NewsCategory(val label: String) {
    AI("KI"),
    ANDROID("Android"),
    SECURITY("Security"),
    SCIENCE("Science"),
}

data class NewsSource(
    val id: String,
    val title: String,
    val category: NewsCategory,
    val feedUrl: String,
    val homeUrl: String,
    val enabledByDefault: Boolean,
)

data class NewsItem(
    val id: String,
    val sourceId: String,
    val sourceTitle: String,
    val category: NewsCategory,
    val title: String,
    val url: String,
    val publishedAt: Instant?,
)

/**
 * Curated feed endpoints only. KAL shows feed metadata and links to the original article; it does not republish article
 * bodies or images. Sources can be disabled individually and no feed is polled in the background.
 */
object NewsSourceCatalog {
    val sources: List<NewsSource> = listOf(
        NewsSource(
            id = "android-developers",
            title = "Android Developers",
            category = NewsCategory.ANDROID,
            feedUrl = "https://android-developers.googleblog.com/feeds/posts/default?alt=rss",
            homeUrl = "https://android-developers.googleblog.com/",
            enabledByDefault = true,
        ),
        NewsSource(
            id = "hugging-face-blog",
            title = "Hugging Face Blog",
            category = NewsCategory.AI,
            feedUrl = "https://huggingface.co/blog/feed.xml",
            homeUrl = "https://huggingface.co/blog",
            enabledByDefault = true,
        ),
        NewsSource(
            id = "arxiv-ai",
            title = "arXiv · Artificial Intelligence",
            category = NewsCategory.AI,
            feedUrl = "https://export.arxiv.org/rss/cs.AI",
            homeUrl = "https://arxiv.org/list/cs.AI/recent",
            enabledByDefault = true,
        ),
        NewsSource(
            id = "nasa-jpl",
            title = "NASA · JPL",
            category = NewsCategory.SCIENCE,
            feedUrl = "https://www.jpl.nasa.gov/feeds/news/",
            homeUrl = "https://www.jpl.nasa.gov/news/",
            enabledByDefault = false,
        ),
    )

    fun byId(id: String): NewsSource? = sources.firstOrNull { it.id == id }
}
