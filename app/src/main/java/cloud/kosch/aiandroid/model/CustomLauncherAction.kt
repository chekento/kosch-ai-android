package cloud.kosch.aiandroid.model

import java.net.URI

private const val MAX_CUSTOM_ACTION_ID = 120
private const val MAX_CUSTOM_ACTION_NAME = 80
private const val MAX_CUSTOM_ACTION_URI = 2_048
private const val MAX_CUSTOM_ICON_KEY = 240

/** Portable launcher-owned action. Raw Intent extras are intentionally not part of the model. */
data class CustomLauncherAction(
    val id: String,
    val name: String,
    val iconKey: String? = null,
    val target: CustomLauncherTarget,
)

sealed interface CustomLauncherTarget {
    data class WebUrl(val url: String) : CustomLauncherTarget
    data class DeepLink(val uri: String) : CustomLauncherTarget
    data class AppLaunch(val packageName: String) : CustomLauncherTarget
    data class Internal(val action: LauncherInternalAction) : CustomLauncherTarget
}

enum class LauncherInternalAction {
    OPEN_APPS,
    OPEN_SEARCH,
    OPEN_COMMAND_PALETTE,
    OPEN_HOME_STUDIO,
    OPEN_SETTINGS,
    OPEN_ASSISTANT,
    OPEN_NOTIFICATIONS,
    OPEN_PEN_SPACE,
    OPEN_FILES,
    OPEN_BACKUP,
    OPEN_AUDIT,
    PREVIOUS_PAGE,
    NEXT_PAGE,
}

sealed interface CustomLauncherActionValidation {
    data class Valid(val normalized: CustomLauncherAction) : CustomLauncherActionValidation
    data class Invalid(val reason: String) : CustomLauncherActionValidation
}

/**
 * Import-safe validator shared by Home/Dock/Folder/Search/Gesture/Pen/Automation surfaces.
 *
 * `intent:`, `file:`, `content:`, `javascript:` and `data:` are deliberately rejected. If richer Android intents
 * are added later they need a typed allow-listed template rather than imported arbitrary extras/components.
 */
object CustomLauncherActionValidator {
    private val stableId = Regex("[a-z0-9][a-z0-9._-]{2,119}")
    private val packageName = Regex("[A-Za-z][A-Za-z0-9_]*(?:\\.[A-Za-z][A-Za-z0-9_]*)+")
    private val blockedDeepLinkSchemes = setOf("http", "https", "file", "content", "javascript", "data", "intent")

    fun validate(action: CustomLauncherAction): CustomLauncherActionValidation {
        val id = action.id.trim().lowercase()
        if (id.length > MAX_CUSTOM_ACTION_ID || !stableId.matches(id)) {
            return CustomLauncherActionValidation.Invalid("Ungültige stabile Action-ID")
        }
        val name = action.name.trim().take(MAX_CUSTOM_ACTION_NAME)
        if (name.isBlank()) return CustomLauncherActionValidation.Invalid("Name fehlt")
        val iconKey = action.iconKey?.trim()?.take(MAX_CUSTOM_ICON_KEY)?.ifBlank { null }

        val target = when (val current = action.target) {
            is CustomLauncherTarget.WebUrl -> normalizeWebUrl(current.url)
                ?: return CustomLauncherActionValidation.Invalid("Nur gültige HTTP(S)-Weblinks sind erlaubt")
            is CustomLauncherTarget.DeepLink -> normalizeDeepLink(current.uri)
                ?: return CustomLauncherActionValidation.Invalid("Deep Link ist ungültig oder verwendet ein gesperrtes Scheme")
            is CustomLauncherTarget.AppLaunch -> {
                val pkg = current.packageName.trim()
                if (!packageName.matches(pkg)) {
                    return CustomLauncherActionValidation.Invalid("Ungültiger Android-Paketname")
                }
                CustomLauncherTarget.AppLaunch(pkg)
            }
            is CustomLauncherTarget.Internal -> current
        }

        return CustomLauncherActionValidation.Valid(
            action.copy(id = id, name = name, iconKey = iconKey, target = target),
        )
    }

    private fun normalizeWebUrl(raw: String): CustomLauncherTarget.WebUrl? {
        val text = raw.trim().take(MAX_CUSTOM_ACTION_URI)
        val parsed = runCatching { URI(text) }.getOrNull() ?: return null
        val scheme = parsed.scheme?.lowercase() ?: return null
        if (scheme != "http" && scheme != "https") return null
        if (parsed.host.isNullOrBlank()) return null
        if (parsed.userInfo != null) return null
        return CustomLauncherTarget.WebUrl(parsed.normalize().toASCIIString())
    }

    private fun normalizeDeepLink(raw: String): CustomLauncherTarget.DeepLink? {
        val text = raw.trim().take(MAX_CUSTOM_ACTION_URI)
        val parsed = runCatching { URI(text) }.getOrNull() ?: return null
        val scheme = parsed.scheme?.lowercase()?.takeIf(String::isNotBlank) ?: return null
        if (scheme in blockedDeepLinkSchemes) return null
        if (parsed.isOpaque && parsed.schemeSpecificPart.isNullOrBlank()) return null
        if (!parsed.isOpaque && parsed.host.isNullOrBlank() && parsed.path.isNullOrBlank()) return null
        if (parsed.userInfo != null) return null
        return CustomLauncherTarget.DeepLink(parsed.normalize().toASCIIString())
    }
}
