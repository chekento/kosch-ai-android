package cloud.kosch.aiandroid.ui

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import cloud.kosch.aiandroid.security.NetworkSecurityN1Policy
import cloud.kosch.aiandroid.security.VpnAuthorizationState
import cloud.kosch.aiandroid.security.VpnConsentGateway

/**
 * Android-facing host for the traffic-neutral N1 surface.
 *
 * A successful Activity result is intentionally not trusted by itself. Android is queried again through
 * [VpnConsentGateway] after every result and every explicit refresh request.
 */
@Composable
fun ColumnScope.SecurityNetworkCenterHost(
    onOpenFaq: () -> Unit,
) {
    val context = LocalContext.current
    var snapshot by remember {
        mutableStateOf(
            NetworkSecurityN1Policy.snapshot(
                VpnConsentGateway.authorizationState(context),
            ),
        )
    }

    val consentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val systemAuthorized =
            VpnConsentGateway.authorizationState(context) == VpnAuthorizationState.AUTHORIZED
        snapshot = NetworkSecurityN1Policy.afterConsentResult(
            resultAccepted = result.resultCode == Activity.RESULT_OK,
            systemReportsAuthorized = systemAuthorized,
        )
    }

    fun requestOrRefreshConsent() {
        VpnConsentGateway.prepareIntent(context)
            .onSuccess { intent ->
                if (intent == null) {
                    snapshot = NetworkSecurityN1Policy.snapshot(VpnAuthorizationState.AUTHORIZED)
                } else {
                    runCatching { consentLauncher.launch(intent) }
                        .onFailure {
                            snapshot = NetworkSecurityN1Policy.snapshot(VpnAuthorizationState.UNKNOWN)
                        }
                }
            }
            .onFailure {
                snapshot = NetworkSecurityN1Policy.snapshot(VpnAuthorizationState.UNKNOWN)
            }
    }

    SecurityNetworkCenterSurface(
        snapshot = snapshot,
        onRequestVpnConsent = ::requestOrRefreshConsent,
        onOpenFaq = onOpenFaq,
    )
}
