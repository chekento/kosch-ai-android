package cloud.kosch.aiandroid.security

enum class CapabilityRisk {
    READ_ONLY,
    EXTERNAL_VIEW,
    REVERSIBLE_WRITE,
    SENSITIVE_TRANSFER,
    DESTRUCTIVE,
}

enum class CapabilityAction {
    SEARCH_APPS,
    LAUNCH_APP,
    OPEN_DIALER,
    INSPECT_SELECTED_DOCUMENT,
    APPLY_LAYOUT,
    EXPORT_BACKUP,
    RESTORE_WORKSPACE,
    SHARE_WITH_AI_PROVIDER,
    CLEAR_AUDIT,
}

data class CapabilityRule(
    val risk: CapabilityRisk,
    val requiresPreview: Boolean,
    val requiresConfirmation: Boolean,
    val offersUndo: Boolean,
)

/** Closed policy table. A model may select an action, but cannot invent or widen its rule. */
object CapabilityPolicy {
    private val rules = mapOf(
        CapabilityAction.SEARCH_APPS to CapabilityRule(CapabilityRisk.READ_ONLY, false, false, false),
        CapabilityAction.LAUNCH_APP to CapabilityRule(CapabilityRisk.EXTERNAL_VIEW, false, false, false),
        CapabilityAction.OPEN_DIALER to CapabilityRule(CapabilityRisk.EXTERNAL_VIEW, true, false, false),
        CapabilityAction.INSPECT_SELECTED_DOCUMENT to CapabilityRule(CapabilityRisk.READ_ONLY, true, true, false),
        CapabilityAction.APPLY_LAYOUT to CapabilityRule(CapabilityRisk.REVERSIBLE_WRITE, true, true, true),
        CapabilityAction.EXPORT_BACKUP to CapabilityRule(CapabilityRisk.SENSITIVE_TRANSFER, true, true, false),
        CapabilityAction.RESTORE_WORKSPACE to CapabilityRule(CapabilityRisk.REVERSIBLE_WRITE, true, true, false),
        CapabilityAction.SHARE_WITH_AI_PROVIDER to CapabilityRule(CapabilityRisk.SENSITIVE_TRANSFER, true, true, false),
        CapabilityAction.CLEAR_AUDIT to CapabilityRule(CapabilityRisk.DESTRUCTIVE, true, true, false),
    )

    fun rule(action: CapabilityAction): CapabilityRule = checkNotNull(rules[action])
}
