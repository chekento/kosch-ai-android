package cloud.kosch.aiandroid.ai

import cloud.kosch.aiandroid.model.SceneId
import cloud.kosch.aiandroid.model.SystemPanel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalCommandPlannerTest {
    private val planner = LocalCommandPlanner()

    @Test
    fun `blank input does nothing`() {
        assertEquals(LauncherCommand.Empty, planner.plan("   "))
    }

    @Test
    fun `drawer command stays local`() {
        assertEquals(LauncherCommand.OpenDrawer, planner.plan("Alle Apps"))
    }

    @Test
    fun `german app command extracts original query`() {
        assertEquals(
            LauncherCommand.LaunchApp("Signal"),
            planner.plan("Öffne Signal"),
        )
    }

    @Test
    fun `scene command resolves without cloud model`() {
        assertEquals(
            LauncherCommand.SwitchScene(SceneId.STUDIO),
            planner.plan("Wechsle zu Studio"),
        )
    }

    @Test
    fun `unrecognized request needs explicit provider routing`() {
        val command = planner.plan("Fasse meinen Tag zusammen")
        assertTrue(command is LauncherCommand.RoutePrompt)
        assertEquals("Fasse meinen Tag zusammen", (command as LauncherCommand.RoutePrompt).prompt)
    }

    @Test
    fun `dial command sanitizes a number but still delegates the actual call`() {
        assertEquals(
            LauncherCommand.OpenPhone("+4930123456"),
            planner.plan("Wähle +49 (30) 123-456"),
        )
    }

    @Test
    fun `file command stays inside explicit document picker route`() {
        assertEquals(LauncherCommand.OpenFiles, planner.plan("Datei analysieren"))
    }

    @Test
    fun `home selection command exposes the launcher escape hatch`() {
        assertEquals(
            LauncherCommand.OpenSystemPanel(SystemPanel.HOME_SELECTION),
            planner.plan("Launcher Auswahl"),
        )
    }

    @Test
    fun `faq command opens local help`() {
        assertEquals(LauncherCommand.OpenFaq, planner.plan("Hilfe"))
    }

    @Test
    fun `pen command opens the local pen workspace`() {
        assertEquals(LauncherCommand.OpenPenSpace, planner.plan("Pen Space"))
    }
}
