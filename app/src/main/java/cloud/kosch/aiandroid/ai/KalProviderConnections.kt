package cloud.kosch.aiandroid.ai

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

/**
 * Authentication and connection capabilities for KAL's AI provider layer.
 *
 * This is deliberately separate from [AiProviderRegistry], which models launchable app/web surfaces.
 * A provider can therefore expose an app handoff without claiming direct API connectivity, or expose
 * direct connectivity without pretending that an Android app exists.
 */
enum class KalProviderAuthMode {
    OAUTH_PKCE,
    OAUTH_USER,
    ENTRA_ID,
    API_KEY,
    CUSTOM_ENDPOINT,
    LOCAL_RUNTIME,
    APP_HANDOFF,
}

enum class KalConnectionMaturity {
    /** The provider publicly supports the auth family; product registration/configuration may still be required. */
    SUPPORTED,

    /** KAL needs provider-side client/tenant/redirect configuration before a login can be offered. */
    CONFIGURATION_REQUIRED,

    /** Safe fallback route, not a direct authenticated model connection. */
    FALLBACK_ONLY,
}

enum class KalNetworkExecutionBoundary {
    /** No network capability is required by KAL for this route. */
    NONE,

    /**
     * Direct provider traffic must cross an explicit network-capable boundary.
     * The current launcher release intentionally has no INTERNET permission, so this cannot silently execute
     * inside the launcher process.
     */
    EXPLICIT_NETWORK_CONNECTOR,
}

data class KalProviderAuthOption(
    val mode: KalProviderAuthMode,
    val maturity: KalConnectionMaturity,
    val recommended: Boolean = false,
    val label: String,
    val note: String,
)

data class KalProviderConnectionProfile(
    val id: String,
    /** Existing AiProviderRegistry id when this connection augments a launchable provider. */
    val aiProviderId: String? = null,
    val displayName: String,
    val authOptions: List<KalProviderAuthOption>,
    val networkBoundary: KalNetworkExecutionBoundary,
    val documentationUrl: String,
) {
    init {
        require(id.isNotBlank())
        require(displayName.isNotBlank())
        require(authOptions.isNotEmpty())
        require(authOptions.count { it.recommended } <= 1) {
            "Only one auth option may be recommended per provider profile"
        }
    }

    val recommendedAuthMode: KalProviderAuthMode?
        get() = authOptions.firstOrNull { it.recommended }?.mode

    fun supports(mode: KalProviderAuthMode): Boolean = authOptions.any {
        it.mode == mode && it.maturity != KalConnectionMaturity.FALLBACK_ONLY
    }
}

/**
 * Provider-auth facts used by KAL. These are capability declarations, not embedded credentials.
 * Client IDs, tenant IDs, tokens and API keys are intentionally absent from source control.
 */
