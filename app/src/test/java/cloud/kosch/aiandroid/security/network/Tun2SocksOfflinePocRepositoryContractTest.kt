package cloud.kosch.aiandroid.security.network

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Tun2SocksOfflinePocRepositoryContractTest {
    private fun repositoryRoot(): File = generateSequence(File(System.getProperty("user.dir")).absoluteFile) { it.parentFile }
        .firstOrNull { File(it, "settings.gradle.kts").isFile }
        ?: error("Repository root not found from ${System.getProperty("user.dir")}")

    private fun repositoryFile(relativePath: String): String {
        val file = File(repositoryRoot(), relativePath)
        assertTrue("Missing repository file: ${file.path}", file.isFile)
        return file.readText()
    }

    private fun pocFile(relativePath: String): String = repositoryFile("tools/n2-tun2socks-poc/$relativePath")

    @Test
    fun sourceLock_isExactAndRuntimeInert() {
        val lock = pocFile("source.lock")

        assertTrue(lock.contains("version=${Tun2SocksOfflinePocContract.UPSTREAM_VERSION}"))
        assertTrue(lock.contains("commit=${Tun2SocksOfflinePocContract.UPSTREAM_COMMIT}"))
        assertTrue(lock.contains("go_version=${Tun2SocksOfflinePocContract.GO_VERSION}"))
        assertTrue(lock.contains("go_linux_amd64_sha256=2b2cfc7148493da5e73981bffbf3353af381d5f93e789c82c79aff64962eb556"))
        assertTrue(lock.contains("x_mobile_version=${Tun2SocksOfflinePocContract.X_MOBILE_VERSION}"))
        assertTrue(lock.contains("x_mobile_commit=${Tun2SocksOfflinePocContract.X_MOBILE_COMMIT}"))
        assertTrue(lock.contains("ndk_version=${Tun2SocksOfflinePocContract.NDK_VERSION}"))
        assertTrue(lock.contains("android_min_api=${Tun2SocksOfflinePocContract.ANDROID_MIN_API}"))
        assertTrue(lock.contains("runtime_integration=false"))
        assertTrue(lock.contains("network_during_build=false"))
        assertFalse(lock.contains("latest"))
    }

    @Test
    fun patch_bindsSafeFacadeAndDuplicatesTunFd() {
        val patch = pocFile("patches/0001-kosch-safe-mobile-bridge.patch")

        assertTrue(patch.contains("package koschmobile"))
        assertTrue(patch.contains("func Start(tunFD int64, mtu int64, protector SocketProtector) (result string)"))
        assertTrue(patch.contains("func Stop() (result string)"))
        assertTrue(patch.contains("func StartKoSchDirect"))
        assertTrue(patch.contains("func StopKoSchDirect"))
        assertTrue(patch.contains("unix.Dup(originalFD)"))
        assertTrue(patch.contains("Proxy:                    \"direct://\""))
        assertTrue(patch.contains("SetMandatorySockOpt"))
        assertTrue(patch.contains("protector(int64(fd))"))
        assertTrue(patch.contains("KoSch socket protector is required"))
        assertTrue(patch.contains("recover()"))
        assertFalse(patch.contains("func StartSafe() error"))
        assertFalse(patch.contains("func StopSafe() error"))
        assertFalse(patch.contains("Builder.establish()"))
        assertFalse(patch.contains("android.permission.INTERNET"))
    }

    @Test
    fun builder_isPinnedOfflineAndBindsOnlyFacade() {
        val script = pocFile("build-pinned-aar.sh")

        assertTrue(script.contains(Tun2SocksOfflinePocContract.UPSTREAM_COMMIT))
        assertTrue(script.contains(Tun2SocksOfflinePocContract.X_MOBILE_VERSION))
        assertTrue(script.contains(Tun2SocksOfflinePocContract.X_MOBILE_COMMIT))
        assertTrue(script.contains(Tun2SocksOfflinePocContract.NDK_VERSION))
        assertTrue(script.contains("ANDROID_NDK_HOME"))
        assertTrue(script.contains("Pkg.Revision"))
        assertTrue(script.contains("X_MOBILE_REV_SUFFIX"))
        assertTrue(script.contains("x_mobile_commit="))
        assertTrue(script.contains("ndk_version="))
        assertTrue(script.contains("GOPROXY=off"))
        assertTrue(script.contains("GOSUMDB=off"))
        assertTrue(script.contains("apply --check"))
        assertTrue(script.contains("for tool in git go gomobile gobind sha256sum unzip jar awk grep"))
        assertTrue(script.contains("go mod edit -require="))
        assertTrue(script.contains("go list -mod=mod golang.org/x/mobile/bind"))
        assertTrue(script.contains("go test -run '^$' ./engine ./koschmobile"))
        assertTrue(script.contains("gomobile bind"))
        assertTrue(script.contains("./koschmobile"))
        assertTrue(script.contains("grep -Ev '^(koschmobile/|go/)'"))
        assertTrue(script.contains("unexpected generated Java API classes"))
        assertTrue(script.contains("ANDROID_API=\"29\""))
        assertTrue(script.contains("bound_package="))
        assertTrue(script.contains("engine_java_api_exposed=false"))
        assertTrue(script.contains("tun_fd_duplicated=true"))
        assertTrue(script.contains("panic_cross_boundary=false"))
        assertTrue(script.contains("fixed_direct_egress=true"))
        assertTrue(script.contains("proxy_configuration_exposed=false"))
        assertTrue(script.contains("gomobile_module_version="))
        assertTrue(script.contains("gobind_module_version="))
        assertTrue(script.contains("gomobile_sha256="))
        assertTrue(script.contains("gobind_sha256="))
        assertTrue(script.contains("patch_sha256="))
        assertTrue(script.contains("artifact_sha256="))
        assertTrue(script.contains("runtime_integrated=false"))
        assertTrue(script.contains("vpn_established=false"))
        assertTrue(script.contains("internet_permission_added=false"))
    }

    @Test
    fun evidenceWorkflow_isManualAndSeparatesOnlinePreparationFromOfflineBuild() {
        val workflow = repositoryFile(".github/workflows/n2-forwarder-poc.yml")

        assertTrue(workflow.contains("workflow_dispatch:"))
        assertFalse(workflow.contains("pull_request:"))
        assertFalse(workflow.contains("push:"))
        assertTrue(workflow.contains(Tun2SocksOfflinePocContract.UPSTREAM_COMMIT))
        assertTrue(workflow.contains(Tun2SocksOfflinePocContract.X_MOBILE_VERSION))
        assertTrue(workflow.contains(Tun2SocksOfflinePocContract.NDK_VERSION))
        assertTrue(workflow.contains("GO_LINUX_AMD64_SHA256"))
        assertTrue(workflow.contains("Prepare networked dependency cache"))
        assertTrue(workflow.contains("Run pinned builder offline"))
        assertTrue(workflow.contains("GOPROXY: off"))
        assertTrue(workflow.contains("GOSUMDB: off"))
        assertTrue(workflow.contains("runtime_integrated=false"))
        assertTrue(workflow.contains("vpn_established=false"))
        assertTrue(workflow.contains("internet_permission_added=false"))
    }
}
