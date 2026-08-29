package cloud.kosch.aiandroid.data

import org.junit.Assert.assertEquals
import org.junit.Test

class WorkspaceWidgetHostRecoveryTest {
    @Test
    fun plan_keepsOnlyExactProviderOwnedPairsAndReclaimsProcessDeathOrphans() {
        val plan = WorkspaceWidgetHostRecovery.plan(
            expectedProviders = mapOf(
                "item:clock" to "com.example/.Clock",
                "item:weather" to "com.example/.Weather",
            ),
            storedBindings = mapOf(
                "item:clock" to 41,
                "item:weather" to 42,
                "item:deleted" to 43,
            ),
            hostedProviders = mapOf(
                41 to "com.example/.Clock",
                42 to "com.other/.ReusedId",
                43 to "com.example/.Old",
                // Allocated before process death, but never committed to the device-local binding store.
                44 to null,
            ),
        )

        assertEquals(mapOf("item:clock" to 41), plan.validBindings)
        assertEquals(setOf(42, 43, 44), plan.orphanedHostedIds)
    }

    @Test
    fun plan_doesNotReclaimAValidBoundHostId() {
        val plan = WorkspaceWidgetHostRecovery.plan(
            expectedProviders = mapOf("item:clock" to "com.example/.Clock"),
            storedBindings = mapOf("item:clock" to 7),
            hostedProviders = mapOf(7 to "com.example/.Clock"),
        )

        assertEquals(mapOf("item:clock" to 7), plan.validBindings)
        assertEquals(emptySet<Int>(), plan.orphanedHostedIds)
    }
}
