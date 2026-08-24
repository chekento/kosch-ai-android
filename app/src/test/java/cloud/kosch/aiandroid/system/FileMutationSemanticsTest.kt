package cloud.kosch.aiandroid.system

import cloud.kosch.aiandroid.model.AuditOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FileMutationSemanticsTest {
    @Test
    fun `failed mutation is failed and never refreshes`() {
        var refreshed = false
        val result = FileMutationSemantics.execute(
            mutation = { error("provider rejected mutation") },
            refresh = {
                refreshed = true
                "snapshot"
            },
        )

        assertTrue(result is FileMutationCompletion.Failed)
        assertEquals(AuditOutcome.FAILED, result.auditOutcome)
        assertFalse(refreshed)
    }

    @Test
    fun `successful mutation stays successful when refresh fails`() {
        val result = FileMutationSemantics.execute(
            mutation = { "rename-undo" },
            refresh = { error("provider cannot list directory") },
        )

        assertTrue(result is FileMutationCompletion.Applied)
        result as FileMutationCompletion.Applied
        assertEquals("rename-undo", result.effect)
        assertTrue(result.refresh.isFailure)
        assertEquals(AuditOutcome.SUCCESS, result.auditOutcome)
    }

    @Test
    fun `successful mutation and refresh retain both outputs`() {
        val result = FileMutationSemantics.execute(
            mutation = { 7 },
            refresh = { listOf("a", "b") },
        ) as FileMutationCompletion.Applied

        assertEquals(7, result.effect)
        assertEquals(listOf("a", "b"), result.refresh.getOrThrow())
    }
}
