package cloud.kosch.aiandroid.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cloud.kosch.aiandroid.LauncherSettingsController
import cloud.kosch.aiandroid.ai.KalCloudAccessMode
import cloud.kosch.aiandroid.ai.KalCloudAccessPolicy
import cloud.kosch.aiandroid.ai.KalConnectionMaturity
import cloud.kosch.aiandroid.ai.KalProviderAuthMode
import cloud.kosch.aiandroid.ai.KalProviderConnectionProfile
import cloud.kosch.aiandroid.ai.KalProviderConnectionRegistry
import cloud.kosch.aiandroid.ai.OpenRouterOAuthConnector
import cloud.kosch.aiandroid.ai.OpenRouterOAuthResult
import cloud.kosch.aiandroid.security.SecureCredentialType
import cloud.kosch.aiandroid.security.SecureCredentialVault
import cloud.kosch.aiandroid.ui.theme.DeepSurface
import cloud.kosch.aiandroid.ui.theme.Mint
import cloud.kosch.aiandroid.ui.theme.MutedMist
import cloud.kosch.aiandroid.ui.theme.Sky
import cloud.kosch.aiandroid.ui.theme.Warm

/**
 * Device-aware KAL provider connection editor.
 *
 * Credentials never enter LauncherSettingsDocument. Connecting an account is deliberately distinct from allowing
 * KAL to route user prompts to it: OAuth needs the general network capability, while actual model execution still
 * requires both existing AI + Privacy opt-ins.
 */
