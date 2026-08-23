package cloud.kosch.aiandroid.ai

import cloud.kosch.aiandroid.model.SceneId
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
}

