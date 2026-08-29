package cloud.kosch.aiandroid.data

import android.content.Context

/**
 * User-owned dismissal list for AI cards and every future app recommendation.
 *
 * Only stable suggestion ids are stored; no package inventory, prompt text or behavioral profile is persisted here.
 * Dismissal means "do not show this suggestion again" and never uninstalls or modifies the target app.
 */
class DismissedSuggestionStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun hiddenIds(): Set<String> = preferences.getStringSet(KEY_HIDDEN_IDS, emptySet())
        .orEmpty()
        .mapNotNull(::normalizeId)
        .take(MAX_IDS)
        .toSet()

    fun isHidden(id: String): Boolean = normalizeId(id)?.let { it in hiddenIds() } ?: false

    fun dismiss(id: String): Boolean {
        val normalized = normalizeId(id) ?: return false
        val next = (hiddenIds() + normalized).take(MAX_IDS).toSet()
        return preferences.edit().putStringSet(KEY_HIDDEN_IDS, next).commit()
    }

    fun restore(id: String): Boolean {
        val normalized = normalizeId(id) ?: return false
        val next = hiddenIds() - normalized
        return preferences.edit().putStringSet(KEY_HIDDEN_IDS, next).commit()
    }

    fun restoreAll(): Boolean = preferences.edit().remove(KEY_HIDDEN_IDS).commit()

    private fun normalizeId(value: String): String? = value
        .trim()
        .lowercase()
        .take(MAX_ID_LENGTH)
        .takeIf { it.matches(ID_PATTERN) }

    private companion object {
        const val PREFS_NAME = "dismissed-suggestions-v1"
        const val KEY_HIDDEN_IDS = "hiddenIds"
        const val MAX_IDS = 512
        const val MAX_ID_LENGTH = 160
        val ID_PATTERN = Regex("[a-z0-9][a-z0-9._:-]{0,159}")
    }
}
