package cloud.kosch.aiandroid.data

import cloud.kosch.aiandroid.model.AuditAction
import cloud.kosch.aiandroid.model.AuditEvent
import cloud.kosch.aiandroid.model.AuditOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuditCsvTest {
    @Test
    fun exportContainsOnlyFixedMetadataColumns() {
        val csv = AuditCsv.encode(
            listOf(AuditEvent(1_700_000_000_000L, AuditAction.DOCUMENT_INSPECT, AuditOutcome.SUCCESS)),
        )

        assertEquals("timestamp_utc,action,outcome", csv.lineSequence().first())
        assertTrue(csv.contains("DOCUMENT_INSPECT,SUCCESS"))
        assertFalse(csv.contains("filename"))
        assertFalse(csv.contains("prompt"))
        assertFalse(csv.contains("phone"))
    }
}
