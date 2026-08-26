package cloud.kosch.aiandroid.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cloud.kosch.aiandroid.security.NetworkEngineState
import cloud.kosch.aiandroid.security.NetworkSecuritySnapshot
import cloud.kosch.aiandroid.security.VpnAuthorizationState
import cloud.kosch.aiandroid.ui.theme.DeepSurface
import cloud.kosch.aiandroid.ui.theme.Mint
import cloud.kosch.aiandroid.ui.theme.MutedMist
import cloud.kosch.aiandroid.ui.theme.RaisedSurface
import cloud.kosch.aiandroid.ui.theme.Sky
import cloud.kosch.aiandroid.ui.theme.Violet
import cloud.kosch.aiandroid.ui.theme.Warm

private data class SecurityMetric(
    val title: String,
    val value: String,
    val detail: String,
    val icon: ImageVector,
    val healthy: Boolean,
)

/**
 * Traffic-neutral N1 surface. Rendering this screen cannot start a VPN, inspect packets or change routing.
 * The only capability callback is an explicit request to open Android's VPN consent path.
 */
@Composable
fun ColumnScope.SecurityNetworkCenterSurface(
    snapshot: NetworkSecuritySnapshot,
    onRequestVpnConsent: () -> Unit,
    onOpenFaq: () -> Unit,
) {
    val authorizationTitle = when (snapshot.vpnAuthorization) {
        VpnAuthorizationState.UNKNOWN -> "Status wird geprüft"
        VpnAuthorizationState.CONSENT_REQUIRED -> "Android-Freigabe erforderlich"
        VpnAuthorizationState.AUTHORIZED -> "Von Android autorisiert"
    }
    val authorizationDetail = when (snapshot.vpnAuthorization) {
        VpnAuthorizationState.UNKNOWN -> "KoSch nimmt keinen positiven Status an, solange Android ihn nicht bestätigt."
        VpnAuthorizationState.CONSENT_REQUIRED -> "Nur Android darf die VPN-Berechtigung erteilen. KoSch öffnet dafür den Systemdialog."
        VpnAuthorizationState.AUTHORIZED -> "Das ist nur eine Berechtigung. In N1 ist weiterhin kein VPN-Tunnel aktiv."
    }
    val metrics = listOf(
        SecurityMetric(
            title = "VPN-Grenze",
            value = authorizationTitle,
            detail = authorizationDetail,
            icon = Icons.Rounded.Security,
            healthy = snapshot.vpnAuthorization == VpnAuthorizationState.AUTHORIZED,
        ),
        SecurityMetric(
            title = "Traffic-Inspektion",
            value = "AUS",
            detail = "Keine Paketinhalte, DNS-Anfragen, Hostnamen, IP-Flows oder Byte-Zähler werden in N1 erfasst.",
            icon = Icons.Rounded.PrivacyTip,
            healthy = true,
        ),
        SecurityMetric(
            title = "Firewall",
            value = "AUS",
            detail = "Es werden keine Verbindungen blockiert oder erlaubt. Entscheidungen folgen erst in einer getrennt geprüften Stufe.",
            icon = Icons.Rounded.Lock,
            healthy = true,
        ),
        SecurityMetric(
            title = "Routing / Proxy",
            value = "DIREKT",
            detail = "KoSch verändert in N1 weder Routing noch Proxy-Konfiguration und installiert keine Zertifikate.",
            icon = Icons.Rounded.Wifi,
            healthy = true,
        ),
    )

    Surface(
        modifier = Modifier.weight(1f).fillMaxWidth(),
        color = DeepSurface.copy(alpha = 0.96f),
        shape = RoundedCornerShape(30.dp),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Surface(
                        modifier = Modifier.size(52.dp),
                        color = Sky.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(17.dp),
                    ) {
                        androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Security, contentDescription = null, tint = Sky)
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Security & Network", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text("N1 · Consent-first · traffic-neutral", color = Mint, style = MaterialTheme.typography.labelMedium)
                    }
                    Surface(
                        color = if (snapshot.engineState == NetworkEngineState.READY_FOR_N2) Mint.copy(alpha = 0.13f) else Warm.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Text(
                            if (snapshot.engineState == NetworkEngineState.READY_FOR_N2) "N2 BEREIT" else "INAKTIV",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            color = if (snapshot.engineState == NetworkEngineState.READY_FOR_N2) Mint else Warm,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            item {
                Surface(color = Violet.copy(alpha = 0.10f), shape = RoundedCornerShape(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(11.dp),
                    ) {
                        Icon(Icons.Rounded.Info, contentDescription = null, tint = Sky)
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Autorisierung ≠ aktives VPN", fontWeight = FontWeight.SemiBold)
                            Text(
                                "Diese Stufe prüft ausschließlich Androids Freigabe. Der deklarierte VPN-Service baut keinen Tunnel auf und stoppt bei einem Startversuch sofort.",
                                color = MutedMist,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }

            metrics.forEach { metric ->
                item(key = metric.title) {
                    SecurityMetricCard(metric)
                }
            }

            item {
                Surface(color = RaisedSurface, shape = RoundedCornerShape(19.dp)) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("N1-Datenbilanz", fontWeight = FontWeight.SemiBold)
                        Text(
                            "Live-Flows ${snapshot.liveFlowCount} · erlaubt ${snapshot.allowedConnectionCount} · blockiert ${snapshot.blockedConnectionCount}",
                            color = MutedMist,
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Text(
                            "Upload ${snapshot.uploadedBytes} B · Download ${snapshot.downloadedBytes} B",
                            color = MutedMist,
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Text(
                            "Alle Werte müssen in N1 konstruktiv 0 bleiben; das Sicherheitsmodell verwirft andere Zustände.",
                            color = Sky,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }

            item {
                Button(
                    onClick = onRequestVpnConsent,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        if (snapshot.vpnAuthorization == VpnAuthorizationState.AUTHORIZED) Icons.Rounded.CheckCircle else Icons.Rounded.Security,
                        contentDescription = null,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        when (snapshot.vpnAuthorization) {
                            VpnAuthorizationState.AUTHORIZED -> "Android-Freigabe erneut prüfen"
                            VpnAuthorizationState.CONSENT_REQUIRED -> "Android-VPN-Freigabe öffnen"
                            VpnAuthorizationState.UNKNOWN -> "Android-Status prüfen"
                        },
                    )
                }
            }

            item {
                OutlinedButton(onClick = onOpenFaq, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.HelpOutline, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Datenschutz, VPN-Konflikte & nächste Stufen")
                }
            }

            item {
                Surface(color = Mint.copy(alpha = 0.08f), shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("N2 bleibt gesperrt", color = Mint, fontWeight = FontWeight.SemiBold)
                        Text(
                            "Traffic-Weiterleitung, Foreground-Service, INTERNET-Recht, Live-App-Zuordnung, Firewall und Proxy werden erst gemeinsam mit Recovery-, Datenschutz- und Gerätetests freigeschaltet.",
                            color = MutedMist,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun SecurityMetricCard(metric: SecurityMetric) {
    Card(
        colors = CardDefaults.cardColors(containerColor = RaisedSurface),
        shape = RoundedCornerShape(19.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(42.dp),
                color = if (metric.healthy) Mint.copy(alpha = 0.11f) else Warm.copy(alpha = 0.11f),
                shape = RoundedCornerShape(14.dp),
            ) {
                androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                    Icon(metric.icon, contentDescription = null, tint = if (metric.healthy) Mint else Warm)
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(metric.title, fontWeight = FontWeight.SemiBold)
                    Text(
                        metric.value,
                        color = if (metric.healthy) Mint else Warm,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Text(metric.detail, color = MutedMist, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
