package cloud.kosch.aiandroid.data

import cloud.kosch.aiandroid.model.AppUsageSignal
import org.junit.Assert.assertEquals
import org.junit.Test

class AppKeyMigrationTest {
    @Test
    fun `legacy keys migrate without losing order or creating duplicates`() {
        val migrated = AppKeyMigration.keys(
            values = listOf("42:pkg/.Main", "7:pkg/.Other", "100:pkg/.Main"),
            aliases = mapOf("42:pkg/.Main" to "100:pkg/.Main"),
        )

        assertEquals(listOf("100:pkg/.Main", "7:pkg/.Other"), migrated)
    }

    @Test
    fun `usage collisions merge counts and preserve newest timestamp`() {
        val migrated = AppKeyMigration.usage(
            values = mapOf(
                "42:pkg/.Main" to AppUsageSignal("42:pkg/.Main", 3, 100L),
                "100:pkg/.Main" to AppUsageSignal("100:pkg/.Main", 5, 200L),
            ),
            aliases = mapOf("42:pkg/.Main" to "100:pkg/.Main"),
        )

        assertEquals(AppUsageSignal("100:pkg/.Main", 8, 200L), migrated["100:pkg/.Main"])
    }
}
