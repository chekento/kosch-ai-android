package cloud.kosch.aiandroid.security

import android.content.Context
import android.content.Intent
import android.net.VpnService

/**
 * Single Android boundary for VPN authorization in N1.
 *
 * Calling [VpnService.prepare] may return the system consent Intent, but this gateway never starts a
 * service, establishes a VPN interface, inspects packets, or changes routing. Authorization can also be
 * revoked outside KoSch, so callers must re-check Android instead of trusting a previous Activity result.
 */
object VpnConsentGateway {
    fun authorizationState(context: Context): VpnAuthorizationState = prepareIntent(context)
        .fold(
            onSuccess = { intent ->
                if (intent == null) VpnAuthorizationState.AUTHORIZED
                else VpnAuthorizationState.CONSENT_REQUIRED
            },
            onFailure = { VpnAuthorizationState.UNKNOWN },
        )

    fun prepareIntent(context: Context): Result<Intent?> = runCatching {
        VpnService.prepare(context)
    }
}
