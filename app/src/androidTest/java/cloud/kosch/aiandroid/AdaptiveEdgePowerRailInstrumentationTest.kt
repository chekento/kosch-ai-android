package cloud.kosch.aiandroid

import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import cloud.kosch.aiandroid.model.AdaptiveHomePresentation
import cloud.kosch.aiandroid.ui.AdaptiveEdgePowerRail
import cloud.kosch.aiandroid.ui.theme.KoSchLauncherTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AdaptiveEdgePowerRailInstrumentationTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun expandedRail_exposesCoreActionsAndKeepsPenHiddenWithoutStylusPresentation() {
        var apps = 0
        var ask = 0
        var controls = 0
        var pen = 0

        composeTestRule.setContent {
            KoSchLauncherTheme {
                AdaptiveEdgePowerRail(
                    presentation = presentation(showPen = false, emphasizePen = false),
                    onOpenApps = { apps += 1 },
                    onAsk = { ask += 1 },
                    onControls = { controls += 1 },
                    onPen = { pen += 1 },
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Power Rail · Alle Apps").assertExists().performClick()
        composeTestRule.onNodeWithContentDescription("Power Rail · Ask").assertExists().performClick()
        composeTestRule.onNodeWithContentDescription("Power Rail · Kontrollzentrum").assertExists().performClick()
        composeTestRule.onNodeWithContentDescription("Power Rail · Pen Space").assertDoesNotExist()

        composeTestRule.runOnIdle {
            assertEquals(1, apps)
            assertEquals(1, ask)
            assertEquals(1, controls)
            assertEquals(0, pen)
        }
    }

    @Test
    fun stylusPresentation_exposesPrioritizedPenAction() {
        var pen = 0

        composeTestRule.setContent {
            KoSchLauncherTheme {
                AdaptiveEdgePowerRail(
                    presentation = presentation(showPen = true, emphasizePen = true),
                    onOpenApps = {},
                    onAsk = {},
                    onControls = {},
                    onPen = { pen += 1 },
                )
            }
        }

        composeTestRule
            .onNodeWithContentDescription("Power Rail · Pen Space · für Stift priorisiert")
            .assertExists()
            .performClick()

        composeTestRule.runOnIdle { assertEquals(1, pen) }
    }

    private fun presentation(
        showPen: Boolean,
        emphasizePen: Boolean,
    ) = AdaptiveHomePresentation(
        horizontalPaddingDp = 12,
        verticalPaddingDp = 10,
        verticalGapDp = 8,
        dockPinnedAppLimit = 8,
        showEdgePowerRail = true,
        showPenShortcut = showPen,
        emphasizePenShortcut = emphasizePen,
    )
}
