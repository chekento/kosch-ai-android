package cloud.kosch.aiandroid.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KalProviderConnectionsTest {
    @Test
    fun openRouterPrefersPkceWithoutEmbeddingAnyCredential() {
        val profile = requireNotNull(KalProviderConnectionRegistry.profile("openrouter"))

        assertEquals(KalProviderAuthMode.OAUTH_PKCE, profile.recommendedAuthMode)
        assertTrue(profile.supports(KalProviderAuthMode.OAUTH_PKCE))
        assertTrue(profile.supports(KalProviderAuthMode.API_KEY))
        assertEquals(
            KalNetworkExecutionBoundary.EXPLICIT_NETWORK_CONNECTOR,
            profile.networkBoundary,
        )
        assertFalse(profile.toString().contains("client_secret", ignoreCase = true))
    }

    @Test
    fun geminiAndHuggingFaceExposeOAuthWhileOpenAiAndAnthropicDoNotClaimIt() {
        val gemini = requireNotNull(KalProviderConnectionRegistry.profile("gemini"))
        val huggingFace = requireNotNull(KalProviderConnectionRegistry.profile("huggingface"))
        val openAi = requireNotNull(KalProviderConnectionRegistry.profile("openai"))
        val anthropic = requireNotNull(KalProviderConnectionRegistry.profile("anthropic"))

        assertTrue(gemini.supports(KalProviderAuthMode.OAUTH_USER))
        assertTrue(huggingFace.supports(KalProviderAuthMode.OAUTH_PKCE))
        assertFalse(openAi.supports(KalProviderAuthMode.OAUTH_USER))
        assertFalse(openAi.supports(KalProviderAuthMode.OAUTH_PKCE))
        assertFalse(anthropic.supports(KalProviderAuthMode.OAUTH_USER))
        assertFalse(anthropic.supports(KalProviderAuthMode.OAUTH_PKCE))
        assertTrue(openAi.supports(KalProviderAuthMode.API_KEY))
        assertTrue(anthropic.supports(KalProviderAuthMode.API_KEY))
    }

    @Test
    fun azurePrefersShortLivedIdentityRoute() {
        val profile = requireNotNull(KalProviderConnectionRegistry.profile("azure-openai"))

        assertEquals(KalProviderAuthMode.ENTRA_ID, profile.recommendedAuthMode)
        assertTrue(profile.supports(KalProviderAuthMode.API_KEY))
        assertTrue(profile.supports(KalProviderAuthMode.CUSTOM_ENDPOINT))
    }

    @Test
    fun existingAiProvidersCanResolveTheirConnectionProfiles() {
        assertEquals(
            "openai",
            KalProviderConnectionRegistry.profileForAiProvider("chatgpt")?.id,
        )
        assertEquals(
            "anthropic",
            KalProviderConnectionRegistry.profileForAiProvider("claude")?.id,
        )
        assertEquals(
            "gemini",
            KalProviderConnectionRegistry.profileForAiProvider("gemini")?.id,
        )
    }

    @Test
    fun pkceMaterialUsesS256AndFreshState() {
        val first = KalPkce.create()
        val second = KalPkce.create()

        assertTrue(first.codeVerifier.length in 43..128)
        assertTrue(first.codeChallenge.length >= 43)
        assertTrue(first.state.length >= 43)
        assertEquals(KalPkce.challengeFor(first.codeVerifier), first.codeChallenge)
        assertNotEquals(first.codeVerifier, second.codeVerifier)
        assertNotEquals(first.state, second.state)
        assertFalse(first.codeVerifier.contains("="))
        assertFalse(first.codeChallenge.contains("="))
        assertFalse(first.state.contains("="))
    }

    @Test(expected = IllegalArgumentException::class)
    fun pkceRejectsTooShortVerifier() {
        KalPkce.challengeFor("too-short")
    }

    @Test
    fun localRuntimeNeverRequiresNetworkBoundary() {
        val local = requireNotNull(KalProviderConnectionRegistry.profile("local-runtime"))

        assertEquals(KalNetworkExecutionBoundary.NONE, local.networkBoundary)
        assertEquals(KalProviderAuthMode.LOCAL_RUNTIME, local.recommendedAuthMode)
        assertNotNull(local.authOptions.singleOrNull())
    }
}
