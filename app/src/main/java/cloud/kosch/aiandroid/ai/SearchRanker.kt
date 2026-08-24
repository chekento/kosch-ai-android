package cloud.kosch.aiandroid.ai

import java.text.Normalizer
import java.util.Locale

data class SearchDocument(
    val id: String,
    val title: String,
    val keywords: List<String> = emptyList(),
)

object SearchRanker {
    fun rank(query: String, documents: List<SearchDocument>): List<SearchDocument> {
        val needle = query.searchVariants()
        if (needle.spaced.isEmpty()) return documents.sortedBy { it.title.lowercase(Locale.ROOT) }

        return documents.mapNotNull { document ->
            score(needle, document)?.let { score -> document to score }
        }.sortedWith(
            compareByDescending<Pair<SearchDocument, Int>> { it.second }
                .thenBy { it.first.title.lowercase(Locale.ROOT) },
        ).map { it.first }
    }

    private fun score(needle: SearchVariants, document: SearchDocument): Int? {
        val title = document.title.searchVariants()
        val keywords = document.keywords.map { it.searchVariants() }
        val candidates = listOf(title) + keywords

        val best = candidates.maxOfOrNull { candidate ->
            maxOf(
                matchScore(needle.spaced, candidate.spaced, exactScore = 1_000),
                matchScore(needle.compact, candidate.compact, exactScore = 980),
            )
        } ?: Int.MIN_VALUE

        return best.takeUnless { it == Int.MIN_VALUE }
    }

    private fun isSubsequence(needle: String, candidate: String): Boolean {
        var cursor = 0
        candidate.forEach { character ->
            if (cursor < needle.length && needle[cursor] == character) cursor += 1
        }
        return cursor == needle.length
    }

    private fun matchScore(needle: String, candidate: String, exactScore: Int): Int {
        if (needle.isEmpty() || candidate.isEmpty()) return Int.MIN_VALUE
        return when {
            candidate == needle -> exactScore
            candidate.startsWith(needle) -> 800 - candidate.length
            candidate.split(' ').any { it.startsWith(needle) } -> 650 - candidate.length
            candidate.contains(needle) -> 500 - candidate.length
            needle.length >= 3 && isSubsequence(needle, candidate) -> 250 - candidate.length
            else -> Int.MIN_VALUE
        }
    }

    private fun String.searchVariants(): SearchVariants {
        val spaced = Normalizer
        .normalize(lowercase(Locale.GERMAN), Normalizer.Form.NFD)
        .replace("\\p{M}+".toRegex(), "")
        .replace("[^a-z0-9]+".toRegex(), " ")
        .replace("\\s+".toRegex(), " ")
        .trim()
        return SearchVariants(spaced = spaced, compact = spaced.replace(" ", ""))
    }

    private data class SearchVariants(
        val spaced: String,
        val compact: String,
    )
}

enum class SmartCollection(val title: String) {
    ALL("Alle"),
    RECENT("Zuletzt"),
    AI("KI"),
    WORK("Arbeit"),
    MEDIA("Medien"),
    COMMUNICATION("Kommunikation"),
    TOOLS("Tools"),
    GAMES("Spiele"),
    HIDDEN("Verborgen"),
}

object LocalAppClassifier {
    fun belongsTo(
        collection: SmartCollection,
        label: String,
        packageName: String,
        recentPackages: List<String>,
        providerPackages: Set<String>,
    ): Boolean {
        if (collection == SmartCollection.ALL) return true
        if (collection == SmartCollection.RECENT) return packageName in recentPackages
        if (collection == SmartCollection.AI) return packageName in providerPackages

        val haystack = "${label.lowercase(Locale.ROOT)} ${packageName.lowercase(Locale.ROOT)}"
        val tokens = when (collection) {
            SmartCollection.WORK -> workTokens
            SmartCollection.MEDIA -> mediaTokens
            SmartCollection.COMMUNICATION -> communicationTokens
            SmartCollection.TOOLS -> toolTokens
            SmartCollection.GAMES -> gameTokens
            else -> emptySet()
        }
        return tokens.any(haystack::contains)
    }

    private val workTokens = setOf(
        "calendar", "docs", "drive", "mail", "office", "notion", "outlook",
        "sheets", "slack", "teams", "trello", "work",
    )
    private val mediaTokens = setOf(
        "audio", "camera", "gallery", "music", "photo", "spotify", "video", "youtube",
    )
    private val communicationTokens = setOf(
        "chat", "contact", "discord", "message", "phone", "signal", "telegram", "whatsapp",
    )
    private val toolTokens = setOf(
        "authenticator", "calculator", "clock", "file", "scanner", "settings", "tool",
    )
    private val gameTokens = setOf("game", "games", "gaming", "playgames")
}
