package cloud.kosch.aiandroid.ui

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cloud.kosch.aiandroid.security.NetworkSecurityN1Policy
import cloud.kosch.aiandroid.security.VpnAuthorizationState
import cloud.kosch.aiandroid.security.VpnConflictState
import cloud.kosch.aiandroid.security.VpnConsentGateway
import cloud.kosch.aiandroid.ui.theme.MutedMist
import cloud.kosch.aiandroid.ui.theme.Warm

/**
 * Android-facing host for the traffic-neutral N1 surface.
 *
 * A successful Activity result is intentionally not trusted by itself. Android is queried again through
 * [VpnConsentGateway] after every result and explicit refresh. Existing VPN transports are detected before
 * `VpnService.prepare()` is launched and require a second, visible acknowledgement.
 */
@Composable
fun ColumnScope.SecurityNetworkCenterHost(
    onOpenFaq: () -> Unit,
) {
    val context = LocalContext.current
    val initialInspection = remember(context) { VpnConsentGateway.inspect(context) }
    var snapshot by remember(context) {
        mutableStateOf(NetworkSecurityN1Policy.snapshot(initialInspection.authorization))
    }
    var conflictState by remember(context) { mutableStateOf(initialInspection.conflict) }
    var conflictDialogVisible by remember { mutableStateOf(false) }

    val consentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val inspection = VpnConsentGateway.inspect(context)
        conflictState = inspection.conflict
        snapshot = NetworkSecurityN1Policy.afterConsentResult(
            resultAccepted = result.resultCode == Activity.RESULT_OK,
            systemReportsAuthorized = inspection.authorization == VpnAuthorizationState.AUTHORIZED,
        )
    }

    fun launchAndroidConsent() {
        VpnConsentGateway.prepareIntent(context)
            .onSuccess { intent ->
                if (intent == null) {
                    conflictState = VpnConflictState.NONE_DETECTED
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

    fun requestOrRefreshConsent() {
        val inspection = VpnConsentGateway.inspect(context)
        conflictState = inspection.conflict
        snapshot = NetworkSecurityN1Policy.snapshot(inspection.authorization)

        when {
            inspection.authorization == VpnAuthorizationState.AUTHORIZED -> Unit
            inspection.conflict == VpnConflictState.NONE_DETECTED -> launchAndroidConsent()
            else -> conflictDialogVisible = true
        }
    }

    if (conflictState != VpnConflictState.NONE_DETECTED) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Warm.copy(alpha = 0.12f),
            shape = RoundedCornerShape(18.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(13.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    Icons.Rounded.WarningAmber,
                    contentDescription = null,
                    tint = Warm,
                    modifier = Modifier.size(22.dp),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (conflictState == VpnConflictState.ACTIVE_VPN_DETECTED) {
                            "Aktives VPN erkannt"
                        } else {
                            "VPN-Konfliktstatus unklar"
                        },
                        color = Warm,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        if (conflictState == VpnConflictState.ACTIVE_VPN_DETECTED) {
                            "Android erlaubt nur einer App gleichzeitig die VPN-Autorisierung. Eine KoSch-Freigabe kann die bestehende VPN-Verbindung beenden. N1 startet selbst trotzdem keinen Tunnel."
                        } else {
                            "KoSch konnte nicht sicher feststellen, ob bereits ein VPN aktiv ist. Der Android-Freigabedialog wird deshalb erst nach zusätzlicher Bestätigung geöffnet."
                        },
                        color = MutedMist,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }

    SecurityNetworkCenterSurface(
        snapshot = snapshot,
        onRequestVpnConsent = ::requestOrRefreshConsent,
        onOpenFaq = onOpenFaq,
    )

    if (conflictDialogVisible) {
        AlertDialog(
            onDismissRequest = { conflictDialogVisible = false },
            title = {
                Text(
                    if (conflictState == VpnConflictState.ACTIVE_VPN_DETECTED) {
                        "Bestehendes VPN kann getrennt werden"
                    } else {
                        "VPN-Status nicht sicher prüfbar"
                    },
                )
            },
            text = {
                Text(
                    if (conflictState == VpnConflictState.ACTIVE_VPN_DETECTED) {
                        "Wenn du im folgenden Android-Dialog KoSch als VPN-App autorisierst, kann Android einer bisher autorisierten VPN-App das Recht entziehen und deren Verbindung beenden. KoSch aktiviert in N1 danach noch keinen VPN-Tunnel."
                    } else {
                        "KoSch kann einen vorhandenen VPN-Konflikt nicht ausschließen. Öffne Androids VPN-Freigabe nur, wenn du akzeptierst, dass eine andere VPN-Autorisierung dadurch ersetzt werden kann."
                    },
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        conflictDialogVisible = false
                        launchAndroidConsent()
                    },
                ) {
                    Text("Trotzdem Android-Dialog öffnen")
                }
            },
            dismissButton = {
                TextButton(onClick = { conflictDialogVisible = false }) {
                    Text("Nicht öffnen")
                }
            },
        )
    }
}
