package cloud.kosch.aiandroid.ai

import cloud.kosch.aiandroid.model.AiSettings
import cloud.kosch.aiandroid.model.PrivacySettings

/**
 * Explicit product-level gate for direct networked AI execution.
 *
 * Android network capability and user permission are deliberately separate. KAL remains local-first unless BOTH
 * existing settings gates are enabled: AI provider networking and the broader privacy/network permission. No third
 * persistent cloud switch is introduced here.
 */
enum class KalCloudAccessMode {
    OFF,
    CONNECTED_PROVIDERS_ONLY,
}

enum class KalCloudRequestOrigin {
    USER_ACTION,
    ASSISTANT_CONFIRMED_ACTION,
    BACKGROUND,
}

data class KalCloudRequest(
    val providerId: String,
    val origin: KalCloudRequestOrigin,
    val providerConnected: Boolean,
    val containsUserContent: Boolean,
)

data class KalCloudAccessDecision(
    val allowed: Boolean,
    val reason: String,
    val requiresContentDisclosure: Boolean = false,
)

object KalCloudAccessPolicy {
    fun effectiveMode(ai: AiSettings, privacy: PrivacySettings): KalCloudAccessMode =
        if (ai.networkProvidersEnabled && privacy.allowNetworkFeatures) {
            KalCloudAccessMode.CONNECTED_PROVIDERS_ONLY
        } else {
            KalCloudAccessMode.OFF
        }

    fun evaluate(
        ai: AiSettings,
        privacy: PrivacySettings,
        request: KalCloudRequest,
    ): KalCloudAccessDecision = evaluate(
        mode = effectiveMode(ai, privacy),
        request = request,
    )

    fun evaluate(
        mode: KalCloudAccessMode,
        request: KalCloudRequest,
    ): KalCloudAccessDecision {
        val profile = KalProviderConnectionRegistry.profile(request.providerId)
            ?: return denied("Unbekannter Provider")

        if (profile.networkBoundary == KalNetworkExecutionBoundary.NONE) {
            return KalCloudAccessDecision(
                allowed = true,
                reason = "Lokale Route benötigt keinen Cloud-Zugriff",
                requiresContentDisclosure = false,
            )
        }

        if (mode == KalCloudAccessMode.OFF) {
            return denied("Direkter Cloud-Zugriff ist in KAL ausgeschaltet")
        }
        if (request.origin == KalCloudRequestOrigin.BACKGROUND) {
            return denied("KAL startet keine Provider-Anfrage im Hintergrund")
        }
        if (!request.providerConnected) {
            return denied("Provider ist nicht ausdrücklich verbunden")
        }

        return KalCloudAccessDecision(
            allowed = true,
            reason = "Explizit verbundener Provider und bestätigte Vordergrundaktion",
            requiresContentDisclosure = request.containsUserContent,
        )
    }

    private fun denied(reason: String) = KalCloudAccessDecision(
        allowed = false,
        reason = reason,
        requiresContentDisclosure = false,
    )
}
