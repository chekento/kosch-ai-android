package cloud.kosch.aiandroid.ai

import cloud.kosch.aiandroid.model.FileWorkspaceEntry
import cloud.kosch.aiandroid.model.FileWorkspaceSummary
import java.text.Normalizer
import java.util.Locale

/** Content-free local organization signals for a user-selected SAF directory. */
object LocalFileWorkspacePlanner {
    data class Fact(
        val displayName: String,
        val sizeBytes: Long?,
        val isDirectory: Boolean,
        val category: String,
    )

    fun validateName(raw: String): Result<String> = runCatching {
        val value = raw.trim()
        require(value.isNotEmpty()) { "Name darf nicht leer sein" }
        require(value.length <= MAX_NAME_LENGTH) { "Name ist zu lang" }
        require(value != "." && value != "..") { "Dieser Name ist reserviert" }
        require(value.none { it == '/' || it == '\\' || it.code < 0x20 || it.code == 0x7F }) {
            "Name enthält unzulässige Zeichen"
        }
        require(value.normalizeKey().isNotEmpty()) { "Name enthält keine nutzbaren Zeichen" }
        value
    }

    fun categoryFor(mimeType: String, name: String): String = when {
        mimeType == DIRECTORY_MIME_TYPE -> "Ordner"
        mimeType.startsWith("text/") -> "Text"
        mimeType.startsWith("image/") -> "Bilder"
        mimeType.startsWith("audio/") -> "Audio"
        mimeType.startsWith("video/") -> "Video"
        mimeType == "application/pdf" -> "PDF"
        mimeType.contains("zip") || name.endsWith(".zip", true) -> "Archive"
        mimeType.contains("android.package") || name.endsWith(".apk", true) -> "Apps"
        else -> "Dokumente"
    }

    fun analyze(entries: List<FileWorkspaceEntry>): FileWorkspaceSummary = analyzeFacts(
        entries.map { Fact(it.displayName, it.sizeBytes, it.isDirectory, it.category) },
    )

    internal fun analyzeFacts(entries: List<Fact>): FileWorkspaceSummary {
        val files = entries.filterNot(Fact::isDirectory)
        val duplicateGroups = files
            .groupBy { it.displayName.normalizeKey() }
            .count { (key, values) -> key.isNotEmpty() && values.size > 1 }
        return FileWorkspaceSummary(
            fileCount = files.size,
            directoryCount = entries.count(Fact::isDirectory),
            knownBytes = files.sumOf { it.sizeBytes?.coerceAtLeast(0L) ?: 0L },
            duplicateNameGroups = duplicateGroups,
            categoryCounts = files.groupingBy(Fact::category).eachCount().toSortedMap(),
            largestFiles = files
                .filter { it.sizeBytes != null }
                .sortedByDescending { it.sizeBytes }
                .take(MAX_LARGEST_FILES)
                .map(Fact::displayName),
        )
    }

    private fun String.normalizeKey(): String = Normalizer
        .normalize(lowercase(Locale.ROOT), Normalizer.Form.NFKC)
        .replace("\\s+".toRegex(), " ")
        .trim()

    const val MAX_NAME_LENGTH = 120
    const val DIRECTORY_MIME_TYPE = "vnd.android.document/directory"
    private const val MAX_LARGEST_FILES = 3
}
