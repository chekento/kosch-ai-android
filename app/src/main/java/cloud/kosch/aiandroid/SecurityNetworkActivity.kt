package cloud.kosch.aiandroid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cloud.kosch.aiandroid.ui.NeuralGlassBackground
import cloud.kosch.aiandroid.ui.SecurityNetworkCenterHost
import cloud.kosch.aiandroid.ui.theme.Ink
import cloud.kosch.aiandroid.ui.theme.KoSchLauncherTheme
import cloud.kosch.aiandroid.ui.theme.Mint
import cloud.kosch.aiandroid.ui.theme.MutedMist

/**
 * Internal N1 surface kept separate from the persistent launcher workspace.
 * It owns Android's VPN-consent Activity result but never starts a VPN traffic engine.
 */
class SecurityNetworkActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KoSchLauncherTheme {
                var helpVisible by remember { mutableStateOf(false) }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Ink),
                ) {
                    NeuralGlassBackground(Modifier.fillMaxSize())
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .systemBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconButton(onClick = ::finish) {
                                Icon(
                                    Icons.AutoMirrored.Rounded.ArrowBack,
                                    contentDescription = "Zurück zum Launcher",
                                )
                            }
                            Spacer(Modifier.width(6.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "KoSch AI · Security",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    "Android-geschützte Freigabe · kein Traffic-Tunnel in N1",
                                    color = MutedMist,
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            }
                            Text(
                                "LOCAL",
                                color = Mint,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                            )
                        }

                        SecurityNetworkCenterHost(
                            onOpenFaq = { helpVisible = true },
                        )
                    }
                }

                if (helpVisible) {
                    AlertDialog(
                        onDismissRequest = { helpVisible = false },
                        title = { Text("Security & Network · N1") },
                        text = {
                            Text(
                                "N1 nutzt Androids VPN-Autorisierung, startet aber keinen aktiven Tunnel, " +
                                    "keine Paket- oder DNS-Inspektion, keine Firewall-Regeln, keinen Proxy, " +
                                    "keine Zertifikatsinstallation und keine Traffic-Historie. Android erlaubt " +
                                    "nur einer App gleichzeitig die VPN-Autorisierung; eine KoSch-Freigabe kann " +
                                    "deshalb eine andere VPN-App verdrängen. Bei erkanntem oder unklarem Konflikt " +
                                    "verlangt KoSch vor dem Android-Dialog eine zusätzliche Bestätigung.",
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = { helpVisible = false }) {
                                Text("Verstanden")
                            }
                        },
                    )
                }
            }
        }
    }
}
