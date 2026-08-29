package cloud.kosch.aiandroid.ai

import org.junit.Assert.assertEquals
import org.junit.Test

class AiProviderRoutePolicyTest {
    @Test
    fun installedProviderAlwaysWinsOverStoreFallback() {
        val provider = AiProviderRegistry.providers.first { it.id == "chatgpt" }

        assertEquals(
            AiProviderDestination.Installed("personal:com.openai.chatgpt/MainActivity"),
            AiProviderRoutePolicy.destination(
                provider = provider,
                installedAppKey = "personal:com.openai.chatgpt/MainActivity",
            ),
        )
    }

    @Test
    fun missingKnownPlayAppRoutesToPlayStore() {
        val provider = AiProviderRegistry.providers.first { it.id == "gemini" }

        assertEquals(
            AiProviderDestination.PlayStore("com.google.android.apps.bard"),
            AiProviderRoutePolicy.destination(provider, installedAppKey = null),
        )
    }

    @Test
    fun sourceOnlyProviderRoutesToWebInsteadOfGuessingStorePackage() {
        val provider = AiProviderRegistry.providers.first { it.id == "chatterui" }

        assertEquals(
            AiProviderDestination.Web("https://github.com/Vali-98/ChatterUI"),
            AiProviderRoutePolicy.destination(provider, installedAppKey = null),
        )
    }

    @Test
    fun metaAiUsesCurrentOfficialPlayPackage() {
        val provider = AiProviderRegistry.providers.first { it.id == "meta-ai" }
        assertEquals("com.facebook.stella", provider.playStorePackageName)
        assertEquals(setOf("com.facebook.stella"), provider.packageHints)
    }
}
