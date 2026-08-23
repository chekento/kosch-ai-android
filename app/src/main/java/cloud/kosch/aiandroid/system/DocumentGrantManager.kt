package cloud.kosch.aiandroid.system

import android.content.Context
import android.content.Intent
import android.net.Uri

/** Owns exactly one long-lived, read-only SAF document grant and releases the previous one. */
class DocumentGrantManager(context: Context) {
    private val appContext = context.applicationContext
    private val resolver = appContext.contentResolver
    private val preferences = appContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    val currentUri: Uri?
        get() = preferences.getString(KEY_URI, null)?.let(Uri::parse)

    fun adopt(uri: Uri): Result<Unit> = runCatching {
        val previous = currentUri
        if (previous == uri) return@runCatching

        resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        preferences.edit().putString(KEY_URI, uri.toString()).commit()
        if (previous != null) releaseGrant(previous)
    }

    fun releaseCurrent(): Result<Boolean> = runCatching {
        val uri = currentUri ?: return@runCatching false
        preferences.edit().remove(KEY_URI).commit()
        releaseGrant(uri)
        true
    }

    private fun releaseGrant(uri: Uri) {
        runCatching {
            resolver.releasePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private companion object {
        const val PREFERENCES_NAME = "kosch_document_grants"
        const val KEY_URI = "retained_document_uri_v1"
    }
}
