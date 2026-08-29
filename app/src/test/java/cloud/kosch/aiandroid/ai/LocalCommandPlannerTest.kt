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
    fun `wake word and politeness wrappers do not weaken deterministic routing`() {
        assertEquals(LauncherCommand.OpenCamera, planner.plan("Computer, öffne Kamera bitte!"))
        assertEquals(LauncherCommand.OpenControls, planner.plan("Hey KoSch, zeige Kontrollzentrum please"))
        assertEquals(
            LauncherCommand.OpenSystemPanel(SystemPanel.WIFI),
            planner.plan("KoSch: öffne WLAN bitte"),
        )
    }

    @Test
    fun `explicit app syntax supports power users`() {
        assertEquals(LauncherCommand.LaunchApp("Signal"), planner.plan("app: Signal"))
        assertEquals(LauncherCommand.LaunchApp("Obsidian"), planner.plan("app Obsidian"))
    }

    @Test
    fun `explicit ask syntax strips only the routing prefix`() {
        assertEquals(
            LauncherCommand.RoutePrompt("Vergleiche lokale LLMs"),
            planner.plan("KI Vergleiche lokale LLMs"),
        )
        assertEquals(
            LauncherCommand.RoutePrompt("summarize this safely"),
            planner.plan("ask summarize this safely"),
        )
    }

    @Test
    fun `scene command resolves without cloud model`() {
        assertEquals(
            LauncherCommand.SwitchScene(SceneId.STUDIO),
            planner.plan("Wechsle zu Studio"),
        )
        assertEquals(
            LauncherCommand.SwitchScene(SceneId.STUDIO),
            planner.plan("Studio Modus"),
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
        assertEquals(LauncherCommand.OpenFileWorkspace, planner.plan("Dateien verwalten"))
    }

    @Test
    fun `home selection command exposes the launcher escape hatch`() {
        assertEquals(
            LauncherCommand.OpenSystemPanel(SystemPanel.HOME_SELECTION),
            planner.plan("Launcher Auswahl"),
        )
        assertEquals(
            LauncherCommand.OpenSystemPanel(SystemPanel.HOME_SELECTION),
            planner.plan("Öffne Home App"),
        )
    }

    @Test
    fun `faq command opens local help`() {
        assertEquals(LauncherCommand.OpenFaq, planner.plan("Hilfe"))
    }

    @Test
    fun `pen command opens the local pen workspace`() {
        assertEquals(LauncherCommand.OpenPenSpace, planner.plan("Pen Space"))
        assertEquals(LauncherCommand.OpenPenSpace, planner.plan("Öffne Canvas"))
    }

    @Test
    fun `professional commands remain deterministic and local`() {
        assertEquals(LauncherCommand.OpenProDesk, planner.plan("Pro Desk"))
        assertEquals(LauncherCommand.OpenProDesk, planner.plan("Power Desk"))
        assertEquals(LauncherCommand.OpenBackup, planner.plan("Workspace sichern"))
        assertEquals(LauncherCommand.OpenAudit, planner.plan("Sicherheitsverlauf"))
        assertEquals(LauncherCommand.PickContact, planner.plan("Kontakt auswählen"))
    }

    @Test
    fun `professional Android settings remain explicit system routes`() {
        assertEquals(LauncherCommand.OpenSystemPanel(SystemPanel.WALLPAPER), planner.plan("Hintergrundbild"))
        assertEquals(LauncherCommand.OpenSystemPanel(SystemPanel.ACCESSIBILITY), planner.plan("Barrierefreiheit"))
        assertEquals(LauncherCommand.OpenSystemPanel(SystemPanel.DEFAULT_APPS), planner.plan("Standard Apps"))
        assertEquals(LauncherCommand.OpenSystemPanel(SystemPanel.STORAGE), planner.plan("Speicher"))
        assertEquals(LauncherCommand.OpenSystemPanel(SystemPanel.PRIVACY), planner.plan("Öffne Datenschutz"))
    }

    @Test
    fun `professional app independent actions use system routes`() {
        assertEquals(LauncherCommand.OpenCalendar, planner.plan("Kalender"))
        assertEquals(LauncherCommand.OpenAlarms, planner.plan("Öffne Wecker"))
        assertEquals(LauncherCommand.OpenCamera, planner.plan("Öffne Kamera"))
        assertEquals(LauncherCommand.CreateSystemNote, planner.plan("Systemnotiz"))
        assertEquals(LauncherCommand.OpenMessage(null), planner.plan("SMS"))
        assertEquals(LauncherCommand.OpenMessage("+4930123456"), planner.plan("Nachricht an +49 30 123456"))
    }
}
