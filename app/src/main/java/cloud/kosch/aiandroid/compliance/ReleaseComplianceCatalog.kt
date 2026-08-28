package cloud.kosch.aiandroid.compliance

/**
 * Machine-readable truth boundary for privacy/store claims.
 *
 * This catalog describes what KoSch itself does, not what a destination app may do after an explicit Android handoff.
 * It is intentionally conservative: a capability may only be described as production-enabled when the release build
 * actually exposes it. New sensitive capabilities must be added here before they may be marketed as LIVE.
 */
enum class ComplianceDataClass {
    NONE,
    APP_ACTIVITY_METADATA,
    NOTIFICATION_METADATA,
    CAMERA_CONTENT,
    SCREEN_CONTENT,
    USER_TEXT,
    NETWORK_METADATA,
}

enum class ComplianceRetention {
    NONE,
    PROCESS_ONLY,
    SESSION_ONLY,
    BOUNDED_LOCAL,
    USER_CONTROLLED_LOCAL,
}

data class ComplianceCapability(
    val id: String,
    val title: String,
    val dataClass: ComplianceDataClass,
    val defaultEnabled: Boolean,
    val requiresExplicitUserAction: Boolean,
    val retention: ComplianceRetention,
    val koSchNetworkTransfer: Boolean,
    val productionEnabled: Boolean,
    val disclosure: String,
) {
    init {
        require(id.matches(Regex("[a-z0-9_]+"))) { "Compliance capability id must be stable" }
        require(title.isNotBlank())
        require(disclosure.isNotBlank())
        require(!koSchNetworkTransfer || dataClass != ComplianceDataClass.NONE) {
            "A KoSch network transfer must name the affected data class"
        }
        require(!defaultEnabled || productionEnabled) {
            "A capability cannot default to enabled when it is excluded from production"
        }
    }
}

object ReleaseComplianceCatalog {
    val capabilities: List<ComplianceCapability> = listOf(
        ComplianceCapability(
            id = "camera_awareness",
            title = "Camera Awareness",
            dataClass = ComplianceDataClass.CAMERA_CONTENT,
            defaultEnabled = false,
            requiresExplicitUserAction = true,
            retention = ComplianceRetention.PROCESS_ONLY,
            koSchNetworkTransfer = false,
            productionEnabled = true,
            disclosure = "Camera Awareness is off by default, uses a visible CameraX session and captures a context frame only after explicit user opt-in/request.",
        ),
        ComplianceCapability(
            id = "screen_awareness",
            title = "Screen Awareness",
            dataClass = ComplianceDataClass.SCREEN_CONTENT,
            defaultEnabled = false,
            requiresExplicitUserAction = true,
            retention = ComplianceRetention.PROCESS_ONLY,
            koSchNetworkTransfer = false,
            productionEnabled = true,
            disclosure = "Screen Awareness is off by default and requires Android MediaProjection consent plus a visible foreground-service notification.",
        ),
        ComplianceCapability(
            id = "notification_badges",
            title = "Notification badges",
            dataClass = ComplianceDataClass.NOTIFICATION_METADATA,
            defaultEnabled = false,
            requiresExplicitUserAction = true,
            retention = ComplianceRetention.PROCESS_ONLY,
            koSchNetworkTransfer = false,
            productionEnabled = true,
            disclosure = "Notification access is granted only in Android settings; KoSch keeps package-level badge counts in process memory and does not copy message text, titles, people or extras.",
        ),
        ComplianceCapability(
            id = "local_usage_learning",
            title = "Local usage learning",
            dataClass = ComplianceDataClass.APP_ACTIVITY_METADATA,
            defaultEnabled = true,
            requiresExplicitUserAction = false,
            retention = ComplianceRetention.BOUNDED_LOCAL,
            koSchNetworkTransfer = false,
            productionEnabled = true,
            disclosure = "Adaptive ranking uses bounded local app keys, launch counts and last-used timestamps; no content is learned or uploaded by KoSch.",
        ),
        ComplianceCapability(
            id = "local_audit_log",
            title = "Local audit log",
            dataClass = ComplianceDataClass.APP_ACTIVITY_METADATA,
            defaultEnabled = true,
            requiresExplicitUserAction = false,
            retention = ComplianceRetention.BOUNDED_LOCAL,
            koSchNetworkTransfer = false,
            productionEnabled = true,
            disclosure = "The local audit log stores bounded action/outcome metadata without a free-text field and can be cleared/exported by the user.",
        ),
        ComplianceCapability(
            id = "external_prompt_handoff",
            title = "External AI prompt handoff",
            dataClass = ComplianceDataClass.USER_TEXT,
            defaultEnabled = false,
            requiresExplicitUserAction = true,
            retention = ComplianceRetention.PROCESS_ONLY,
            koSchNetworkTransfer = false,
            productionEnabled = true,
            disclosure = "KoSch itself does not upload the prompt. A destination app receives the exact text only after a two-step explicit Android share handoff; subsequent processing is controlled by that destination app.",
        ),
        ComplianceCapability(
            id = "vpn_n1_prototype",
            title = "N1 VPN consent prototype",
            dataClass = ComplianceDataClass.NETWORK_METADATA,
            defaultEnabled = false,
            requiresExplicitUserAction = true,
            retention = ComplianceRetention.NONE,
            koSchNetworkTransfer = false,
            productionEnabled = false,
            disclosure = "The inert N1 VpnService and its Security surface are development-only and are excluded from release manifests until an eligible production security/network capability exists.",
        ),
    )

    val productionCapabilities: List<ComplianceCapability>
        get() = capabilities.filter(ComplianceCapability::productionEnabled)

    val developmentOnlyCapabilities: List<ComplianceCapability>
        get() = capabilities.filterNot(ComplianceCapability::productionEnabled)

    fun byId(id: String): ComplianceCapability? = capabilities.firstOrNull { it.id == id }

    /**
     * Store/Data-Safety invariant: the current production catalog must not claim any silent KoSch network transfer.
     * If a future release adds one, this invariant must intentionally change together with policy, UI and declarations.
     */
    fun currentReleaseHasNoKoSchContentUpload(): Boolean =
        productionCapabilities.none(ComplianceCapability::koSchNetworkTransfer)
}
