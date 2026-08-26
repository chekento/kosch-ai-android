package cloud.kosch.aiandroid.security.network

/**
 * Evidence boundary for the isolated tun2socks Android packaging POC.
 *
 * Passing this gate proves only that a reproducible, traffic-inert AAR artifact was produced from
 * the pinned source with the KoSch safety bridge present. It does not authorize VpnService
 * activation and cannot make [ForwarderCandidateEvaluation] activation-eligible.
 */
object Tun2SocksOfflinePocContract {
    const val UPSTREAM_VERSION = "v2.7.0"
    const val UPSTREAM_COMMIT = "8dda19e8e4613e014f0b12f3e624fdff5e5f23b3"
    const val GO_VERSION = "go1.26.3"
    const val ANDROID_MIN_API = 29

    private val sha256 = Regex("[0-9a-f]{64}")

    fun evaluate(evidence: Tun2SocksOfflinePocEvidence): Tun2SocksOfflinePocResult {
        val blockers = buildList {
            if (evidence.upstreamVersion != UPSTREAM_VERSION) add(Tun2SocksOfflinePocBlocker.VERSION_DRIFT)
            if (evidence.upstreamCommit != UPSTREAM_COMMIT) add(Tun2SocksOfflinePocBlocker.COMMIT_DRIFT)
            if (evidence.goVersion != GO_VERSION) add(Tun2SocksOfflinePocBlocker.GO_TOOLCHAIN_DRIFT)
            if (evidence.androidMinApi != ANDROID_MIN_API) add(Tun2SocksOfflinePocBlocker.ANDROID_API_DRIFT)
            if (!sha256.matches(evidence.gomobileSha256)) add(Tun2SocksOfflinePocBlocker.GOMOBILE_FINGERPRINT_MISSING)
            if (!sha256.matches(evidence.patchSha256)) add(Tun2SocksOfflinePocBlocker.PATCH_FINGERPRINT_MISSING)
            if (!sha256.matches(evidence.artifactSha256)) add(Tun2SocksOfflinePocBlocker.ARTIFACT_FINGERPRINT_MISSING)
            if (evidence.networkDuringBuild) add(Tun2SocksOfflinePocBlocker.BUILD_WAS_NOT_OFFLINE)
            if (evidence.runtimeIntegrated) add(Tun2SocksOfflinePocBlocker.RUNTIME_INTEGRATION_FORBIDDEN)
            if (evidence.vpnEstablished) add(Tun2SocksOfflinePocBlocker.VPN_ESTABLISH_FORBIDDEN)
            if (evidence.internetPermissionAdded) add(Tun2SocksOfflinePocBlocker.INTERNET_PERMISSION_FORBIDDEN)
            if (!evidence.recoverableStartApi) add(Tun2SocksOfflinePocBlocker.RECOVERABLE_START_MISSING)
            if (!evidence.mandatoryProtectHook) add(Tun2SocksOfflinePocBlocker.MANDATORY_PROTECT_MISSING)
        }
        return Tun2SocksOfflinePocResult(blockers)
    }
}

data class Tun2SocksOfflinePocEvidence(
    val upstreamVersion: String,
    val upstreamCommit: String,
    val goVersion: String,
    val gomobileSha256: String,
    val patchSha256: String,
    val artifactSha256: String,
    val androidMinApi: Int,
    val networkDuringBuild: Boolean,
    val runtimeIntegrated: Boolean,
    val vpnEstablished: Boolean,
    val internetPermissionAdded: Boolean,
    val recoverableStartApi: Boolean,
    val mandatoryProtectHook: Boolean,
)

enum class Tun2SocksOfflinePocBlocker {
    VERSION_DRIFT,
    COMMIT_DRIFT,
    GO_TOOLCHAIN_DRIFT,
    ANDROID_API_DRIFT,
    GOMOBILE_FINGERPRINT_MISSING,
    PATCH_FINGERPRINT_MISSING,
    ARTIFACT_FINGERPRINT_MISSING,
    BUILD_WAS_NOT_OFFLINE,
    RUNTIME_INTEGRATION_FORBIDDEN,
    VPN_ESTABLISH_FORBIDDEN,
    INTERNET_PERMISSION_FORBIDDEN,
    RECOVERABLE_START_MISSING,
    MANDATORY_PROTECT_MISSING,
}

data class Tun2SocksOfflinePocResult(
    val blockers: List<Tun2SocksOfflinePocBlocker>,
) {
    val artifactEligible: Boolean get() = blockers.isEmpty()

    // Deliberately constant: an offline packaging POC can never authorize active forwarding.
    val activationEligible: Boolean get() = false
}
