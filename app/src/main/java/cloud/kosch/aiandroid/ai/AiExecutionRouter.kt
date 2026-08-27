package cloud.kosch.aiandroid.ai

/**
 * Product-level routing for "AI everywhere" without requiring an API key.
 *
 * The order is intentional: deterministic local functionality first, then Android's on-device GenAI lane when
 * available, then an explicit installed-app handoff. Cloud/web is never silently selected by this policy.
 */
enum class AiExecutionLane(val title: String) {
    LOCAL_DETERMINISTIC("Local Core"),
    ANDROID_ON_DEVICE_GENAI("Android On-device GenAI"),
    INSTALLED_APP_HANDOFF("Installierte KI-App"),
    EXPLICIT_WEB_HANDOFF("Bewusste Web-Übergabe"),
    UNAVAILABLE("Nicht verfügbar"),
}

data class AiExecutionContext(
    val deterministicLocalSupport: Boolean,
    val onDeviceGenAiAvailable: Boolean,
    val appHandoffAvailable: Boolean,
    val userAllowsExternalHandoff: Boolean,
    val userExplicitlyRequestedWeb: Boolean = false,
)

object AiExecutionRouter {
    fun route(context: AiExecutionContext): AiExecutionLane = when {
        context.deterministicLocalSupport -> AiExecutionLane.LOCAL_DETERMINISTIC
        context.onDeviceGenAiAvailable -> AiExecutionLane.ANDROID_ON_DEVICE_GENAI
        context.appHandoffAvailable && context.userAllowsExternalHandoff -> AiExecutionLane.INSTALLED_APP_HANDOFF
        context.userAllowsExternalHandoff && context.userExplicitlyRequestedWeb -> AiExecutionLane.EXPLICIT_WEB_HANDOFF
        else -> AiExecutionLane.UNAVAILABLE
    }
}
