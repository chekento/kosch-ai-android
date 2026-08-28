package cloud.kosch.aiandroid.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenRouterOAuthProtocolTest {
    @Test
    fun authorizationUrlUsesLoopbackPkceS256() {
        val url = OpenRouterOAuthProtocol.authorizationUrl(
            callbackUrl = "http://127.0.0.1:51234/kal/oauth/openrouter/random-state",
            codeChallenge = "abc_DEF-123~challenge",
        )

        assertTrue(url.startsWith("https://openrouter.ai/auth?"))
        assertTrue(url.contains("callback_url=http%3A%2F%2F127.0.0.1%3A51234%2Fkal%2Foauth%2Fopenrouter%2Frandom-state"))
        assertTrue(url.contains("code_challenge="))
        assertTrue(url.endsWith("code_challenge_method=S256"))
        assertFalse(url.contains("client_secret", ignoreCase = true))
    }

    @Test(expected = IllegalArgumentException::class)
    fun authorizationUrlRejectsRemoteCallback() {
        OpenRouterOAuthProtocol.authorizationUrl(
            callbackUrl = "https://attacker.example/callback",
            codeChallenge = "challenge",
        )
    }

    @Test
    fun callbackAcceptsExactRandomPathOnly() {
        val expectedPath = "/kal/oauth/openrouter/random-state"
        assertEquals(
            "auth_code_123",
            OpenRouterOAuthProtocol.codeFromRequestLine(
                "GET $expectedPath?code=auth_code_123 HTTP/1.1",
                expectedPath,
            ),
        )
        assertNull(
            OpenRouterOAuthProtocol.codeFromRequestLine(
                "GET /kal/oauth/openrouter/wrong?code=auth_code_123 HTTP/1.1",
                expectedPath,
            ),
        )
    }

    @Test
    fun callbackDecodesCodeAndRejectsOtherMethods() {
        val path = "/kal/oauth/openrouter/state"
        assertEquals(
            "code/value+one",
            OpenRouterOAuthProtocol.codeFromRequestLine(
                "GET $path?code=code%2Fvalue%2Bone&other=x HTTP/1.1",
                path,
            ),
        )
        assertNull(
            OpenRouterOAuthProtocol.codeFromRequestLine(
                "POST $path?code=secret HTTP/1.1",
                path,
            ),
        )
    }
}
