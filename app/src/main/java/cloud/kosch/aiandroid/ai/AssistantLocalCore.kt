package cloud.kosch.aiandroid.ai

import cloud.kosch.aiandroid.model.AssistantVisualState
import java.text.Normalizer
import java.util.Locale

data class AssistantLocalReply(
    val text: String,
    val command: LauncherCommand? = null,
    val handoffPrompt: String? = null,
    val visualState: AssistantVisualState = AssistantVisualState.IDLE,
)

/**
 * Deterministic assistant layer that stays useful without pretending to be a bundled generative LLM.
 */
class AssistantLocalCore(
    private val commandPlanner: LocalCommandPlanner = LocalCommandPlanner(),
) {
    fun reply(input: String): AssistantLocalReply {
        val raw = input.trim()
        if (raw.isBlank()) {
            return AssistantLocalReply("Sag oder schreib mir, was ich für dich tun soll.")
        }

        val normalized = raw.normalized()
        if (normalized in greetingPhrases) {
            return AssistantLocalReply("Hallo. Ich bin dein optionaler KoSch-Assistent. Launcher-Befehle kann ich lokal ausführen; freie KI-Fragen übergebe ich nur nach deiner Auswahl.")
        }
        if (normalized in thanksPhrases) {
            return AssistantLocalReply("Gern.")
        }
        if (normalized in privacyPhrases) {
            return AssistantLocalReply(
                "Dieser Chat bleibt in der aktuellen Sitzung. Der Local Core braucht kein Internet. Für generative Antworten wählst du den Zielanbieter ausdrücklich selbst.",
            )
        }
        if (normalized in helpPhrases) {
            return AssistantLocalReply(
                "Ich kann Apps, Szenen, Kamera, Kalender, Dateien, Widgets, Einstellungen und weitere Launcher-Funktionen lokal anstoßen. Für eine freie KI-Frage biete ich dir anschließend eine bewusste Anbieterübergabe an.",
            )
        }

        return when (val command = commandPlanner.plan(raw)) {
            LauncherCommand.Empty -> AssistantLocalReply("Sag oder schreib mir, was ich für dich tun soll.")
            is LauncherCommand.RoutePrompt -> AssistantLocalReply(
                text = "Das ist eine freie KI-Anfrage. Im aktuellen Offline-Build ist kein generatives Modell eingebettet. Ich kann den Text unverändert an einen von dir gewählten KI-Anbieter übergeben.",
                handoffPrompt = command.prompt,
                visualState = AssistantVisualState.OFFLINE,
            )
            else -> AssistantLocalReply(
                text = commandAcknowledgement(command),
                command = command,
                visualState = AssistantVisualState.WORKING,
            )
        }
    }

    private fun commandAcknowledgement(command: LauncherCommand): String = when (command) {
        LauncherCommand.OpenDrawer -> "Ich öffne deine Apps."
        LauncherCommand.StartVoice -> "Ich höre zu."
        LauncherCommand.OpenFiles -> "Ich öffne die sichere Dateiauswahl."
        LauncherCommand.OpenFileWorkspace -> "Ich öffne deinen freigegebenen Datei-Arbeitsraum."
        LauncherCommand.OpenControls -> "Ich öffne das Kontrollzentrum."
        LauncherCommand.OpenWidgets -> "Ich öffne das Widget-Board."
        LauncherCommand.OpenFaq -> "Ich öffne die lokale Hilfe."
        LauncherCommand.OpenPenSpace -> "Ich öffne Pen Space."
        LauncherCommand.OpenBackup -> "Ich öffne die verschlüsselte Workspace-Sicherung."
        LauncherCommand.OpenAudit -> "Ich öffne das lokale Audit."
        LauncherCommand.OpenProDesk -> "Ich öffne Pro Desk."
        LauncherCommand.PickContact -> "Ich öffne die einmalige Android-Kontaktauswahl."
        is LauncherCommand.OpenPhone -> "Ich öffne den sicheren Telefonweg."
        is LauncherCommand.OpenMessage -> "Ich öffne den Nachrichten-Composer."
        LauncherCommand.OpenCalendar -> "Ich öffne den Kalender."
        LauncherCommand.OpenAlarms -> "Ich öffne die Wecker-App."
        LauncherCommand.OpenCamera -> "Ich öffne die Kamera."
        LauncherCommand.CreateSystemNote -> "Ich öffne eine Systemnotiz."
        is LauncherCommand.OpenSystemPanel -> "Ich öffne ${command.panel.title}."
        is LauncherCommand.SwitchScene -> "Ich wechsle zu ${command.scene.title}."
        is LauncherCommand.LaunchApp -> "Ich suche und öffne ${command.query}."
        is LauncherCommand.RoutePrompt -> "Ich bereite die bewusste KI-Übergabe vor."
        LauncherCommand.Empty -> ""
    }

    private fun String.normalized(): String = Normalizer
        .normalize(lowercase(Locale.GERMAN), Normalizer.Form.NFD)
        .replace("\\p{M}+".toRegex(), "")
        .replace("[^a-z0-9 ]".toRegex(), " ")
        .replace("\\s+".toRegex(), " ")
        .trim()

    private companion object {
        val greetingPhrases = setOf("hallo", "hi", "hey", "guten morgen", "guten tag", "guten abend")
        val thanksPhrases = setOf("danke", "vielen dank", "danke dir", "thanks", "thank you")
        val privacyPhrases = setOf("datenschutz", "privacy", "ist der chat privat", "was speicherst du")
        val helpPhrases = setOf("hilfe", "help", "was kannst du", "was kannst du tun", "funktionen")
    }
}
