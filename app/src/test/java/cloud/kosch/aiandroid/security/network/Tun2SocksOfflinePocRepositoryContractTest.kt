package cloud.kosch.aiandroid.security.network

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Tun2SocksOfflinePocRepositoryContractTest {
    private fun repositoryRoot(): File = generateSequence(File(System.getProperty("user.dir")).absoluteFile) { it.parentFile }
        .firstOrNull { File(it, "settings.gradle.kts").isFile }
        ?: error("Repository root not found from ${System.getProperty("user.dir")}")

    private fun pocFile(relativePath: String): String {
        val file = File(repositoryRoot(), "tools/n2-tun2socks-poc/$relativePath")
        assertTrue("Missing POC file: ${file.path}", file.isFile)
        return file.readText()
    }

    @Test
    fun sourceLock_isExactAndRuntimeInert() {
        val lock = pocFile("source.lock")

        assertTrue(lock.contains("version=${Tun2SocksOfflinePocContract.UPSTREAM_VERSION}"))
        assertTrue(lock.contains("commit=${Tun2SocksOfflinePocContract.UPSTREAM_COMMIT}"))
        assertTrue(lock.contains("go_version=${Tun2SocksOfflinePocContract.GO_VERSION}"))
        assertTrue(lock.contains("android_min_api=${Tun2SocksOfflinePocContract.ANDROID_MIN_API}"))
        assertTrue(lock.contains("runtime_integration=false"))
        assertTrue(lock.contains("network_during_build=false"))
        assertFalse(lock.contains("latest"))
    }

    @Test
    fun patch_exposesRecoverableStartAndMandatoryProtect_withoutVpnActivation() {
        val patch = pocFile("patches/0001-kosch-safe-mobile-bridge.patch")

        assertTrue(patch.contains("func StartSafe() error"))
        assertTrue(patch.contains("func StopSafe() error"))
        assertTrue(patch.contains("SetMandatorySockOpt"))
        assertTrue(patch.contains("protector.Protect(int64(fd))"))
        assertTrue(patch.contains("registerKoSchSocketProtector()"))
        assertFalse(patch.contains("Builder.establish()"))
        assertFalse(patch.contains("android.permission.INTERNET"))
    }

    @Test
    fun builder_isPinnedOfflineAndProducesFingerprintedEvidence() {
        val script = pocFile("build-pinned-aar.sh")

        assertTrue(script.contains(Tun2SocksOfflinePocContract.UPSTREAM_COMMIT))
        assertTrue(script.contains("GOPROXY=off"))
        assertTrue(script.contains("GOSUMDB=off"))
        assertTrue(script.contains("apply --check"))
        assertTrue(script.contains("gomobile bind"))
        assertTrue(script.contains("ANDROID_API=\"29\""))
        assertTrue(script.contains("-androidapi"))
        assertTrue(script.contains("gomobile_sha256="))
        assertTrue(script.contains("patch_sha256="))
        assertTrue(script.contains("artifact_sha256="))
        assertTrue(script.contains("runtime_integrated=false"))
        assertTrue(script.contains("vpn_established=false"))
        assertTrue(script.contains("internet_permission_added=false"))
    }
}
