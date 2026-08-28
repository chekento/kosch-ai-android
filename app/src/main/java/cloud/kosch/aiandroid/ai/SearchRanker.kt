package cloud.kosch.aiandroid.ai

import java.text.Normalizer
import java.util.Locale

data class SearchDocument(
    val id: String,
    val title: String,
    val keywords: List<String> = emptyList(),
)

enum class SearchMatchReason {
    EXACT,
    TRANSLITERATED,
    COMPACT_EXACT,
    TOKEN_PREFIX,
    PREFIX,
    WORD_PREFIX,
    CONTAINS,
    ACRONYM,
    TYPO,
    SUBSEQUENCE,
}

data class RankedSearchDocument(
    val document: SearchDocument,
    val score: Int,
    val reason: SearchMatchReason,
)

/**
 * Fully local search scorer shared by launcher search surfaces.
 *
 * Ranking is deterministic and explainable: exact matches in the user's/script's original form win, then a bounded
 * local romanization path, compact/multi-token prefixes, regular prefixes, contained terms, acronyms, tightly bounded
 * one-edit typo recovery and finally subsequences. Short unrelated queries are never typo-guessed.
 */
object SearchRanker {
    fun rank(query: String, documents: List<SearchDocument>): List<SearchDocument> =
        rankDetailed(query, documents).map(RankedSearchDocument::document)

    fun rankDetailed(query: String, documents: List<SearchDocument>): List<RankedSearchDocument> {
        val needle = query.searchVariants()
        if (needle.spaced.isEmpty()) {
            return documents
                .sortedBy { it.title.lowercase(Locale.ROOT) }
                .map { RankedSearchDocument(it, score = 0, reason = SearchMatchReason.SUBSEQUENCE) }
        }

        return documents.mapNotNull { document ->
            score(needle, document)?.let { match ->
                RankedSearchDocument(document, match.score, match.reason)
            }
        }.sortedWith(
            compareByDescending<RankedSearchDocument> { it.score }
                .thenBy { it.document.title.lowercase(Locale.ROOT) },
        )
    }

    private fun score(needle: SearchVariants, document: SearchDocument): SearchMatch? {
        val title = document.title.searchVariants()
        val keywords = document.keywords.map { it.searchVariants() }
        return (listOf(title) + keywords)
            .mapNotNull { candidate -> bestMatch(needle, candidate) }
            .maxWithOrNull(compareBy<SearchMatch> { it.score }.thenBy { it.reason.ordinal * -1 })
    }

    private fun bestMatch(needle: SearchVariants, candidate: SearchVariants): SearchMatch? {
        val matches = buildList {
            matchScore(needle.spaced, candidate.spaced, compact = false)?.let(::add)
            matchScore(needle.compact, candidate.compact, compact = true)?.let(::add)

            val transliterated = needle.romanizedSpaced.isNotEmpty() && candidate.romanizedSpaced.isNotEmpty() &&
                (needle.usedRomanization || candidate.usedRomanization)
            if (transliterated) {
                matchScore(needle.romanizedSpaced, candidate.romanizedSpaced, compact = false)?.let { match ->
                    add(
                        SearchMatch(
                            score = (match.score - ROMANIZATION_PENALTY).coerceAtLeast(1),
                            reason = SearchMatchReason.TRANSLITERATED,
                        ),
                    )
                }
                matchScore(needle.romanizedCompact, candidate.romanizedCompact, compact = true)?.let { match ->
                    add(
                        SearchMatch(
                            score = (match.score - ROMANIZATION_PENALTY - 10).coerceAtLeast(1),
                            reason = SearchMatchReason.TRANSLITERATED,
                        ),
                    )
                }
            }

            if (needle.tokens.size > 1 && candidate.tokens.isNotEmpty() &&
                needle.tokens.all { queryToken -> candidate.tokens.any { it.startsWith(queryToken) } }
            ) {
                add(SearchMatch(970 - candidate.spaced.length, SearchMatchReason.TOKEN_PREFIX))
            }

            if (needle.compact.length in 2..8 && candidate.acronym == needle.compact) {
                add(SearchMatch(570 - candidate.spaced.length, SearchMatchReason.ACRONYM))
            }

            if (needle.spaced.length >= 4) {
                val typoDistance = candidate.tokens
                    .asSequence()
                    .filter { kotlin.math.abs(it.length - needle.spaced.length) <= 1 }
                    .map { boundedEditDistance(needle.spaced, it, maxDistance = 1) }
                    .minOrNull()
                if (typoDistance != null && typoDistance <= 1) {
                    add(SearchMatch(440 - candidate.spaced.length, SearchMatchReason.TYPO))
                }
            }
        }
        return matches.maxByOrNull(SearchMatch::score)
    }

