package cloud.kosch.aiandroid.model

import org.junit.Assert.assertEquals
import org.junit.Test

class WorkspacePageIndicatorPolicyTest {
    @Test
    fun smallWorkspacesShowEveryPage() {
        assertEquals(
            listOf<Int?>(0, 1, 2, 3, 4, 5),
            WorkspacePageIndicatorPolicy.slots(pageCount = 6, activeIndex = 2),
        )
    }

    @Test
    fun largeWorkspaceNearStartKeepsStartAndEndVisible() {
        assertEquals(
            listOf<Int?>(0, 1, 2, 3, 4, null, 11),
            WorkspacePageIndicatorPolicy.slots(pageCount = 12, activeIndex = 1),
        )
    }

    @Test
    fun largeWorkspaceCentersCurrentPageWithoutGrowing() {
        assertEquals(
            listOf<Int?>(0, null, 5, 6, 7, null, 11),
            WorkspacePageIndicatorPolicy.slots(pageCount = 12, activeIndex = 6),
        )
        assertEquals(
            WorkspacePageIndicatorPolicy.MAX_VISIBLE_SLOTS,
            WorkspacePageIndicatorPolicy.slots(pageCount = 50, activeIndex = 24).size,
        )
    }

    @Test
    fun largeWorkspaceNearEndKeepsEndClusterVisible() {
        assertEquals(
            listOf<Int?>(0, null, 7, 8, 9, 10, 11),
            WorkspacePageIndicatorPolicy.slots(pageCount = 12, activeIndex = 10),
        )
    }

    @Test
    fun invalidActiveIndexIsClampedSafely() {
        assertEquals(
            listOf<Int?>(0, 1, 2, 3, 4, null, 9),
            WorkspacePageIndicatorPolicy.slots(pageCount = 10, activeIndex = -8),
        )
        assertEquals(emptyList<Int?>(), WorkspacePageIndicatorPolicy.slots(pageCount = 0, activeIndex = 0))
    }
}
