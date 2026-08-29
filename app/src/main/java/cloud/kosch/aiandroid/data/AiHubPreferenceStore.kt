package cloud.kosch.aiandroid.data

import android.content.Context
import cloud.kosch.aiandroid.ai.AiHubTaskIntent

/** Device-local routing preferences. Stores stable Hub ids only; no prompt text, history or app inventory. */
class AiHubPreferenceStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun preferred(intent: AiHubTaskIntent): String? = preferences
        .getString(key(intent), null)
        ?.let(::normalizeStableId)

    fun snapshot(): Map<AiHubTaskIntent, String> = AiHubTaskIntent.entries.mapNotNull { intent ->
        preferred(intent)?.let { intent to it }
    }.toMap()

    fun setPreferred(intent: AiHubTaskIntent, stableId: String): Boolean {
        val normalized = normalizeStableId(stableId) ?: return false
        return preferences.edit().putString(key(intent), normalized).commit()
    }

    fun clear(intent: AiHubTaskIntent): Boolean = preferences.edit().remove(key(intent)).commit()

    private fun key(intent: AiHubTaskIntent): String = "preferred.${intent.name}"

    private fun normalizeStableId(value: String): String? = value
        .trim()
        .lowercase()
        .take(MAX_ID_LENGTH)
        .takeIf { it.matches(ID_PATTERN) }

    private companion object {
        const val PREFS_NAME = "ai-hub-routing-v1"
        const val MAX_ID_LENGTH = 160
        val ID_PATTERN = Regex("(?:ai|browser):[a-z0-9][a-z0-9._:-]{0,156}")
    }
}
