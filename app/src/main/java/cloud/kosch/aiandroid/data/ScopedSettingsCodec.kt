package cloud.kosch.aiandroid.data

import cloud.kosch.aiandroid.model.MAX_SCOPED_SETTING_RECORDS
import cloud.kosch.aiandroid.model.PortableSettingValue
import cloud.kosch.aiandroid.model.SCOPED_SETTINGS_SCHEMA_VERSION
import cloud.kosch.aiandroid.model.ScopedSettingsDocument
import java.nio.charset.StandardCharsets
import java.util.Base64

/** Deterministic portable wire format for page/object overrides. Missing entries mean INHERIT. */
object ScopedSettingsCodec {
    fun encode(document: ScopedSettingsDocument): String {
        val normalized = document.normalized()
        return buildString {
            append("schema=").append(SCOPED_SETTINGS_SCHEMA_VERSION).append('\n')
            appendScope('P', normalized.pageOverrides)
            appendScope('O', normalized.objectOverrides)
        }
    }

    fun decode(payload: String): ScopedSettingsDocument {
        require(payload.toByteArray(StandardCharsets.UTF_8).size <= MAX_BYTES) { "Scoped settings payload too large" }
        val lines = payload.lineSequence().filter(String::isNotBlank).toList()
        require(lines.firstOrNull() == "schema=$SCOPED_SETTINGS_SCHEMA_VERSION") {
            "Unsupported scoped settings schema"
        }
        require(lines.size - 1 <= MAX_SCOPED_SETTING_RECORDS) { "Too many scoped setting records" }

        val pages = linkedMapOf<String, MutableMap<String, PortableSettingValue>>()
        val objects = linkedMapOf<String, MutableMap<String, PortableSettingValue>>()
        lines.drop(1).forEach { line ->
            require(line.length <= MAX_LINE_CHARS) { "Scoped setting record too large" }
            val fields = line.split('|')
            require(fields.size == 5) { "Malformed scoped setting record" }
            val target = when (fields[0]) {
                "P" -> pages
                "O" -> objects
                else -> throw IllegalArgumentException("Unknown scoped setting owner kind")
            }
            val ownerId = unb64(fields[1])
            val featureId = unb64(fields[2])
            val type = fields[3]
            val rawValue = unb64(fields[4])
            val value = when (type) {
                "b" -> PortableSettingValue.Bool(
                    when (rawValue) {
                        "true" -> true
                        "false" -> false
                        else -> throw IllegalArgumentException("Invalid scoped boolean")
                    },
                )
                "i" -> PortableSettingValue.Integer(rawValue.toInt())
                "d" -> PortableSettingValue.Decimal(rawValue.toDouble().also {
                    require(it.isFinite()) { "Scoped decimal must be finite" }
                })
                "s" -> PortableSettingValue.Text(rawValue)
                else -> throw IllegalArgumentException("Unknown scoped setting value type")
            }
            val ownerValues = target.getOrPut(ownerId) { linkedMapOf() }
            require(ownerValues.put(featureId, value) == null) { "Duplicate scoped setting record" }
        }
        return ScopedSettingsDocument(
            pageOverrides = pages.mapValues { it.value.toMap() },
            objectOverrides = objects.mapValues { it.value.toMap() },
        ).normalized()
    }

    private fun StringBuilder.appendScope(
        marker: Char,
        values: Map<String, Map<String, PortableSettingValue>>,
    ) {
        values.toSortedMap().forEach { (ownerId, features) ->
            features.toSortedMap().forEach { (featureId, value) ->
                val (type, raw) = when (value) {
                    is PortableSettingValue.Bool -> "b" to value.value.toString()
                    is PortableSettingValue.Integer -> "i" to value.value.toString()
                    is PortableSettingValue.Decimal -> "d" to value.value.toString()
                    is PortableSettingValue.Text -> "s" to value.value
                }
                append(marker).append('|')
                    .append(b64(ownerId)).append('|')
                    .append(b64(featureId)).append('|')
                    .append(type).append('|')
                    .append(b64(raw)).append('\n')
            }
        }
    }

    private fun b64(value: String): String = Base64.getUrlEncoder().withoutPadding()
        .encodeToString(value.toByteArray(StandardCharsets.UTF_8))

    private fun unb64(value: String): String = String(
        Base64.getUrlDecoder().decode(value),
        StandardCharsets.UTF_8,
    )

    private const val MAX_BYTES = 512 * 1024
    private const val MAX_LINE_CHARS = 12 * 1024
}