    private fun matchScore(needle: String, candidate: String, compact: Boolean): SearchMatch? {
        if (needle.isEmpty() || candidate.isEmpty()) return null
        return when {
            candidate == needle -> SearchMatch(
                if (compact) 1_160 else 1_200,
                if (compact) SearchMatchReason.COMPACT_EXACT else SearchMatchReason.EXACT,
            )
            candidate.startsWith(needle) -> SearchMatch(900 - candidate.length, SearchMatchReason.PREFIX)
            !compact && candidate.split(' ').any { it.startsWith(needle) } ->
                SearchMatch(760 - candidate.length, SearchMatchReason.WORD_PREFIX)
            candidate.contains(needle) -> SearchMatch(600 - candidate.length, SearchMatchReason.CONTAINS)
            needle.length >= 3 && isSubsequence(needle, candidate) ->
                SearchMatch(300 - candidate.length, SearchMatchReason.SUBSEQUENCE)
            else -> null
        }
    }

    private fun isSubsequence(needle: String, candidate: String): Boolean {
        var cursor = 0
        candidate.forEach { character ->
            if (cursor < needle.length && needle[cursor] == character) cursor += 1
        }
        return cursor == needle.length
    }

    /** Small bounded Levenshtein implementation; exits early once one edit can no longer recover the row. */
    private fun boundedEditDistance(left: String, right: String, maxDistance: Int): Int {
        if (kotlin.math.abs(left.length - right.length) > maxDistance) return maxDistance + 1
        var previous = IntArray(right.length + 1) { it }
        for (i in left.indices) {
            val current = IntArray(right.length + 1)
            current[0] = i + 1
            var rowMin = current[0]
            for (j in right.indices) {
                val substitution = previous[j] + if (left[i] == right[j]) 0 else 1
                current[j + 1] = minOf(
                    current[j] + 1,
                    previous[j + 1] + 1,
                    substitution,
                )
                rowMin = minOf(rowMin, current[j + 1])
            }
            if (rowMin > maxDistance) return maxDistance + 1
            previous = current
        }
        return previous[right.length]
    }

    private fun String.searchVariants(): SearchVariants {
        val source = lowercase(Locale.ROOT)
        val spaced = source.asciiSearchForm()
        val romanizedSource = source.romanizeForSearch()
        val romanizedSpaced = romanizedSource.asciiSearchForm()
        val tokens = spaced.split(' ').filter(String::isNotBlank)
        return SearchVariants(
            spaced = spaced,
            compact = spaced.replace(" ", ""),
            tokens = tokens,
            acronym = tokens.joinToString("") { token -> token.take(1) },
            romanizedSpaced = romanizedSpaced,
            romanizedCompact = romanizedSpaced.replace(" ", ""),
            usedRomanization = romanizedSource != source,
        )
    }

    private fun String.asciiSearchForm(): String = Normalizer
        .normalize(this, Normalizer.Form.NFD)
        .replace("\\p{M}+".toRegex(), "")
        .replace("ß", "ss")
        .replace("æ", "ae")
        .replace("œ", "oe")
        .replace("ø", "o")
        .replace("ł", "l")
        .replace("đ", "d")
        .replace("ð", "d")
        .replace("þ", "th")
        .replace("[^a-z0-9]+".toRegex(), " ")
        .replace("\\s+".toRegex(), " ")
        .trim()

