package cloud.kosch.aiandroid.system

import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetHostOwnershipTest {
    @Test
    fun orphanedIds_reclaimsOnlyPositiveHostedIdsWithoutDeviceBindingOwner() {
        assertEquals(
            setOf(43, 44),
            WidgetHostOwnership.orphanedIds(
                hostedIds = setOf(-1, 0, 41, 42, 43, 44),
                ownedIds = setOf(41, 42, 99),
            ),
        )
    }

    @Test
    fun orphanedIds_keepsEveryHostedIdThatStillHasAnOwner() {
        assertEquals(
            emptySet<Int>(),
            WidgetHostOwnership.orphanedIds(
                hostedIds = setOf(7, 8),
                ownedIds = setOf(7, 8),
            ),
        )
    }
}
