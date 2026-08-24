package cloud.kosch.aiandroid.ai

import cloud.kosch.aiandroid.model.SceneId
import cloud.kosch.aiandroid.model.SystemPanel
import java.text.Normalizer
import java.util.Locale

sealed interface LauncherCommand {
    data object Empty : LauncherCommand
    data object OpenDrawer : LauncherCommand
    data object StartVoice : LauncherCommand
    data object OpenFiles : LauncherCommand
    data object OpenFileWorkspace : LauncherCommand
    data object OpenControls : LauncherCommand
    data object OpenWidgets : LauncherCommand
    data object OpenFaq : LauncherCommand
    data object OpenPenSpace : LauncherCommand
    data object OpenBackup : LauncherCommand
    data object OpenAudit : LauncherCommand
    data object OpenProDesk : LauncherCommand
    data object PickContact : LauncherCommand
    data class OpenPhone(val number: String?) : LauncherCommand
    data class OpenMessage(val number: String?) : LauncherCommand
    data object OpenCalendar : LauncherCommand
    data object OpenAlarms : LauncherCommand
    data object OpenCamera : LauncherCommand
    data object CreateSystemNote : LauncherCommand
    data class OpenSystemPanel(val panel: SystemPanel) : LauncherCommand
    data class SwitchScene(val scene: SceneId) : LauncherCommand
    data class LaunchApp(val query: String) : LauncherCommand
    data class RoutePrompt(val prompt: String) : LauncherCommand
}

class LocalCommandPlanner {
    fun plan(input: String): LauncherCommand {
        val raw = input.trim()
        if (raw.isEmpty()) return LauncherCommand.Empty

        val normalized = raw.normalized()
        if (normalized in drawerCommands) return LauncherCommand.OpenDrawer
        if (normalized in voiceCommands) return LauncherCommand.StartVoice
        if (normalized in fileWorkspaceCommands) return LauncherCommand.OpenFileWorkspace
        if (normalized in fileCommands) return LauncherCommand.OpenFiles
        if (normalized in controlCommands) return LauncherCommand.OpenControls
        if (normalized in widgetCommands) return LauncherCommand.OpenWidgets
        if (normalized in faqCommands) return LauncherCommand.OpenFaq
        if (normalized in penCommands) return LauncherCommand.OpenPenSpace
        if (normalized in backupCommands) return LauncherCommand.OpenBackup
        if (normalized in auditCommands) return LauncherCommand.OpenAudit
        if (normalized in proDeskCommands) return LauncherCommand.OpenProDesk
        if (normalized in contactCommands) return LauncherCommand.PickContact
        if (normalized in calendarCommands) return LauncherCommand.OpenCalendar
        if (normalized in alarmCommands) return LauncherCommand.OpenAlarms
        if (normalized in cameraCommands) return LauncherCommand.OpenCamera
        if (normalized in systemNoteCommands) return LauncherCommand.CreateSystemNote
        systemPanelFrom(normalized)?.let { return LauncherCommand.OpenSystemPanel(it) }
        messageFrom(raw, normalized)?.let { return it }
        phoneFrom(raw, normalized)?.let { return it }

        sceneFrom(normalized)?.let { return LauncherCommand.SwitchScene(it) }

        launchPrefixes.firstOrNull { normalized.startsWith(it) }?.let { prefix ->
            val query = raw.drop(prefix.length).trim()
            if (query.isNotEmpty()) return LauncherCommand.LaunchApp(query)
        }

        return LauncherCommand.RoutePrompt(raw)
    }

    private fun systemPanelFrom(value: String): SystemPanel? = when (value) {
        "wlan", "wifi", "wi-fi", "wlan einstellungen" -> SystemPanel.WIFI
        "bluetooth", "bluetooth einstellungen" -> SystemPanel.BLUETOOTH
        "benachrichtigungen", "notification settings" -> SystemPanel.NOTIFICATIONS
        "benachrichtigungspunkte", "notification dots", "app punkte" -> SystemPanel.NOTIFICATION_ACCESS
        "hintergrund", "hintergrundbild", "wallpaper" -> SystemPanel.WALLPAPER
        "anzeige", "display", "bildschirm" -> SystemPanel.DISPLAY
        "ton", "sound", "lautstarke", "lautstärke" -> SystemPanel.SOUND
        "akku", "batterie", "battery" -> SystemPanel.BATTERY
        "datenschutz", "privacy" -> SystemPanel.PRIVACY
        "bedienungshilfen", "barrierefreiheit", "accessibility" -> SystemPanel.ACCESSIBILITY
        "standard apps", "standard-apps", "default apps" -> SystemPanel.DEFAULT_APPS
        "speicher", "storage" -> SystemPanel.STORAGE
        "android einstellungen", "systemeinstellungen", "settings" -> SystemPanel.ANDROID_SETTINGS
        "home auswahl", "launcher auswahl", "start app auswahl", "standard launcher" -> SystemPanel.HOME_SELECTION
        else -> null
    }

