package cloud.kosch.aiandroid.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProviderEndpointPolicyTest {
    @Test
    fun `remote endpoints require https`() {
        assertTrue(ProviderEndpointPolicy.validate("https://api.example.com/v1").allowed)
        assertFalse(ProviderEndpointPolicy.validate("http://api.example.com/v1").allowed)
    }

    @Test
    fun `credentials embedded in endpoint are rejected`() {
        assertFalse(ProviderEndpointPolicy.validate("https://token@example.com/v1").allowed)
    }

    @Test
    fun `loopback http requires a deliberate local opt in`() {
        assertFalse(ProviderEndpointPolicy.validate("http://127.0.0.1:11434/v1").allowed)
        assertTrue(
            ProviderEndpointPolicy.validate(
                "http://127.0.0.1:11434/v1",
                allowLoopbackHttp = true,
            ).allowed,
        )
    }
}
