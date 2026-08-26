package cloud.kosch.aiandroid.security.network

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StrictVpnSocketProtectorBridgeTest {
    @Test
    fun validFd_isDelegatedExactlyOnce() {
        var seen: Int? = null
        val bridge = StrictVpnSocketProtectorBridge { fd ->
            seen = fd
            true
        }

        assertTrue(bridge.protect(42L))
        assertTrue(seen == 42)
    }

    @Test
    fun negativeOrOversizedFd_isRejectedWithoutDelegation() {
        var calls = 0
        val bridge = StrictVpnSocketProtectorBridge {
            calls += 1
            true
        }

        assertFalse(bridge.protect(-1L))
        assertFalse(bridge.protect(Int.MAX_VALUE.toLong() + 1L))
        assertTrue(calls == 0)
    }

    @Test
    fun androidProtectFalse_failsClosed() {
        val bridge = StrictVpnSocketProtectorBridge { false }

        assertFalse(bridge.protect(7L))
    }

    @Test
    fun androidProtectException_failsClosed() {
        val bridge = StrictVpnSocketProtectorBridge {
            throw IllegalStateException("simulated VpnService failure")
        }

        assertFalse(bridge.protect(7L))
    }
}
