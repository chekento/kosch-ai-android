package cloud.kosch.aiandroid.data

import cloud.kosch.aiandroid.model.CustomLauncherAction
import cloud.kosch.aiandroid.model.CustomLauncherActionValidation
import cloud.kosch.aiandroid.model.CustomLauncherActionValidator
import cloud.kosch.aiandroid.model.CustomLauncherTarget
import cloud.kosch.aiandroid.model.LauncherInternalAction
import java.nio.charset.StandardCharsets
import java.util.Base64

private const val CUSTOM_ACTION_SCHEMA = 1
const val MAX_CUSTOM_LAUNCHER_ACTIONS = 256

/** Deterministic portable codec for validated launcher actions. No arbitrary Intent extras exist in the format. */
object CustomLauncherActionCodec {
    fun encode(actions: List<CustomLauncherAction>): String {
        val normalized = normalize(actions)
        return buildString {
            append("schema=").append(CUSTOM_ACTION_SCHEMA).append('\n')
            normalized.sortedBy(CustomLauncherAction::id).forEach { action ->
                val (kind, target) = when (val current = action.target) {
                    is CustomLauncherTarget.WebUrl -> "web" to current.url
                    is CustomLauncherTarget.DeepLink -> "deep" to current.uri
                    is CustomLauncherTarget.AppLaunch -> "app" to current.packageName
                    is CustomLauncherTarget.Internal -> "internal" to current.action.name
                }
                append(listOf(action.id, action.name, action.iconKey.orEmpty(), kind, target).joinToString("|") { b64(it) })
                append('\n')
            }
        }
    }

    fun decode(payload: String): List<CustomLauncherAction> {
        require(payload.toByteArray(StandardCharsets.UTF_8).size <= MAX_BYTES) { "Custom actions payload too large" }
        val lines = payload.lineSequence().filter(String::isNotBlank).toList()
        require(lines.firstOrNull() == "schema=$CUSTOM_ACTION_SCHEMA") { "Unsupported custom action schema" }
        val parsed = lines.drop(1).take(MAX_CUSTOM_LAUNCHER_ACTIONS).map { line ->
            require(line.length <= MAX_LINE_CHARS) { "Custom action record too large" }
            val fields = line.split('|')
            require(fields.size == 5) { "Malformed custom action record" }
            val id = unb64(fields[0])
            val name = unb64(fields[1])
            val icon = unb64(fields[2]).ifBlank { null }
            val kind = unb64(fields[3])
            val targetValue = unb64(fields[4])
            val target = when (kind) {
                "web" -> CustomLauncherTarget.WebUrl(targetValue)
                "deep" -> CustomLauncherTarget.DeepLink(targetValue)
                "app" -> CustomLauncherTarget.AppLaunch(targetValue)
                "internal" -> CustomLauncherTarget.Internal(LauncherInternalAction.valueOf(targetValue))
                else -> throw IllegalArgumentException("Unknown custom action target")
            }
            val candidate = CustomLauncherAction(id, name, icon, target)
            when (val validation = CustomLauncherActionValidator.validate(candidate)) {
                is CustomLauncherActionValidation.Valid -> validation.normalized
                is CustomLauncherActionValidation.Invalid -> throw IllegalArgumentException(validation.reason)
            }
        }
        return normalize(parsed)
    }

    private fun normalize(actions: List<CustomLauncherAction>): List<CustomLauncherAction> {
        val normalized = actions.take(MAX_CUSTOM_LAUNCHER_ACTIONS).map { action ->
            when (val validation = CustomLauncherActionValidator.validate(action)) {
                is CustomLauncherActionValidation.Valid -> validation.normalized
                is CustomLauncherActionValidation.Invalid -> throw IllegalArgumentException(validation.reason)
            }
        }
        require(normalized.map(CustomLauncherAction::id).distinct().size == normalized.size) {
            "Duplicate custom action id"
        }
        return normalized
    }

    private fun b64(value: String): String = Base64.getUrlEncoder().withoutPadding()
        .encodeToString(value.toByteArray(StandardCharsets.UTF_8))

    private fun unb64(value: String): String = String(
        Base64.getUrlDecoder().decode(value),
        StandardCharsets.UTF_8,
    )

    private const val MAX_BYTES = 512 * 1024
    private const val MAX_LINE_CHARS = 8 * 1024
}
