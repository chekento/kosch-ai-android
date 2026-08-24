package cloud.kosch.aiandroid.system

import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.provider.DocumentsContract
import cloud.kosch.aiandroid.ai.LocalFileWorkspacePlanner
import cloud.kosch.aiandroid.model.FileWorkspaceEntry

/**
 * Owns one user-selected read/write SAF tree. It never requests broad storage access and only
 * constructs descendants returned by the selected DocumentsProvider.
 */
class WorkspaceTreeManager(context: Context) {
    private val appContext = context.applicationContext
    private val resolver = appContext.contentResolver
    private val preferences = appContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    val currentTreeUri: Uri?
        get() = preferences.getString(KEY_TREE_URI, null)?.let(Uri::parse)

    fun adopt(treeUri: Uri): Result<Unit> = runCatching {
        require(DocumentsContract.isTreeUri(treeUri)) { "Kein gültiger Android-Arbeitsordner" }
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        val previous = currentTreeUri
        resolver.takePersistableUriPermission(treeUri, flags)
        check(preferences.edit().putString(KEY_TREE_URI, treeUri.toString()).commit()) {
            "Arbeitsordner konnte nicht gespeichert werden"
        }
        if (previous != null && previous != treeUri) releaseGrant(previous)
    }

    fun releaseCurrent(): Result<Boolean> = runCatching {
        val treeUri = currentTreeUri ?: return@runCatching false
        check(preferences.edit().remove(KEY_TREE_URI).commit()) {
            "Arbeitsordner konnte nicht vergessen werden"
        }
        releaseGrant(treeUri)
        true
    }

    fun rootDirectory(): Result<FileWorkspaceEntry> = runCatching {
        val treeUri = requireTree()
        val rootUri = DocumentsContract.buildDocumentUriUsingTree(
            treeUri,
            DocumentsContract.getTreeDocumentId(treeUri),
        )
        queryOne(rootUri) ?: error("Arbeitsordner ist nicht mehr erreichbar")
    }

    fun listChildren(directoryUri: Uri): Result<List<FileWorkspaceEntry>> = runCatching {
        val treeUri = requireTree()
        require(directoryUri.authority == treeUri.authority) { "Ordner liegt außerhalb des Arbeitsbereichs" }
        val documentId = DocumentsContract.getDocumentId(directoryUri)
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, documentId)
        val result = mutableListOf<FileWorkspaceEntry>()
        resolver.query(childrenUri, PROJECTION, null, null, null)?.use { cursor ->
            while (cursor.moveToNext() && result.size < MAX_CHILDREN) {
                cursor.toEntry(treeUri)?.let(result::add)
            }
        } ?: error("Ordnerinhalt konnte nicht gelesen werden")
        result
    }

    fun createDirectory(parentUri: Uri, rawName: String): Result<Uri> = runCatching {
        val name = LocalFileWorkspacePlanner.validateName(rawName).getOrThrow()
        requireWithinTree(parentUri)
        DocumentsContract.createDocument(
            resolver,
            parentUri,
            DocumentsContract.Document.MIME_TYPE_DIR,
            name,
        ) ?: error("Ordner konnte nicht erstellt werden")
    }

    fun rename(entryUri: Uri, rawName: String): Result<Uri> = runCatching {
        val name = LocalFileWorkspacePlanner.validateName(rawName).getOrThrow()
        requireWithinTree(entryUri)
        DocumentsContract.renameDocument(resolver, entryUri, name)
            ?: error("Dokument konnte nicht umbenannt werden")
    }

    fun delete(entryUri: Uri): Result<Unit> = runCatching {
        requireWithinTree(entryUri)
        check(DocumentsContract.deleteDocument(resolver, entryUri)) {
            "Dokument konnte nicht gelöscht werden"
        }
    }

    private fun queryOne(uri: Uri): FileWorkspaceEntry? = resolver.query(
        uri,
        PROJECTION,
        null,
        null,
        null,
    )?.use { cursor ->
        if (cursor.moveToFirst()) cursor.toEntry(requireTree()) else null
    }

    private fun Cursor.toEntry(treeUri: Uri): FileWorkspaceEntry? {
        val documentId = string(DocumentsContract.Document.COLUMN_DOCUMENT_ID) ?: return null
        val name = string(DocumentsContract.Document.COLUMN_DISPLAY_NAME)?.trim().orEmpty()
            .ifBlank { "Unbenannt" }
        val mimeType = string(DocumentsContract.Document.COLUMN_MIME_TYPE)
            ?: "application/octet-stream"
        val flags = long(DocumentsContract.Document.COLUMN_FLAGS)?.toInt() ?: 0
        return FileWorkspaceEntry(
            uri = DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId),
            documentId = documentId,
            displayName = name.take(LocalFileWorkspacePlanner.MAX_NAME_LENGTH),
            mimeType = mimeType,
            sizeBytes = long(DocumentsContract.Document.COLUMN_SIZE)?.takeIf { it >= 0L },
            lastModifiedEpochMillis = long(DocumentsContract.Document.COLUMN_LAST_MODIFIED)?.takeIf { it > 0L },
            isDirectory = mimeType == DocumentsContract.Document.MIME_TYPE_DIR,
            canRename = flags and DocumentsContract.Document.FLAG_SUPPORTS_RENAME != 0,
            canDelete = flags and DocumentsContract.Document.FLAG_SUPPORTS_DELETE != 0,
            canCreateChildren = flags and DocumentsContract.Document.FLAG_DIR_SUPPORTS_CREATE != 0,
            category = LocalFileWorkspacePlanner.categoryFor(mimeType, name),
        )
    }

    private fun requireWithinTree(uri: Uri) {
        val treeUri = requireTree()
        require(uri.authority == treeUri.authority) { "Dokument liegt außerhalb des Arbeitsbereichs" }
        DocumentsContract.getDocumentId(uri)
    }

    private fun requireTree(): Uri = currentTreeUri ?: error("Kein Arbeitsordner ausgewählt")

    private fun releaseGrant(uri: Uri) {
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        runCatching { resolver.releasePersistableUriPermission(uri, flags) }
    }

    private fun Cursor.string(column: String): String? {
        val index = getColumnIndex(column)
        return if (index >= 0 && !isNull(index)) getString(index) else null
    }

    private fun Cursor.long(column: String): Long? {
        val index = getColumnIndex(column)
        return if (index >= 0 && !isNull(index)) getLong(index) else null
    }

    private companion object {
        const val PREFERENCES_NAME = "kosch_workspace_tree_v1"
        const val KEY_TREE_URI = "workspace_tree_uri"
        const val MAX_CHILDREN = 500
        val PROJECTION = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
            DocumentsContract.Document.COLUMN_FLAGS,
        )
    }
}
