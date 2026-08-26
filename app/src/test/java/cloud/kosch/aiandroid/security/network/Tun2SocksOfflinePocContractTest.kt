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
        gomobileSha256 = digest,
        patchSha256 = digest,
        artifactSha256 = digest,
        androidMinApi = Tun2SocksOfflinePocContract.ANDROID_MIN_API,
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
            ),
        )

        assertFalse(result.artifactEligible)
        assertTrue(result.blockers.contains(Tun2SocksOfflinePocBlocker.COMMIT_DRIFT))
        assertTrue(result.blockers.contains(Tun2SocksOfflinePocBlocker.GO_TOOLCHAIN_DRIFT))
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
                patchSha256 = "",
                artifactSha256 = "ABCDEF",
            ),
        )

        assertFalse(result.artifactEligible)
        assertTrue(result.blockers.contains(Tun2SocksOfflinePocBlocker.GOMOBILE_FINGERPRINT_MISSING))
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