    /**
     * Small embedded romanization table for launcher discovery. It deliberately covers common Cyrillic and Greek
     * alphabets without pulling a network/service dependency into local search. Original-script matching is still
     * scored first and therefore never loses to the romanized fallback.
     */
    private fun String.romanizeForSearch(): String = buildString(length) {
        this@romanizeForSearch.forEach { character ->
            append(ROMANIZATION[character] ?: character)
        }
    }

    private data class SearchVariants(
        val spaced: String,
        val compact: String,
        val tokens: List<String>,
        val acronym: String,
        val romanizedSpaced: String,
        val romanizedCompact: String,
        val usedRomanization: Boolean,
    )

    private data class SearchMatch(
        val score: Int,
        val reason: SearchMatchReason,
    )

    private const val ROMANIZATION_PENALTY = 30

    private val ROMANIZATION: Map<Char, String> = mapOf(
        // Cyrillic: Russian, Ukrainian, Belarusian, Bulgarian, Serbian and common regional additions.
        'а' to "a", 'б' to "b", 'в' to "v", 'г' to "g", 'ґ' to "g", 'д' to "d",
        'е' to "e", 'ё' to "yo", 'є' to "ye", 'ж' to "zh", 'з' to "z", 'и' to "i",
        'і' to "i", 'ї' to "yi", 'й' to "y", 'к' to "k", 'л' to "l", 'м' to "m",
        'н' to "n", 'о' to "o", 'п' to "p", 'р' to "r", 'с' to "s", 'т' to "t",
        'у' to "u", 'ў' to "u", 'ф' to "f", 'х' to "kh", 'ц' to "ts", 'ч' to "ch",
        'ш' to "sh", 'щ' to "shch", 'ъ' to "", 'ы' to "y", 'ь' to "", 'э' to "e",
        'ю' to "yu", 'я' to "ya", 'ј' to "j", 'љ' to "lj", 'њ' to "nj", 'ђ' to "dj",
        'ћ' to "c", 'џ' to "dz", 'ќ' to "k", 'ѓ' to "g", 'ѕ' to "dz",
        // Greek incl. common precomposed tonos/dialytika characters.
        'α' to "a", 'ά' to "a", 'β' to "v", 'γ' to "g", 'δ' to "d", 'ε' to "e",
        'έ' to "e", 'ζ' to "z", 'η' to "i", 'ή' to "i", 'θ' to "th", 'ι' to "i",
        'ί' to "i", 'ϊ' to "i", 'ΐ' to "i", 'κ' to "k", 'λ' to "l", 'μ' to "m",
        'ν' to "n", 'ξ' to "x", 'ο' to "o", 'ό' to "o", 'π' to "p", 'ρ' to "r",
        'σ' to "s", 'ς' to "s", 'τ' to "t", 'υ' to "y", 'ύ' to "y", 'ϋ' to "y",
        'ΰ' to "y", 'φ' to "f", 'χ' to "ch", 'ψ' to "ps", 'ω' to "o", 'ώ' to "o",
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
        "calendar", "kalender", "docs", "drive", "mail", "office", "notion", "outlook",
        "sheets", "slack", "teams", "trello", "work", "arbeit", "projekt", "project", "meet",
    )
    private val mediaTokens = setOf(
        "audio", "camera", "kamera", "gallery", "galerie", "music", "musik", "photo", "foto",
        "spotify", "video", "youtube", "podcast",
    )
    private val communicationTokens = setOf(
        "chat", "contact", "kontakt", "discord", "message", "nachricht", "phone", "telefon", "signal",
        "telegram", "whatsapp", "messenger",
    )
    private val toolTokens = setOf(
        "authenticator", "calculator", "rechner", "clock", "uhr", "file", "datei", "scanner", "settings",
        "einstellungen", "tool", "werkzeug", "terminal", "vpn",
    )
    private val gameTokens = setOf("game", "games", "gaming", "playgames", "spiel", "spiele")
}
