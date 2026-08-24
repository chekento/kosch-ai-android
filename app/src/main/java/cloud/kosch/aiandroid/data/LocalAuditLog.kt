package cloud.kosch.aiandroid.data

import android.content.Context
import cloud.kosch.aiandroid.model.AuditAction
import cloud.kosch.aiandroid.model.AuditEvent
import cloud.kosch.aiandroid.model.AuditOutcome
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

/** Metadata-only local history. Its schema intentionally has no free-text field. */
class LocalAuditLog(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    @Synchronized
    fun append(action: AuditAction, outcome: AuditOutcome) {
        val cutoff = System.currentTimeMillis() - RETENTION_MILLIS
        val updated = (listOf(AuditEvent(System.currentTimeMillis(), action, outcome)) + events())
            .filter { it.timestampEpochMillis >= cutoff }
            .take(MAX_EVENTS)
        write(updated)
    }

    @Synchronized
    fun events(): List<AuditEvent> = runCatching {
        val cutoff = System.currentTimeMillis() - RETENTION_MILLIS
        val array = JSONArray(preferences.getString(KEY_EVENTS, "[]"))
        buildList {
            repeat(array.length().coerceAtMost(MAX_EVENTS)) { index ->
                val item = array.optJSONObject(index) ?: return@repeat
                val timestamp = item.optLong("time", -1L).takeIf { it > 0L } ?: return@repeat
                val action = runCatching { AuditAction.valueOf(item.optString("action")) }.getOrNull()
                    ?: return@repeat
                val outcome = runCatching { AuditOutcome.valueOf(item.optString("outcome")) }.getOrNull()
                    ?: return@repeat
                add(AuditEvent(timestamp, action, outcome))
            }
        }.filter { it.timestampEpochMillis >= cutoff }
    }.getOrDefault(emptyList())

    @Synchronized
    fun clear() {
        preferences.edit().remove(KEY_EVENTS).apply()
    }

    fun exportCsv(): ByteArray = AuditCsv.encode(events()).encodeToByteArray()

    private fun write(events: List<AuditEvent>) {
        val array = JSONArray()
        events.forEach { event ->
            array.put(
                JSONObject()
                    .put("time", event.timestampEpochMillis)
                    .put("action", event.action.name)
                    .put("outcome", event.outcome.name),
            )
        }
        preferences.edit().putString(KEY_EVENTS, array.toString()).apply()
    }

    companion object {
        const val MAX_EVENTS = 250
        const val RETENTION_DAYS = 90
        private const val RETENTION_MILLIS = RETENTION_DAYS * 24L * 60L * 60L * 1_000L
        private const val PREFERENCES_NAME = "kosch_local_audit_v1"
        private const val KEY_EVENTS = "events"
    }
}

object AuditCsv {
    fun encode(events: List<AuditEvent>): String = buildString {
        appendLine("timestamp_utc,action,outcome")
        events.forEach { event ->
            append(Instant.ofEpochMilli(event.timestampEpochMillis).toString())
            append(',')
            append(event.action.name)
            append(',')
            appendLine(event.outcome.name)
        }
    }
}
