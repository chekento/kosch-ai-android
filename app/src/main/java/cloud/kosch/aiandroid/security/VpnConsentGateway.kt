package cloud.kosch.aiandroid.security

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.VpnService

enum class VpnConflictState {
    NONE_DETECTED,
    ACTIVE_VPN_DETECTED,
    UNKNOWN,
}

data class VpnConsentInspection(
    val authorization: VpnAuthorizationState,
    val conflict: VpnConflictState,
)

/**
 * Single Android boundary for VPN authorization in N1.
 *
 * Android grants VPN preparation rights to only one application at a time. Granting KoSch can therefore
 * revoke another VPN application's right. Callers should use [inspect] before presenting or launching
 * consent so an existing VPN is never displaced silently.
 *
 * This gateway never starts a service, establishes a VPN interface, inspects packets, or changes routing.
 */
object VpnConsentGateway {
    /**
     * Conflict-first inspection. If an active VPN transport is already visible, N1 intentionally avoids
     * calling [VpnService.prepare] just to query KoSch authorization and reports consent required instead.
     * N1 never establishes a VPN itself, so a visible VPN transport belongs to another active VPN path.
     */
    fun inspect(context: Context): VpnConsentInspection {
        val conflict = conflictState(context)
        val authorization = when (conflict) {
            VpnConflictState.NONE_DETECTED -> authorizationState(context)
            VpnConflictState.ACTIVE_VPN_DETECTED -> VpnAuthorizationState.CONSENT_REQUIRED
            VpnConflictState.UNKNOWN -> VpnAuthorizationState.UNKNOWN
        }
        return VpnConsentInspection(
            authorization = authorization,
            conflict = conflict,
        )
    }

    fun conflictState(context: Context): VpnConflictState = runCatching {
        val connectivityManager = context.getSystemService(ConnectivityManager::class.java)
            ?: return@runCatching VpnConflictState.UNKNOWN
        val activeVpnDetected = connectivityManager.allNetworks.any { network ->
            connectivityManager.getNetworkCapabilities(network)
                ?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true
        }
        if (activeVpnDetected) {
            VpnConflictState.ACTIVE_VPN_DETECTED
        } else {
            VpnConflictState.NONE_DETECTED
        }
    }.getOrDefault(VpnConflictState.UNKNOWN)

    fun authorizationState(context: Context): VpnAuthorizationState = prepareIntent(context)
        .fold(
            onSuccess = { intent ->
                if (intent == null) VpnAuthorizationState.AUTHORIZED
                else VpnAuthorizationState.CONSENT_REQUIRED
            },
            onFailure = { VpnAuthorizationState.UNKNOWN },
        )

    /**
     * Calls Android's consent boundary. Invoke this only after the user has explicitly accepted any
     * conflict warning surfaced from [inspect].
     */
    fun prepareIntent(context: Context): Result<Intent?> = runCatching {
        VpnService.prepare(context)
    }
}
