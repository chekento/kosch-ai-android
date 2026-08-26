package cloud.kosch.aiandroid.security.network

/**
 * Traffic-inert adapter contract for the future gomobile SocketProtector callback.
 *
 * The production Android port will delegate [AndroidVpnProtectPort.protect] to
 * VpnService.protect(fd). This class intentionally has no VpnService dependency and opens no
 * sockets; it only defines the fail-closed type/range/exception boundary.
 */
fun interface AndroidVpnProtectPort {
    fun protect(fd: Int): Boolean
}

class StrictVpnSocketProtectorBridge(
    private val androidPort: AndroidVpnProtectPort,
) {
    fun protect(fd: Long): Boolean {
        if (fd < 0L || fd > Int.MAX_VALUE.toLong()) return false
        return runCatching { androidPort.protect(fd.toInt()) }.getOrDefault(false)
    }
}
