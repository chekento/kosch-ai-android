package cloud.kosch.aiandroid.ai

import cloud.kosch.aiandroid.model.AssistantObservationSource
import java.text.Normalizer
import java.util.Locale

/**
 * Narrow deterministic parser for user-initiated one-shot visual context requests.
 * It intentionally does not treat generic "Kamera" or "Bildschirm" commands as analysis requests.
 */
object AssistantVisualContextRequestParser {
    fun parse(input: String): AssistantObservationSource? = parseRequest(input)?.source

    fun parseRequest(input: String): Request? {
        val normalized = input.normalized()
        if (normalized.isBlank()) return null

        val hasVisualIntent = visualPhrases.any(normalized::contains) ||
            (visualVerbs.any(normalized::contains) && visualObjects.any(normalized::contains))
        if (!hasVisualIntent) return null

        val source = when {
            cameraTerms.any(normalized::contains) -> AssistantObservationSource.CAMERA
            screenTerms.any(normalized::contains) -> AssistantObservationSource.SCREEN
            else -> null
        }
        return Request(source)
    }

    data class Request(val source: AssistantObservationSource?)

    private fun String.normalized(): String = Normalizer
        .normalize(lowercase(Locale.GERMAN), Normalizer.Form.NFD)
        .replace("\\p{M}+".toRegex(), "")
        .replace("[^a-z0-9 ]".toRegex(), " ")
        .replace("\\s+".toRegex(), " ")
        .trim()

    private val visualPhrases = listOf(
        "was siehst du",
        "was kannst du sehen",
        "schau dir das an",
        "sieh dir das an",
        "analysiere was du siehst",
        "analysiere was du sehen kannst",
        "look at this",
        "what do you see",
    )
    private val visualVerbs = listOf(
        "analysier",
        "ansehen",
        "anschauen",
        "erkenn",
        "siehst",
        "sehen",
        "schau",
        "look",
        "analyze",
        "analyse",
    )
    private val visualObjects = listOf(
        "bildschirm",
        "screen",
        "display",
        "kamera",
        "camera",
        "bild",
        "image",
        "das hier",
    )
    private val cameraTerms = listOf("kamera", "camera")
    private val screenTerms = listOf("bildschirm", "screen", "display")
}
