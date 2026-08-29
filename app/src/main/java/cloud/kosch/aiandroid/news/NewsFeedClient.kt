package cloud.kosch.aiandroid.news

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * Small metadata-only RSS/Atom client. It never downloads article bodies or images and enforces HTTPS, byte, item and
 * timeout budgets. The caller controls when network work is allowed and when it is triggered.
 */
class NewsFeedClient {
    fun fetch(source: NewsSource): Result<List<NewsItem>> = runCatching {
        requireHttps(source.feedUrl)
        val connection = URL(source.feedUrl).openConnection() as HttpURLConnection
        try {
            connection.instanceFollowRedirects = true
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = READ_TIMEOUT_MS
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/rss+xml, application/atom+xml, application/xml, text/xml")
            connection.setRequestProperty("User-Agent", "KAL-News/0.2")
            val status = connection.responseCode
            check(status in 200..299) { "${source.title}: HTTP $status" }
            val payload = connection.inputStream.use(::readBounded)
            try {
                parse(source, payload)
            } finally {
                payload.fill(0)
            }
        } finally {
            connection.disconnect()
        }
    }

    internal fun parse(source: NewsSource, payload: ByteArray): List<NewsItem> {
        val parser = XmlPullParserFactory.newInstance().apply {
            isNamespaceAware = true
        }.newPullParser()
        runCatching { parser.setFeature(XmlPullParser.FEATURE_PROCESS_DOCDECL, false) }
        parser.setInput(ByteArrayInputStream(payload), null)

        val items = mutableListOf<NewsItem>()
        var insideItem = false
        var title: String? = null
        var link: String? = null
        var guid: String? = null
        var published: Instant? = null

        while (parser.eventType != XmlPullParser.END_DOCUMENT && items.size < MAX_ITEMS_PER_SOURCE) {
            when (parser.eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name.lowercase()) {
                        "item", "entry" -> {
                            insideItem = true
                            title = null
                            link = null
                            guid = null
                            published = null
                        }
                        "title" -> if (insideItem && title == null) {
                            title = parser.safeNextText()
                        }
                        "link" -> if (insideItem && link == null) {
                            val href = parser.getAttributeValue(null, "href")
                            link = href?.trim()?.takeIf(String::isNotBlank) ?: parser.safeNextText()
                        }
                        "guid", "id" -> if (insideItem && guid == null) {
                            guid = parser.safeNextText()
                        }
                        "pubdate", "published", "updated", "date" -> if (insideItem && published == null) {
                            published = parseDate(parser.safeNextText())
                        }
                    }
                }
                XmlPullParser.END_TAG -> if (parser.name.equals("item", ignoreCase = true) ||
                    parser.name.equals("entry", ignoreCase = true)
                ) {
                    val safeTitle = title?.cleanText()?.take(MAX_TITLE_CHARS)
                    val safeUrl = normalizeArticleUrl(link ?: guid)
                    if (!safeTitle.isNullOrBlank() && safeUrl != null) {
                        items += NewsItem(
                            id = stableId(source.id, safeUrl, safeTitle),
                            sourceId = source.id,
                            sourceTitle = source.title,
                            category = source.category,
                            title = safeTitle,
                            url = safeUrl,
                            publishedAt = published,
                        )
                    }
                    insideItem = false
                }
            }
            parser.next()
        }
        return items.distinctBy(NewsItem::url)
    }

    private fun XmlPullParser.safeNextText(): String = runCatching { nextText() }.getOrDefault("")

    private fun String.cleanText(): String = this
        .replace(HTML_TAG_REGEX, " ")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace(WHITESPACE_REGEX, " ")
        .trim()

    private fun normalizeArticleUrl(value: String?): String? {
        val trimmed = value?.trim()?.take(MAX_URL_CHARS)?.takeIf(String::isNotBlank) ?: return null
        val uri = runCatching { URI(trimmed) }.getOrNull() ?: return null
        return if (uri.scheme.equals("https", ignoreCase = true) && !uri.host.isNullOrBlank()) trimmed else null
    }

    private fun parseDate(value: String): Instant? {
        val text = value.trim()
        if (text.isBlank()) return null
        return runCatching { Instant.parse(text) }.getOrNull()
            ?: runCatching { OffsetDateTime.parse(text, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant() }.getOrNull()
            ?: runCatching { ZonedDateTime.parse(text, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant() }.getOrNull()
    }

    private fun requireHttps(value: String) {
        val uri = URI(value)
        require(uri.scheme.equals("https", ignoreCase = true) && !uri.host.isNullOrBlank()) {
            "News feeds must use HTTPS"
        }
    }

    private fun readBounded(input: java.io.InputStream): ByteArray {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(16 * 1024)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            check(out.size() + read <= MAX_FEED_BYTES) { "News feed exceeds ${MAX_FEED_BYTES / 1024} KiB budget" }
            out.write(buffer, 0, read)
        }
        buffer.fill(0)
        return out.toByteArray()
    }

    private fun stableId(sourceId: String, url: String, title: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest("$sourceId\u0000$url\u0000$title".toByteArray(StandardCharsets.UTF_8))
        return digest.take(12).joinToString("") { "%02x".format(it) }
    }

    private companion object {
        const val CONNECT_TIMEOUT_MS = 8_000
        const val READ_TIMEOUT_MS = 10_000
        const val MAX_FEED_BYTES = 1_500_000
        const val MAX_ITEMS_PER_SOURCE = 30
        const val MAX_TITLE_CHARS = 360
        const val MAX_URL_CHARS = 2_048
        val HTML_TAG_REGEX = Regex("<[^>]+>")
        val WHITESPACE_REGEX = Regex("\\s+")
    }
}
