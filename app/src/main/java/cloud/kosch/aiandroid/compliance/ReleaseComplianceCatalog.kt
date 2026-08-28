package cloud.kosch.aiandroid.compliance

/**
 * Machine-readable truth boundary for privacy/store claims.
 *
 * This catalog describes what KAL itself does, not what a destination app may do after an explicit Android handoff.
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
    AUTH_CREDENTIAL,
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
    val kalNetworkTransfer: Boolean,
    val productionEnabled: Boolean,
    val disclosure: String,
) {
    init {
        require(id.matches(Regex("[a-z0-9_]+"))) { "Compliance capability id must be stable" }
        require(title.isNotBlank())
        require(disclosure.isNotBlank())
        require(!kalNetworkTransfer || dataClass != ComplianceDataClass.NONE) {
            "A KAL network transfer must name the affected data class"
        }
        require(!defaultEnabled || productionEnabled) {
            "A capability cannot default to enabled when it is excluded from production"
        }
        require(!kalNetworkTransfer || requiresExplicitUserAction) {
            "KAL network transfers must remain explicit user actions"
        }
        require(!kalNetworkTransfer || !defaultEnabled) {
            "KAL network transfers must remain off by default"
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
            kalNetworkTransfer = false,
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
            kalNetworkTransfer = false,
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
            kalNetworkTransfer = false,
            productionEnabled = true,
            disclosure = "Notification access is granted only in Android settings; KAL keeps package-level badge counts in process memory and does not copy message text, titles, people or extras.",
        ),
        ComplianceCapability(
            id = "local_usage_learning",
            title = "Local usage learning",
            dataClass = ComplianceDataClass.APP_ACTIVITY_METADATA,
            defaultEnabled = true,
            requiresExplicitUserAction = false,
            retention = ComplianceRetention.BOUNDED_LOCAL,
            kalNetworkTransfer = false,
            productionEnabled = true,
            disclosure = "Adaptive ranking uses bounded local app keys, launch counts and last-used timestamps; no content is learned or uploaded by KAL.",
        ),
        ComplianceCapability(
            id = "local_audit_log",
            title = "Local audit log",
            dataClass = ComplianceDataClass.APP_ACTIVITY_METADATA,
            defaultEnabled = true,
            requiresExplicitUserAction = false,
            retention = ComplianceRetention.BOUNDED_LOCAL,
            kalNetworkTransfer = false,
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
            kalNetworkTransfer = false,
            productionEnabled = true,
            disclosure = "KAL itself does not upload this handoff. A destination app receives the exact text only after a two-step explicit Android share handoff; subsequent processing is controlled by that destination app.",
        ),
        ComplianceCapability(
            id = "provider_authentication",
            title = "Direct AI provider authentication",
            dataClass = ComplianceDataClass.AUTH_CREDENTIAL,
            defaultEnabled = false,
            requiresExplicitUserAction = true,
            retention = ComplianceRetention.USER_CONTROLLED_LOCAL,
            kalNetworkTransfer = true,
            productionEnabled = true,
            disclosure = "A provider connection starts only after the user chooses Connect. OAuth authorization occurs in the system browser; resulting tokens or provider keys are encrypted with Android Keystore and are never included in portable backup.",
        ),
        ComplianceCapability(
            id = "direct_ai_provider_request",
            title = "Direct AI provider request",
            dataClass = ComplianceDataClass.USER_TEXT,
            defaultEnabled = false,
            requiresExplicitUserAction = true,
            retention = ComplianceRetention.PROCESS_ONLY,
            kalNetworkTransfer = true,
            productionEnabled = true,
            disclosure = "KAL may send user-selected prompt/context data directly to an explicitly connected AI provider only after Cloud Access is enabled and the foreground request is confirmed. KAL does not perform background provider requests.",
        ),
        ComplianceCapability(
            id = "vpn_n1_prototype",
            title = "N1 VPN consent prototype",
            dataClass = ComplianceDataClass.NETWORK_METADATA,
            defaultEnabled = false,
            requiresExplicitUserAction = true,
            retention = ComplianceRetention.NONE,
            kalNetworkTransfer = false,
            productionEnabled = false,
            disclosure = "The inert N1 VpnService and its Security surface are development-only and are excluded from release manifests until an eligible production security/network capability exists.",
        ),
    )

    val productionCapabilities: List<ComplianceCapability>
        get() = capabilities.filter(ComplianceCapability::productionEnabled)

    val developmentOnlyCapabilities: List<ComplianceCapability>
        get() = capabilities.filterNot(ComplianceCapability::productionEnabled)

    fun byId(id: String): ComplianceCapability? = capabilities.firstOrNull { it.id == id }

    /** Every KAL-controlled production network transfer must be opt-in and foreground/user initiated. */
    fun productionNetworkTransfersAreExplicitOptIn(): Boolean = productionCapabilities
        .filter(ComplianceCapability::kalNetworkTransfer)
        .all { !it.defaultEnabled && it.requiresExplicitUserAction }
}
