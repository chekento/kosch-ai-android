package cloud.kosch.aiandroid.ai

/**
 * Explicit product-level gate for direct networked AI execution.
 *
 * Network capability and user permission are deliberately separate. Even if a future release contains the Android
 * INTERNET permission, KAL must remain local-first until the user deliberately enables direct provider connections.
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