object KalProviderConnectionRegistry {
    val profiles: List<KalProviderConnectionProfile> = listOf(
        KalProviderConnectionProfile(
            id = "local-runtime",
            displayName = "Lokale Modelle",
            authOptions = listOf(
                KalProviderAuthOption(
                    mode = KalProviderAuthMode.LOCAL_RUNTIME,
                    maturity = KalConnectionMaturity.SUPPORTED,
                    recommended = true,
                    label = "Lokal ausführen",
                    note = "Kein Konto und kein Netzwerk erforderlich.",
                ),
            ),
            networkBoundary = KalNetworkExecutionBoundary.NONE,
            documentationUrl = "https://github.com/chekento/kosch-ai-android",
        ),
        KalProviderConnectionProfile(
            id = "openrouter",
            displayName = "OpenRouter",
            authOptions = listOf(
                KalProviderAuthOption(
                    mode = KalProviderAuthMode.OAUTH_PKCE,
                    maturity = KalConnectionMaturity.CONFIGURATION_REQUIRED,
                    recommended = true,
                    label = "Mit OpenRouter verbinden",
                    note = "OAuth/PKCE kann einen benutzerkontrollierten Provider-Key erzeugen; KAL benötigt dafür eine registrierte Client-/Redirect-Konfiguration.",
                ),
                KalProviderAuthOption(
                    mode = KalProviderAuthMode.API_KEY,
                    maturity = KalConnectionMaturity.SUPPORTED,
                    label = "OpenRouter API-Key",
                    note = "BYOK-Fallback für Nutzer, die ihren Key selbst verwalten möchten.",
                ),
            ),
            networkBoundary = KalNetworkExecutionBoundary.EXPLICIT_NETWORK_CONNECTOR,
            documentationUrl = "https://openrouter.ai/docs",
        ),
        KalProviderConnectionProfile(
            id = "gemini",
            aiProviderId = "gemini",
            displayName = "Gemini API",
            authOptions = listOf(
                KalProviderAuthOption(
                    mode = KalProviderAuthMode.OAUTH_USER,
                    maturity = KalConnectionMaturity.CONFIGURATION_REQUIRED,
                    recommended = true,
                    label = "Mit Google verbinden",
                    note = "Google OAuth ist für die Gemini API vorgesehen; KAL benötigt ein registriertes OAuth-Client-Profil und passende Scopes.",
                ),
                KalProviderAuthOption(
                    mode = KalProviderAuthMode.API_KEY,
                    maturity = KalConnectionMaturity.SUPPORTED,
                    label = "Gemini API-Key",
                    note = "Einfacher BYOK-Fallback.",
                ),
                KalProviderAuthOption(
                    mode = KalProviderAuthMode.APP_HANDOFF,
                    maturity = KalConnectionMaturity.FALLBACK_ONLY,
                    label = "Gemini App",
                    note = "Bewusste Übergabe an die installierte App, keine direkte KAL-API-Verbindung.",
                ),
            ),
            networkBoundary = KalNetworkExecutionBoundary.EXPLICIT_NETWORK_CONNECTOR,
            documentationUrl = "https://ai.google.dev/gemini-api/docs/oauth",
        ),
        KalProviderConnectionProfile(
            id = "huggingface",
            displayName = "Hugging Face",
            authOptions = listOf(
                KalProviderAuthOption(
                    mode = KalProviderAuthMode.OAUTH_PKCE,
                    maturity = KalConnectionMaturity.CONFIGURATION_REQUIRED,
                    recommended = true,
                    label = "Mit Hugging Face verbinden",
                    note = "Public OAuth Apps ohne Client-Secret und Authorization Code + PKCE sind für native Apps vorgesehen.",
                ),
                KalProviderAuthOption(
                    mode = KalProviderAuthMode.API_KEY,
                    maturity = KalConnectionMaturity.SUPPORTED,
                    label = "Hugging Face Token",
                    note = "Token-basierter Fallback für Inference-Zugriffe.",
                ),
            ),
            networkBoundary = KalNetworkExecutionBoundary.EXPLICIT_NETWORK_CONNECTOR,
            documentationUrl = "https://huggingface.co/docs/hub/oauth",
        ),
        KalProviderConnectionProfile(
            id = "azure-openai",
            displayName = "Azure OpenAI / Microsoft Foundry",
            authOptions = listOf(
                KalProviderAuthOption(
                    mode = KalProviderAuthMode.ENTRA_ID,
                    maturity = KalConnectionMaturity.CONFIGURATION_REQUIRED,
                    recommended = true,
                    label = "Mit Microsoft Entra ID verbinden",
                    note = "Kurzlebige Bearer-Tokens und RBAC statt dauerhaftem Service-Key.",
                ),
                KalProviderAuthOption(
                    mode = KalProviderAuthMode.API_KEY,
                    maturity = KalConnectionMaturity.SUPPORTED,
                    label = "Azure API-Key",
                    note = "Kompatibler Fallback für bestehende Deployments.",
                ),
                KalProviderAuthOption(
                    mode = KalProviderAuthMode.CUSTOM_ENDPOINT,
                    maturity = KalConnectionMaturity.SUPPORTED,
                    label = "Azure Endpoint / Deployment",
                    note = "Ressourcen-Endpoint und Deployment bleiben nutzer- bzw. organisationsspezifisch.",
                ),
            ),
            networkBoundary = KalNetworkExecutionBoundary.EXPLICIT_NETWORK_CONNECTOR,
            documentationUrl = "https://learn.microsoft.com/azure/ai-services/openai/how-to/managed-identity",
        ),
        KalProviderConnectionProfile(
            id = "openai",
            aiProviderId = "chatgpt",
            displayName = "OpenAI API",
            authOptions = listOf(
                KalProviderAuthOption(
                    mode = KalProviderAuthMode.API_KEY,
                    maturity = KalConnectionMaturity.SUPPORTED,
                    recommended = true,
                    label = "OpenAI API-Key",
                    note = "Direkte OpenAI-API-Verbindung; ChatGPT-App-Abos werden nicht als API-Credential behandelt.",
                ),
                KalProviderAuthOption(
                    mode = KalProviderAuthMode.APP_HANDOFF,
                    maturity = KalConnectionMaturity.FALLBACK_ONLY,
                    label = "ChatGPT App",
                    note = "Bewusste Android-Übergabe ohne direkte API-Verbindung.",
                ),
            ),
            networkBoundary = KalNetworkExecutionBoundary.EXPLICIT_NETWORK_CONNECTOR,
            documentationUrl = "https://platform.openai.com/docs",
        ),
        KalProviderConnectionProfile(
            id = "anthropic",
            aiProviderId = "claude",
            displayName = "Anthropic API",
            authOptions = listOf(
                KalProviderAuthOption(
                    mode = KalProviderAuthMode.API_KEY,
                    maturity = KalConnectionMaturity.SUPPORTED,
                    recommended = true,
                    label = "Anthropic API-Key",
                    note = "Direkter Claude-API-Zugang über einen Anthropic API-Key.",
                ),
                KalProviderAuthOption(
                    mode = KalProviderAuthMode.APP_HANDOFF,
                    maturity = KalConnectionMaturity.FALLBACK_ONLY,
                    label = "Claude App",
                    note = "Bewusste Android-Übergabe ohne direkte API-Verbindung.",
                ),
            ),
            networkBoundary = KalNetworkExecutionBoundary.EXPLICIT_NETWORK_CONNECTOR,
            documentationUrl = "https://docs.anthropic.com/",
        ),
        KalProviderConnectionProfile(
            id = "custom-openai-compatible",
            displayName = "OpenAI-kompatibler eigener Endpoint",
            authOptions = listOf(
                KalProviderAuthOption(
                    mode = KalProviderAuthMode.CUSTOM_ENDPOINT,
                    maturity = KalConnectionMaturity.SUPPORTED,
                    recommended = true,
                    label = "Eigenen Endpoint konfigurieren",
                    note = "Host, Modell-ID und Auth-Verfahren werden explizit vom Nutzer festgelegt; keine stillen Provider-Annahmen.",
                ),
                KalProviderAuthOption(
                    mode = KalProviderAuthMode.API_KEY,
                    maturity = KalConnectionMaturity.SUPPORTED,
                    label = "Optionaler API-Key",
                    note = "Nur wenn der konfigurierte Endpoint ihn tatsächlich verlangt.",
                ),
            ),
            networkBoundary = KalNetworkExecutionBoundary.EXPLICIT_NETWORK_CONNECTOR,
            documentationUrl = "https://github.com/chekento/kosch-ai-android",
        ),
    )

