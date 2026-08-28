package cloud.kosch.aiandroid.ai

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KalCloudAccessPolicyTest {
    @Test
    fun cloudIsDeniedByDefaultEvenWhenProviderIsConnected() {
        val decision = KalCloudAccessPolicy.evaluate(
            mode = KalCloudAccessMode.OFF,
            request = KalCloudRequest(
                providerId = "openrouter",
                origin = KalCloudRequestOrigin.USER_ACTION,
                providerConnected = true,
                containsUserContent = true,
            ),
        )

        assertFalse(decision.allowed)
        assertFalse(decision.requiresContentDisclosure)
    }

    @Test
    fun backgroundProviderTrafficIsAlwaysDenied() {
        val decision = KalCloudAccessPolicy.evaluate(
            mode = KalCloudAccessMode.CONNECTED_PROVIDERS_ONLY,
            request = KalCloudRequest(
                providerId = "openrouter",
                origin = KalCloudRequestOrigin.BACKGROUND,
                providerConnected = true,
                containsUserContent = false,
            ),
        )

        assertFalse(decision.allowed)
    }

    @Test
    fun unconnectedProviderIsDenied() {
        val decision = KalCloudAccessPolicy.evaluate(
            mode = KalCloudAccessMode.CONNECTED_PROVIDERS_ONLY,
            request = KalCloudRequest(
                providerId = "gemini",
                origin = KalCloudRequestOrigin.USER_ACTION,
                providerConnected = false,
                containsUserContent = true,
            ),
        )

        assertFalse(decision.allowed)
    }

    @Test
    fun connectedForegroundProviderCanRunAndContentRequiresDisclosure() {
        val decision = KalCloudAccessPolicy.evaluate(
            mode = KalCloudAccessMode.CONNECTED_PROVIDERS_ONLY,
            request = KalCloudRequest(
                providerId = "openrouter",
                origin = KalCloudRequestOrigin.ASSISTANT_CONFIRMED_ACTION,
                providerConnected = true,
                containsUserContent = true,
            ),
        )

        assertTrue(decision.allowed)
        assertTrue(decision.requiresContentDisclosure)
    }

    @Test
    fun localRuntimeWorksWhenCloudIsOff() {
        val decision = KalCloudAccessPolicy.evaluate(
            mode = KalCloudAccessMode.OFF,
            request = KalCloudRequest(
                providerId = "local-runtime",
                origin = KalCloudRequestOrigin.BACKGROUND,
                providerConnected = false,
                containsUserContent = true,
            ),
        )

        assertTrue(decision.allowed)
        assertFalse(decision.requiresContentDisclosure)
    }

    @Test
    fun unknownProviderFailsClosed() {
        val decision = KalCloudAccessPolicy.evaluate(
            mode = KalCloudAccessMode.CONNECTED_PROVIDERS_ONLY,
            request = KalCloudRequest(
                providerId = "not-real",
                origin = KalCloudRequestOrigin.USER_ACTION,
                providerConnected = true,
                containsUserContent = false,
            ),
        )

        assertFalse(decision.allowed)
    }
}