@Composable
fun ColumnScope.KalProviderConnectionsEditor(settings: LauncherSettingsController) {
    val context = LocalContext.current
    val vault = remember(context) { SecureCredentialVault(context) }
    val openRouterConnector = remember(context) { OpenRouterOAuthConnector(context, vault) }
    fun hasOpenRouterCredential(): Boolean =
        vault.contains("openrouter", SecureCredentialType.OAUTH_GENERATED_KEY) ||
            vault.contains("openrouter", SecureCredentialType.API_KEY)

    var openRouterConnected by remember { mutableStateOf(hasOpenRouterCredential()) }
    var openRouterConnecting by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    DisposableEffect(openRouterConnector) {
        onDispose { openRouterConnector.close() }
    }

    val ai = settings.document.ai
    val privacy = settings.document.privacy
    val networkAllowed = privacy.allowNetworkFeatures
    val cloudMode = KalCloudAccessPolicy.effectiveMode(ai, privacy)
    val cloudExecutionEnabled = cloudMode == KalCloudAccessMode.CONNECTED_PROVIDERS_ONLY

    LazyColumn(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text(
                "KAL Provider Connections",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "Direkte Modellverbindungen mit OAuth/PKCE, kurzlebiger Identity, BYOK oder eigenem Endpoint. KAL bleibt ohne Freigabe vollständig local-first.",
                color = MutedMist,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        item {
            Surface(
                color = if (cloudExecutionEnabled) Mint.copy(alpha = 0.10f) else Warm.copy(alpha = 0.10f),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        if (cloudExecutionEnabled) {
                            "Direkte Provider-Nutzung: FREIGEGEBEN"
                        } else {
                            "Direkte Provider-Nutzung: AUS"
                        },
                        color = if (cloudExecutionEnabled) Mint else Warm,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        "Die Netzwerkfreigabe erlaubt Verbindungsaufbau und Account-Verwaltung. Erst der zusätzliche AI-Routing-Schalter erlaubt KAL, bestätigte Anfragen an verbundene Provider zu senden.",
                        color = MutedMist,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    NetworkGateRow(
                        title = "Netzwerkfunktionen erlauben",
                        subtitle = "Übergeordnete Privacy-Freigabe für KAL-eigene Netzwerkaktionen und Provider-Login.",
                        checked = privacy.allowNetworkFeatures,
                        onCheckedChange = { enabled ->
                            settings.applyPrivacy(privacy.copy(allowNetworkFeatures = enabled))
                        },
                    )
                    NetworkGateRow(
                        title = "Externe KI-Provider verwenden",
                        subtitle = "AI-Routing darf verbundene Netzwerkprovider berücksichtigen und bestätigte Prompts senden.",
                        checked = ai.networkProvidersEnabled,
                        onCheckedChange = { enabled ->
                            settings.applyAi(ai.copy(networkProvidersEnabled = enabled))
                        },
                    )
                }
            }
        }

        statusMessage?.let { message ->
            item {
                Surface(color = Sky.copy(alpha = 0.10f), shape = RoundedCornerShape(14.dp)) {
                    Text(message, modifier = Modifier.fillMaxWidth().padding(11.dp), color = Sky)
                }
            }
        }

        item {
            ProviderConnectionCard(
                profile = requireNotNull(KalProviderConnectionRegistry.profile("openrouter")),
                status = when {
                    openRouterConnected && cloudExecutionEnabled -> "Verbunden · für AI-Routing freigegeben"
                    openRouterConnected -> "Verbunden · Prompt-Routing bleibt aus"
                    openRouterConnecting -> "Autorisierung läuft im Systembrowser …"
                    else -> "Nicht verbunden"
                },
                statusPositive = openRouterConnected,
            ) {
                if (openRouterConnected) {
                    Text(
                        "Schlüssel verschlüsselt im Android Keystore · nicht Bestandteil portabler Backups.",
                        color = MutedMist,
                        style = MaterialTheme.typography.labelSmall,
                    )
                    OutlinedButton(
                        onClick = {
                            openRouterConnector.cancel()
                            vault.deleteProvider("openrouter")
                            openRouterConnected = false
                            openRouterConnecting = false
                            statusMessage = "OpenRouter wurde von diesem Gerät getrennt."
                        },
                    ) {
                        Text("Trennen")
                    }
                } else {
                    Button(
                        enabled = networkAllowed && !openRouterConnecting,
                        onClick = {
                            openRouterConnecting = true
                            statusMessage = null
                            val result = runCatching {
                                openRouterConnector.prepare { oauthResult ->
                                    openRouterConnecting = false
                                    when (oauthResult) {
                                        is OpenRouterOAuthResult.Connected -> {
                                            openRouterConnected = hasOpenRouterCredential()
                                            statusMessage = if (openRouterConnected) {
                                                "OpenRouter erfolgreich mit KAL verbunden. Prompt-Routing bleibt von der separaten Freigabe oben abhängig."
                                            } else {
                                                "OpenRouter-Autorisierung war erfolgreich, der lokale Schlüsselstatus konnte aber nicht bestätigt werden."
                                            }
                                        }
                                        is OpenRouterOAuthResult.Failed -> statusMessage = oauthResult.reason
                                        OpenRouterOAuthResult.Cancelled -> statusMessage = "OpenRouter-Verbindung abgebrochen."
                                    }
                                }
                            }
                            val plan = result.getOrNull()
                            if (plan == null) {
                                openRouterConnecting = false
                                statusMessage = result.exceptionOrNull()?.message
                                    ?: "OpenRouter-Verbindung konnte nicht gestartet werden."
                            } else if (!openRouterConnector.openAuthorizationPage(plan)) {
                                openRouterConnecting = false
                                statusMessage = "Systembrowser konnte für OpenRouter nicht geöffnet werden."
                            }
                        },
                    ) {
                        Text("Mit OpenRouter verbinden")
                    }
                }
                if (!networkAllowed && !openRouterConnected) {
                    Text(
                        "Aktiviere für den Login nur „Netzwerkfunktionen erlauben“. Der AI-Routing-Schalter kann dabei AUS bleiben.",
                        color = MutedMist,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }

        item {
            Text(
                "Weitere direkte Verbindungen",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "KAL zeigt nur Auth-Verfahren an, die wir als Provider-Fähigkeit kennen. Ein Eintrag ist erst klickbar, wenn der konkrete native Flow implementiert und getestet ist.",
                color = MutedMist,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        items(
            KalProviderConnectionRegistry.profiles.filter { profile ->
                profile.id !in setOf("openrouter", "local-runtime")
            },
            key = { it.id },
        ) { profile ->
            ProviderConnectionCard(
                profile = profile,
                status = providerReadiness(profile),
                statusPositive = false,
            )
        }

        item {
            HorizontalDivider()
            Text(
                "Lokale Modelle bleiben unabhängig davon verfügbar. KAL sendet niemals im Hintergrund an einen LLM-Provider; Screen- und Camera-Awareness behalten zusätzlich ihre eigenen Opt-ins.",
                color = MutedMist,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun NetworkGateRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium)
            Text(subtitle, color = MutedMist, style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ProviderConnectionCard(
    profile: KalProviderConnectionProfile,
    status: String,
    statusPositive: Boolean,
    actions: @Composable ColumnScope.() -> Unit = {},
) {
    Surface(color = DeepSurface.copy(alpha = 0.96f), shape = RoundedCornerShape(18.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(13.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(profile.displayName, fontWeight = FontWeight.SemiBold)
                    Text(
                        profile.authOptions.joinToString(" · ") { option -> authModeLabel(option.mode) },
                        color = MutedMist,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
                Text(
                    status,
                    color = if (statusPositive) Mint else Sky,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            profile.authOptions.firstOrNull { it.recommended }?.let { recommended ->
                Text(
                    "Empfohlen: ${recommended.label}",
                    color = Mint,
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(recommended.note, color = MutedMist, style = MaterialTheme.typography.bodySmall)
            }
            actions()
        }
    }
}

private fun providerReadiness(profile: KalProviderConnectionProfile): String {
    val recommended = profile.authOptions.firstOrNull { it.recommended }
    return when (recommended?.maturity) {
        KalConnectionMaturity.SUPPORTED -> "Auth-Core unterstützt · UI folgt"
        KalConnectionMaturity.CONFIGURATION_REQUIRED -> "Provider-Konfiguration erforderlich"
        KalConnectionMaturity.FALLBACK_ONLY -> "Nur Fallback"
        null -> "Nicht konfiguriert"
    }
}

private fun authModeLabel(mode: KalProviderAuthMode): String = when (mode) {
    KalProviderAuthMode.OAUTH_PKCE -> "OAuth + PKCE"
    KalProviderAuthMode.OAUTH_USER -> "OAuth"
    KalProviderAuthMode.ENTRA_ID -> "Entra ID"
    KalProviderAuthMode.API_KEY -> "API-Key"
    KalProviderAuthMode.CUSTOM_ENDPOINT -> "Endpoint"
    KalProviderAuthMode.LOCAL_RUNTIME -> "Lokal"
    KalProviderAuthMode.APP_HANDOFF -> "App-Handoff"
}