    private val byId = profiles.associateBy(KalProviderConnectionProfile::id)
    private val byAiProviderId = profiles.mapNotNull { profile ->
        profile.aiProviderId?.let { it to profile }
    }.toMap()

    fun profile(id: String): KalProviderConnectionProfile? = byId[id]

    fun profileForAiProvider(aiProviderId: String): KalProviderConnectionProfile? = byAiProviderId[aiProviderId]
}

data class KalPkceMaterial(
    val codeVerifier: String,
    val codeChallenge: String,
    val state: String,
)

/** RFC 7636 PKCE material for public/native OAuth clients. No client secret is generated or expected. */
object KalPkce {
    private val verifierPattern = Regex("^[A-Za-z0-9._~-]{43,128}$")

    fun create(random: SecureRandom = SecureRandom()): KalPkceMaterial {
        val verifierBytes = ByteArray(64).also(random::nextBytes)
        val stateBytes = ByteArray(32).also(random::nextBytes)
        val verifier = base64Url(verifierBytes)
        check(verifierPattern.matches(verifier))
        return KalPkceMaterial(
            codeVerifier = verifier,
            codeChallenge = challengeFor(verifier),
            state = base64Url(stateBytes),
        )
    }

    fun challengeFor(codeVerifier: String): String {
        require(verifierPattern.matches(codeVerifier)) { "Invalid PKCE code verifier" }
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(codeVerifier.toByteArray(Charsets.US_ASCII))
        return base64Url(digest)
    }

    private fun base64Url(bytes: ByteArray): String = Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(bytes)
}
