package cloud.kosch.aiandroid.security

import android.content.Intent
import android.net.VpnService

/**
 * N1 declaration target for Android's VPN authorization contract.
 *
 * This service is intentionally inert. It never calls Builder.establish(), never opens a tunnel and never
 * forwards packets. If it is started before N2 exists, it immediately stops instead of pretending that a
 * network engine is active.
 */
class KoSchConsentVpnService : VpnService() {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        stopSelf(startId)
        return START_NOT_STICKY
    }

    override fun onRevoke() {
        stopSelf()
    }
}
