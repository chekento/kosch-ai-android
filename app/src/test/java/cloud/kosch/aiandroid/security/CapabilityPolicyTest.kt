package cloud.kosch.aiandroid.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CapabilityPolicyTest {
    @Test
    fun everyDeclaredActionHasExactlyOneRule() {
        assertEquals(
            CapabilityAction.entries.size,
            CapabilityAction.entries.map(CapabilityPolicy::rule).size,
        )
    }

    @Test
    fun destructiveAndSensitiveActionsCannotSkipConfirmation() {
        CapabilityAction.entries
            .map(CapabilityPolicy::rule)
            .filter { it.risk == CapabilityRisk.DESTRUCTIVE || it.risk == CapabilityRisk.SENSITIVE_TRANSFER }
            .forEach { rule ->
                assertTrue(rule.requiresPreview)
                assertTrue(rule.requiresConfirmation)
                assertFalse(rule.offersUndo)
            }
    }

    @Test
    fun layoutWriteIsPreviewedConfirmedAndUndoable() {
        val rule = CapabilityPolicy.rule(CapabilityAction.APPLY_LAYOUT)
        assertTrue(rule.requiresPreview)
        assertTrue(rule.requiresConfirmation)
        assertTrue(rule.offersUndo)
    }
}
