package cloud.kosch.aiandroid.ai

import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import cloud.kosch.aiandroid.model.FileInsight
import java.util.Locale

/**
 * A deliberately bounded, offline inspection pass. It never mutates a document and only reads a
 * short text prefix for formats that are safe to represent as plain text.
 */
class LocalFileIntelligenceEngine(private val resolver: ContentResolver) {
    fun inspect(uri: Uri): FileInsight {
        val metadata = queryMetadata(uri)
        val mimeType = resolver.getType(uri) ?: "application/octet-stream"
        val category = categoryFor(mimeType, metadata.name)
        val preview = if (isReadableText(mimeType, metadata.name)) readPreview(uri) else null
        val summary = summarize(metadata.name, mimeType, metadata.size, preview)

        return FileInsight(
            uri = uri,
            displayName = metadata.name,
            mimeType = mimeType,
            sizeBytes = metadata.size,
            category = category,
            summary = summary,
            preview = preview,
            suggestedName = suggestName(metadata.name, category, preview),
            safetyNote = "Nur lokal analysiert · keine Änderung ohne deine ausdrückliche Aktion",
        )
    }

    private fun queryMetadata(uri: Uri): Metadata {
        var name = uri.lastPathSegment?.substringAfterLast('/') ?: "Dokument"
        var size: Long? = null
        resolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
            null,
            null,
            null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                name = cursor.stringOrNull(OpenableColumns.DISPLAY_NAME)?.ifBlank { name } ?: name
                size = cursor.longOrNull(OpenableColumns.SIZE)?.takeIf { it >= 0 }
            }
        }
        return Metadata(name, size)
    }

    private fun readPreview(uri: Uri): String? = resolver.openInputStream(uri)?.bufferedReader()?.use { reader ->
        val buffer = CharArray(MAX_PREVIEW_CHARS)
        val count = reader.read(buffer, 0, buffer.size)
        if (count <= 0) null else String(buffer, 0, count)
            .replace("\u0000", "")
            .trim()
            .takeIf(String::isNotBlank)
    }

    private fun summarize(
        name: String,
        mimeType: String,
        size: Long?,
        preview: String?,
    ): String {
        val sizeText = size?.let(::humanReadableBytes) ?: "Größe unbekannt"
        if (preview == null) return "$name · $mimeType · $sizeText. Binärinhalt wird nicht ungefragt gelesen."

        val lines = preview.lineSequence().count()
        val words = WORD_REGEX.findAll(preview).count()
        val clipped = preview.length == MAX_PREVIEW_CHARS
        return "$name · $sizeText · mindestens $lines Zeilen und $words Wörter" +
            if (clipped) " im begrenzten Textausschnitt." else "."
    }

    private fun suggestName(name: String, category: String, preview: String?): String? {
        val extension = name.substringAfterLast('.', "").lowercase(Locale.ROOT)
        val stem = preview
            ?.lineSequence()
            ?.firstOrNull { it.isNotBlank() }
            ?.replace("[^\\p{L}\\p{N} -]".toRegex(), "")
            ?.trim()
            ?.take(42)
            ?.replace("\\s+".toRegex(), "-")
            ?.lowercase(Locale.GERMAN)
            ?.takeIf { it.length >= 4 }
            ?: return null
        val suffix = extension.takeIf(String::isNotBlank)?.let { ".$it" }.orEmpty()
        val candidate = "$stem$suffix"
        return candidate.takeUnless { it.equals(name, ignoreCase = true) || category == "Ausführbare Datei" }
    }

    private fun categoryFor(mimeType: String, name: String): String = when {
        mimeType.startsWith("text/") -> "Text"
        mimeType.startsWith("image/") -> "Bild"
        mimeType.startsWith("audio/") -> "Audio"
        mimeType.startsWith("video/") -> "Video"
        mimeType == "application/pdf" -> "PDF"
        mimeType.contains("zip") || name.endsWith(".zip", true) -> "Archiv"
        mimeType.contains("android.package") || name.endsWith(".apk", true) -> "Ausführbare Datei"
        else -> "Dokument"
    }

    private fun isReadableText(mimeType: String, name: String): Boolean =
        mimeType.startsWith("text/") ||
            mimeType in textMimeTypes ||
            name.substringAfterLast('.', "").lowercase(Locale.ROOT) in textExtensions

    private fun Cursor.stringOrNull(column: String): String? {
        val index = getColumnIndex(column)
        return if (index >= 0 && !isNull(index)) getString(index) else null
    }

    private fun Cursor.longOrNull(column: String): Long? {
        val index = getColumnIndex(column)
        return if (index >= 0 && !isNull(index)) getLong(index) else null
    }

    private fun humanReadableBytes(bytes: Long): String = when {
        bytes < 1_024 -> "$bytes B"
        bytes < 1_048_576 -> "%.1f KB".format(Locale.GERMAN, bytes / 1_024.0)
        bytes < 1_073_741_824 -> "%.1f MB".format(Locale.GERMAN, bytes / 1_048_576.0)
        else -> "%.1f GB".format(Locale.GERMAN, bytes / 1_073_741_824.0)
    }

    private data class Metadata(val name: String, val size: Long?)

    private companion object {
        const val MAX_PREVIEW_CHARS = 4_096
        val WORD_REGEX = "[\\p{L}\\p{N}']+".toRegex()
        val textMimeTypes = setOf("application/json", "application/xml", "application/csv")
        val textExtensions = setOf("csv", "json", "log", "md", "txt", "xml", "yaml", "yml")
    }
}
