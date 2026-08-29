package cloud.kosch.aiandroid.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WorkspaceStyleColorTest {
    @Test
    fun `parses rgb argb and short hex forms`() {
        assertEquals(0xFFFF0000.toInt(), WorkspaceStyleColor.parse("#ff0000"))
        assertEquals(0x80FF0000.toInt(), WorkspaceStyleColor.parse("80FF0000"))
        assertEquals(0xFFAABBCC.toInt(), WorkspaceStyleColor.parse("#abc"))
        assertEquals(0xAABBCCDD.toInt(), WorkspaceStyleColor.parse("#abcd"))
        assertEquals(0xFF112233.toInt(), WorkspaceStyleColor.parse("0x112233"))
    }

    @Test
    fun `invalid color strings fail closed`() {
        assertNull(WorkspaceStyleColor.parse("red"))
        assertNull(WorkspaceStyleColor.parse("#12"))
        assertNull(WorkspaceStyleColor.parse("#GG1122"))
        assertNull(WorkspaceStyleColor.parse("#123456789"))
    }

    @Test
    fun `formats signed ints as eight digit argb`() {
        assertEquals("#FFAABBCC", WorkspaceStyleColor.format(0xFFAABBCC.toInt()))
        assertEquals("", WorkspaceStyleColor.format(null))
    }
}
