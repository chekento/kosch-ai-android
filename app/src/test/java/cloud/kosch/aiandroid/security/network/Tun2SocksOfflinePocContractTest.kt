package cloud.kosch.aiandroid.security.network

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Tun2SocksOfflinePocContractTest {
    private val digest = "a".repeat(64)

    private fun validEvidence() = Tun2SocksOfflinePocEvidence(
        upstreamVersion = Tun2SocksOfflinePocContract.UPSTREAM_VERSION,
        upstreamCommit = Tun2SocksOfflinePocContract.UPSTREAM_COMMIT,
        goVersion = Tun2SocksOfflinePocContract.GO_VERSION,
        xMobileVersion = Tun2SocksOfflinePocContract.X_MOBILE_VERSION,
        xMobileCommit = Tun2SocksOfflinePocContract.X_MOBILE_COMMIT,
        gomobileModuleVersion = Tun2SocksOfflinePocContract.X_MOBILE_VERSION,
        gobindModuleVersion = Tun2SocksOfflinePocContract.X_MOBILE_VERSION,
        gomobileSha256 = digest,
        gobindSha256 = digest,
        patchSha256 = digest,
        artifactSha256 = digest,
        androidMinApi = Tun2SocksOfflinePocContract.ANDROID_MIN_API,
        boundPackage = Tun2SocksOfflinePocContract.BOUND_PACKAGE,
        engineJavaApiExposed = false,
        tunFdDuplicated = true,
        panicCrossBoundary = false,
        fixedDirectEgress = true,
        proxyConfigurationExposed = false,
        networkDuringBuild = false,
        runtimeIntegrated = false,
        vpnEstablished = false,
        internetPermissionAdded = false,
        recoverableStartApi = true,
        mandatoryProtectHook = true,
    )

    @Test
    fun completeOfflineEvidence_allowsArtifactButNeverActivation() {
        val result = Tun2SocksOfflinePocContract.evaluate(validEvidence())

        assertTrue(result.artifactEligible)
        assertFalse(result.activationEligible)
    }

    @Test
    fun sourceOrToolchainDrift_blocksArtifact() {
        val result = Tun2SocksOfflinePocContract.evaluate(
            validEvidence().copy(
                upstreamCommit = "0".repeat(40),
                goVersion = "go1.99.0",
                xMobileVersion = "v0.0.0-bad",
                xMobileCommit = "0".repeat(40),
                gomobileModuleVersion = "v0.0.0-other",
                gobindModuleVersion = "v0.0.0-other",
            ),
        )

        assertFalse(result.artifactEligible)
        assertTrue(result.blockers.contains(Tun2SocksOfflinePocBlocker.COMMIT_DRIFT))
        assertTrue(result.blockers.contains(Tun2SocksOfflinePocBlocker.GO_TOOLCHAIN_DRIFT))
        assertTrue(result.blockers.contains(Tun2SocksOfflinePocBlocker.X_MOBILE_TOOLCHAIN_DRIFT))
        assertTrue(result.blockers.contains(Tun2SocksOfflinePocBlocker.X_MOBILE_COMMIT_DRIFT))
        assertTrue(result.blockers.contains(Tun2SocksOfflinePocBlocker.GOMOBILE_MODULE_DRIFT))
        assertTrue(result.blockers.contains(Tun2SocksOfflinePocBlocker.GOBIND_MODULE_DRIFT))
    }

    @Test
    fun unsafeWrapperOrFdOwnership_blocksArtifact() {
        val result = Tun2SocksOfflinePocContract.evaluate(
            validEvidence().copy(
                boundPackage = "github.com/xjasonlyu/tun2socks/v2/engine",
                engineJavaApiExposed = true,
                tunFdDuplicated = false,
                panicCrossBoundary = true,
                fixedDirectEgress = false,
                proxyConfigurationExposed = true,
            ),
        )

        assertFalse(result.artifactEligible)
        assertTrue(result.blockers.contains(Tun2SocksOfflinePocBlocker.BOUND_PACKAGE_DRIFT))
        assertTrue(result.blockers.contains(Tun2SocksOfflinePocBlocker.UNSAFE_ENGINE_API_EXPOSED))
        assertTrue(result.blockers.contains(Tun2SocksOfflinePocBlocker.TUN_FD_OWNERSHIP_UNSAFE))
        assertTrue(result.blockers.contains(Tun2SocksOfflinePocBlocker.PANIC_BOUNDARY_UNSAFE))
        assertTrue(result.blockers.contains(Tun2SocksOfflinePocBlocker.DIRECT_EGRESS_NOT_FIXED))
        assertTrue(result.blockers.contains(Tun2SocksOfflinePocBlocker.PROXY_CONFIGURATION_EXPOSED))
    }

    @Test
    fun onlineBuildOrRuntimeActivation_blocksArtifact() {
        val result = Tun2SocksOfflinePocContract.evaluate(
            validEvidence().copy(
                networkDuringBuild = true,
                runtimeIntegrated = true,
                vpnEstablished = true,
                internetPermissionAdded = true,
            ),
        )

        assertFalse(result.artifactEligible)
        assertTrue(result.blockers.contains(Tun2SocksOfflinePocBlocker.BUILD_WAS_NOT_OFFLINE))
        assertTrue(result.blockers.contains(Tun2SocksOfflinePocBlocker.RUNTIME_INTEGRATION_FORBIDDEN))
        assertTrue(result.blockers.contains(Tun2SocksOfflinePocBlocker.VPN_ESTABLISH_FORBIDDEN))
        assertTrue(result.blockers.contains(Tun2SocksOfflinePocBlocker.INTERNET_PERMISSION_FORBIDDEN))
    }

    @Test
    fun missingSafeLifecycleOrProtectHook_blocksArtifact() {
        val result = Tun2SocksOfflinePocContract.evaluate(
            validEvidence().copy(
                recoverableStartApi = false,
                mandatoryProtectHook = false,
            ),
        )

        assertFalse(result.artifactEligible)
        assertTrue(result.blockers.contains(Tun2SocksOfflinePocBlocker.RECOVERABLE_START_MISSING))
        assertTrue(result.blockers.contains(Tun2SocksOfflinePocBlocker.MANDATORY_PROTECT_MISSING))
    }

    @Test
    fun malformedFingerprints_blockArtifact() {
        val result = Tun2SocksOfflinePocContract.evaluate(
            validEvidence().copy(
                gomobileSha256 = "not-a-digest",
                gobindSha256 = "also-not-a-digest",
                patchSha256 = "",
                artifactSha256 = "ABCDEF",
            ),
        )

        assertFalse(result.artifactEligible)
        assertTrue(result.blockers.contains(Tun2SocksOfflinePocBlocker.GOMOBILE_FINGERPRINT_MISSING))
        assertTrue(result.blockers.contains(Tun2SocksOfflinePocBlocker.GOBIND_FINGERPRINT_MISSING))
        assertTrue(result.blockers.contains(Tun2SocksOfflinePocBlocker.PATCH_FINGERPRINT_MISSING))
        assertTrue(result.blockers.contains(Tun2SocksOfflinePocBlocker.ARTIFACT_FINGERPRINT_MISSING))
    }

    @Test
    fun offlineArtifactEvidence_doesNotOverrideForwarderActivationGate() {
        val artifact = Tun2SocksOfflinePocContract.evaluate(validEvidence())
        val forwarder = ForwarderCandidateEvaluation.evaluate(
            ForwarderCandidateCatalog.TUN2SOCKS_2_7_0,
            ForwarderUseCase.N2_DIRECT,
        )

        assertTrue(artifact.artifactEligible)
        assertFalse(artifact.activationEligible)
        assertFalse(forwarder.activationEligible)
    }
}
