package cloud.kosch.aiandroid.ai

import cloud.kosch.aiandroid.model.AssistantActionRisk
import cloud.kosch.aiandroid.model.AssistantVisualState
import java.text.Normalizer
import java.util.Locale

data class AssistantLocalReply(
    val text: String,
    val command: LauncherCommand? = null,
    val handoffPrompt: String? = null,
    val visualState: AssistantVisualState = AssistantVisualState.IDLE,
    val actionRisk: AssistantActionRisk? = null,
)

/**
 * Deterministic assistant layer that stays useful without pretending to be a bundled generative LLM.
 * Every executable command is accompanied by the central risk class so the UI/runtime can display and gate it.
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
            return AssistantLocalReply(
                "Hallo. Ich bin dein KAL Assistant. Apps, Launcher-Einstellungen und Darstellung kann ich lokal bedienen. Für allgemeine Wissensfragen nutze ich dein eingerichtetes KI-Modell.",
            )
        }
        if (normalized in thanksPhrases) {
            return AssistantLocalReply("Gern.")
        }
        if (normalized in privacyPhrases) {
            return AssistantLocalReply(
                "Der Chat bleibt in dieser laufenden Sitzung. Lokale Launcher-Befehle brauchen kein Internet. Ein externes KI-Modell wird nur genutzt, wenn du es vorher eingerichtet und Netzwerkzugriff freigegeben hast.",
            )
        }
        if (normalized in helpPhrases) {
            return AssistantLocalReply(
                "Du kannst mir normale Anweisungen geben: Apps öffnen, Launcher-Bereiche aufrufen oder Darstellung, Dock, Icons und weitere unterstützte Einstellungen ändern. Freie Wissensfragen beantworte ich direkt im Chat, sobald ein KI-Modell eingerichtet ist.",
            )
        }

        return when (val command = commandPlanner.plan(raw)) {
            LauncherCommand.Empty -> AssistantLocalReply("Sag oder schreib mir, was ich für dich tun soll.")
            is LauncherCommand.RoutePrompt -> AssistantLocalReply(
                text = "Für allgemeine Wissensfragen brauche ich einmalig ein KI-Modell. Deine Frage bleibt hier im Chat; lokale Launcher-Anweisungen funktionieren trotzdem sofort.",
                handoffPrompt = command.prompt,
                visualState = AssistantVisualState.OFFLINE,
                actionRisk = AssistantCommandRiskClassifier.risk(command),
            )
            else -> AssistantLocalReply(
                text = commandAcknowledgement(command),
                command = command,
                visualState = AssistantVisualState.WORKING,
                actionRisk = AssistantCommandRiskClassifier.risk(command),
            )
        }
    }

    private fun commandAcknowledgement(command: LauncherCommand): String = when (command) {
        LauncherCommand.OpenDrawer -> "Ich öffne deine Apps."
        LauncherCommand.StartVoice -> "Ich bereite den Sprachmodus vor. Die Audio-Sitzung bleibt ein eigener sensibler Bestätigungsschritt."
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
        LauncherCommand.OpenCamera -> "Ich öffne die Kamera-App. Das aktiviert keine Assistant-Camera-Awareness."
        LauncherCommand.CreateSystemNote -> "Ich öffne eine Systemnotiz."
        is LauncherCommand.OpenSystemPanel -> "Ich öffne ${command.panel.title}."
        is LauncherCommand.SwitchScene -> "Ich wechsle zu ${command.scene.title}."
        is LauncherCommand.LaunchApp -> "Ich suche und öffne ${command.query}."
        is LauncherCommand.RoutePrompt -> "Ich halte die Frage für dein KI-Modell bereit."
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
