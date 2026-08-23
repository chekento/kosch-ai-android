package cloud.kosch.aiandroid.security

import java.net.URI

data class EndpointDecision(
    val allowed: Boolean,
    val reason: String,
)

object ProviderEndpointPolicy {
    fun validate(rawUrl: String, allowLoopbackHttp: Boolean = false): EndpointDecision {
        val uri = runCatching { URI(rawUrl.trim()) }.getOrNull()
            ?: return EndpointDecision(false, "Ungültige URL")
        val host = uri.host?.lowercase()?.trim().orEmpty()
        if (host.isBlank() || uri.userInfo != null || uri.fragment != null) {
            return EndpointDecision(false, "Endpoint benötigt einen eindeutigen Host ohne Zugangsdaten oder Fragment")
        }
        if (uri.scheme.equals("https", ignoreCase = true)) {
            return EndpointDecision(true, "Verschlüsselter Remote-Endpoint")
        }
        val loopback = host == "localhost" || host == "127.0.0.1" || host == "::1"
        if (allowLoopbackHttp && loopback && uri.scheme.equals("http", ignoreCase = true)) {
            return EndpointDecision(true, "Explizit erlaubter Loopback-Endpoint")
        }
        return EndpointDecision(false, "Remote-APIs müssen HTTPS verwenden")
    }
}
