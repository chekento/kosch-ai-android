package cloud.kosch.aiandroid.assistant

import cloud.kosch.aiandroid.model.AssistantAnchor
import cloud.kosch.aiandroid.model.LabelMode
import cloud.kosch.aiandroid.model.MotionProfile
import cloud.kosch.aiandroid.model.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AssistantLauncherControlPlannerTest {
    private val planner = AssistantLauncherControlPlanner()

    @Test
    fun appearanceCommands_areRecognizedLocally() {
        assertEquals(
            AssistantLauncherControlAction.SetThemeMode(ThemeMode.DARK),
            planner.plan("Mach das Theme bitte dunkel"),
        )
        assertEquals(
            AssistantLauncherControlAction.SetThemeMode(ThemeMode.LIGHT),
            planner.plan("Stell die Darstellung auf hell"),
        )
        assertEquals(
            AssistantLauncherControlAction.SetMaterialYou(false),
            planner.plan("Material You ausschalten"),
        )
        assertEquals(
            AssistantLauncherControlAction.SetMotionProfile(MotionProfile.REDUCED),
            planner.plan("Animationen reduzieren"),
        )
    }

    @Test
    fun homeDockAndAssistantCommands_areRecognizedLocally() {
        assertEquals(
            AssistantLauncherControlAction.SetDockEnabled(false),
            planner.plan("Dock ausschalten"),
        )
        assertEquals(
            AssistantLauncherControlAction.SetDockItemLimit(5),
            planner.plan("Dock auf 5 Apps"),
        )
        assertEquals(
            AssistantLauncherControlAction.SetDockAskEnabled(false),
            planner.plan("Ask im Dock aus"),
        )
        assertEquals(
            AssistantLauncherControlAction.SetPageIndicator(false),
            planner.plan("Seitenpunkte aus"),
        )
        assertEquals(
            AssistantLauncherControlAction.SetHomeLabels(LabelMode.NEVER),
            planner.plan("App-Namen nie anzeigen"),
        )
        assertEquals(
            AssistantLauncherControlAction.AdjustHomeIconScale(+0.10f),
            planner.plan("Mach die Icons größer"),
        )
        assertEquals(
            AssistantLauncherControlAction.SetAssistantAnchor(AssistantAnchor.LEFT),
            planner.plan("Assistent nach links"),
        )
    }

    @Test
    fun statusUndoAndUnknownText_haveDeterministicResults() {
        assertEquals(
            AssistantLauncherControlAction.DescribeAppearance,
            planner.plan("Welches Theme ist eingestellt?"),
        )
        assertEquals(
            AssistantLauncherControlAction.UndoLastSettingsChange,
            planner.plan("Mach das rückgängig"),
        )
        assertNull(planner.plan("Erkläre mir bitte Quantenverschränkung"))
    }
}