    private fun phoneFrom(raw: String, normalized: String): LauncherCommand.OpenPhone? {
        if (normalized in phoneCommands) return LauncherCommand.OpenPhone(null)
        val prefix = dialPrefixes.firstOrNull { normalized.startsWith(it) } ?: return null
        val rawNumber = raw.drop(prefix.length).removeSuffix(" an").trim()
        val number = PhoneNumberParser.sanitize(rawNumber) ?: return LauncherCommand.OpenPhone(null)
        return LauncherCommand.OpenPhone(number)
    }

    private fun messageFrom(raw: String, normalized: String): LauncherCommand.OpenMessage? {
        if (normalized in messageCommands) return LauncherCommand.OpenMessage(null)
        val prefix = messagePrefixes.firstOrNull { normalized.startsWith(it) } ?: return null
        val rawNumber = raw.drop(prefix.length).trim()
        val number = PhoneNumberParser.sanitize(rawNumber) ?: return LauncherCommand.OpenMessage(null)
        return LauncherCommand.OpenMessage(number)
    }

    private fun sceneFrom(value: String): SceneId? {
        val withoutPrefix = scenePrefixes.fold(value) { result, prefix ->
            result.removePrefix(prefix).trim()
        }
        return SceneId.entries.firstOrNull { scene ->
            withoutPrefix == scene.name.lowercase(Locale.ROOT) ||
                withoutPrefix == scene.title.normalized()
        }
    }

    private fun String.normalized(): String = Normalizer
        .normalize(lowercase(Locale.GERMAN), Normalizer.Form.NFD)
        .replace("\\p{M}+".toRegex(), "")
        .replace("\\s+".toRegex(), " ")
        .trim()

    private companion object {
        val drawerCommands = setOf(
            "apps",
            "alle apps",
            "app drawer",
            "app-drawer",
            "zeige apps",
        )
        val voiceCommands = setOf(
            "voice",
            "sprache",
            "zuhoren",
            "hor zu",
            "listen",
        )
        val fileCommands = setOf(
            "datei", "dateien", "datei offnen", "datei analysieren", "file", "files",
        )
        val fileWorkspaceCommands = setOf(
            "arbeitsordner", "dateien verwalten", "dateimanager", "file manager", "file workspace",
        )
        val controlCommands = setOf(
            "kontrollzentrum", "schnelleinstellungen", "systemsteuerung", "quick controls",
        )
        val widgetCommands = setOf(
            "widget", "widgets", "widget board", "widget-bereich",
        )
        val faqCommands = setOf(
            "faq", "hilfe", "hilfebereich", "haufige fragen", "häufige fragen",
        )
        val penCommands = setOf(
            "pen space", "penspace", "stift", "smartpen", "notiz", "zeichnen",
        )
        val backupCommands = setOf(
            "backup", "sicherung", "workspace sichern", "backup exportieren", "backup importieren",
        )
        val auditCommands = setOf(
            "audit", "audit log", "aktionsverlauf", "sicherheitsverlauf",
        )
        val proDeskCommands = setOf(
            "pro desk", "prodesk", "kommandozentrale", "professional dashboard",
        )
        val contactCommands = setOf(
            "kontakt", "kontakte", "kontakt auswahlen", "kontakt wählen", "kontakt waehlen",
        )
        val calendarCommands = setOf(
            "kalender", "kalender offnen", "offne kalender", "calendar", "open calendar",
        )
        val alarmCommands = setOf(
            "wecker", "alarme", "wecker offnen", "offne wecker", "alarm", "alarms",
        )
        val cameraCommands = setOf(
            "kamera", "kamera offnen", "offne kamera", "camera", "open camera",
        )
        val systemNoteCommands = setOf(
            "systemnotiz", "android notiz", "system note", "create note",
        )
        val messageCommands = setOf(
            "nachricht", "nachrichten", "sms", "message", "messages",
        )
        val messagePrefixes = listOf(
            "nachricht an ", "sms an ", "message ",
        )
        val phoneCommands = setOf(
            "telefon", "wahler", "dialer", "anrufen", "phone",
        )
        val dialPrefixes = listOf(
            "wahle ", "wähle ", "ruf ", "rufe ", "dial ", "call ",
        )
        val scenePrefixes = listOf(
            "szene ",
            "scene ",
            "offne szene ",
            "wechsle zu ",
        )
        val launchPrefixes = listOf(
            "öffne ",
            "offne ",
            "starte ",
            "open ",
            "launch ",
        )
    }
}

object PhoneNumberParser {
    fun sanitize(value: String): String? {
        val trimmed = value.trim()
        if (trimmed.isBlank()) return null
        val hasLeadingPlus = trimmed.startsWith('+')
        val digits = trimmed.filter(Char::isDigit)
        if (digits.length !in 3..20) return null
        return (if (hasLeadingPlus) "+" else "") + digits
    }
}
