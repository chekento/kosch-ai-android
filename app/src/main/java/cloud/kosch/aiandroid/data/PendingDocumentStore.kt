package cloud.kosch.aiandroid.data

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

enum class PendingDocumentKind(val prefix: String) {
    BACKUP("backup"),
    AUDIT("audit"),
    INK_SVG("ink"),
}

/**
 * Process-death-safe hand-off for payloads waiting behind Android's CreateDocument picker.
 *
 * Only an opaque token is stored in saved instance state. Payloads live in the app-private,
 * no-backup directory, are bounded, atomically staged, consumed once and expired automatically.
 */
class PendingDocumentStore internal constructor(
    private val directory: File,
) {
    constructor(context: Context) : this(File(context.noBackupFilesDir, DIRECTORY_NAME))

    init {
        check(directory.exists() || directory.mkdirs()) { "Pending document directory is unavailable" }
        cleanupExpired()
    }

    fun stage(kind: PendingDocumentKind, payload: ByteArray): Result<String> = runCatching {
        var temporary: File? = null
        try {
            require(payload.isNotEmpty()) { "Export payload is empty" }
            require(payload.size <= MAX_PAYLOAD_BYTES) { "Export payload is too large" }
            val token = "${kind.prefix}-${UUID.randomUUID()}$FILE_SUFFIX"
            val target = checkedFile(token, kind)
            val tempFile = File(directory, "$token.tmp")
            temporary = tempFile
            FileOutputStream(tempFile).use { output ->
                output.write(payload)
                output.flush()
                output.fd.sync()
            }
            check(tempFile.renameTo(target)) { "Export payload could not be staged atomically" }
            token
        } finally {
            payload.fill(0)
            temporary?.takeIf { it.exists() }?.delete()
        }
    }

    fun consume(kind: PendingDocumentKind, token: String): ByteArray? {
        val file = runCatching { checkedFile(token, kind) }.getOrNull() ?: return null
        if (!file.isFile || file.length() !in 1..MAX_PAYLOAD_BYTES.toLong()) {
            file.delete()
            return null
        }
        return try {
            file.readBytes().takeIf { it.isNotEmpty() }
        } finally {
            file.delete()
        }
    }

    fun discard(kind: PendingDocumentKind, token: String?) {
        if (token == null) return
        runCatching { checkedFile(token, kind).delete() }
    }

    fun contains(kind: PendingDocumentKind, token: String?): Boolean {
        if (token == null) return false
        return runCatching {
            val file = checkedFile(token, kind)
            file.isFile && file.length() in 1..MAX_PAYLOAD_BYTES.toLong()
        }.getOrDefault(false)
    }

    fun cleanupExpired(nowEpochMillis: Long = System.currentTimeMillis()) {
        directory.listFiles().orEmpty().forEach { file ->
            val age = nowEpochMillis - file.lastModified()
            if (!file.isFile || age < 0L || age > MAX_AGE_MILLIS || file.name.endsWith(".tmp")) {
                file.delete()
            }
        }
    }

    private fun checkedFile(token: String, kind: PendingDocumentKind): File {
        require(TOKEN_PATTERN.matches(token)) { "Invalid export token" }
        require(token.startsWith("${kind.prefix}-")) { "Export token has the wrong kind" }
        val file = File(directory, token)
        require(file.canonicalFile.parentFile == directory.canonicalFile) { "Invalid export path" }
        return file
    }

    private companion object {
        const val DIRECTORY_NAME = "pending-document-exports"
        const val FILE_SUFFIX = ".payload"
        const val MAX_PAYLOAD_BYTES = 8 * 1024 * 1024
        const val MAX_AGE_MILLIS = 24L * 60L * 60L * 1_000L
        val TOKEN_PATTERN = Regex("^(backup|audit|ink)-[0-9a-fA-F-]{36}\\.payload$")
    }
}
