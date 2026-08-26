package cloud.kosch.aiandroid.security

enum class VpnConsentAction {
    NO_SYSTEM_DIALOG,
    OPEN_SYSTEM_DIALOG,
    REQUIRE_CONFLICT_ACKNOWLEDGEMENT,
}

/** Pure decision layer between Android inspection and the consent UI. */
object VpnConsentDecisionPolicy {
    fun actionFor(inspection: VpnConsentInspection): VpnConsentAction = when {
        inspection.authorization == VpnAuthorizationState.AUTHORIZED -> {
            VpnConsentAction.NO_SYSTEM_DIALOG
        }

        inspection.conflict == VpnConflictState.NONE_DETECTED -> {
            VpnConsentAction.OPEN_SYSTEM_DIALOG
        }

        else -> {
            VpnConsentAction.REQUIRE_CONFLICT_ACKNOWLEDGEMENT
        }
    }
}
