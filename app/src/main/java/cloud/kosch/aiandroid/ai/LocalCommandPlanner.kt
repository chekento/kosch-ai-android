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

/**
 * Deterministic local command interpreter used before any AI/provider route.
 *
 * It deliberately understands common conversational wrappers (wake words, politeness, "open/show" verbs) while
 * keeping the executable vocabulary closed. Unknown text still becomes RoutePrompt and therefore passes through the
 * normal Smart AI routing and disclosure gates instead of being guessed as an Android action.
 */
class LocalCommandPlanner {
    fun plan(input: String): LauncherCommand {
        val raw = cleanSurfaceText(input)
        if (raw.isEmpty()) return LauncherCommand.Empty

        val normalized = raw.normalized()
        val semantic = stripActionVerb(normalized)

        if (matches(normalized, semantic, drawerCommands)) return LauncherCommand.OpenDrawer
        if (matches(normalized, semantic, voiceCommands)) return LauncherCommand.StartVoice
        if (matches(normalized, semantic, fileWorkspaceCommands)) return LauncherCommand.OpenFileWorkspace
        if (matches(normalized, semantic, fileCommands)) return LauncherCommand.OpenFiles
        if (matches(normalized, semantic, controlCommands)) return LauncherCommand.OpenControls
        if (matches(normalized, semantic, widgetCommands)) return LauncherCommand.OpenWidgets
        if (matches(normalized, semantic, faqCommands)) return LauncherCommand.OpenFaq
        if (matches(normalized, semantic, penCommands)) return LauncherCommand.OpenPenSpace
        if (matches(normalized, semantic, backupCommands)) return LauncherCommand.OpenBackup
        if (matches(normalized, semantic, auditCommands)) return LauncherCommand.OpenAudit
        if (matches(normalized, semantic, proDeskCommands)) return LauncherCommand.OpenProDesk
        if (matches(normalized, semantic, contactCommands)) return LauncherCommand.PickContact
        if (matches(normalized, semantic, calendarCommands)) return LauncherCommand.OpenCalendar
        if (matches(normalized, semantic, alarmCommands)) return LauncherCommand.OpenAlarms
        if (matches(normalized, semantic, cameraCommands)) return LauncherCommand.OpenCamera
        if (matches(normalized, semantic, systemNoteCommands)) return LauncherCommand.CreateSystemNote

        systemPanelFrom(semantic)?.let { return LauncherCommand.OpenSystemPanel(it) }
        systemPanelFrom(normalized)?.let { return LauncherCommand.OpenSystemPanel(it) }
        messageFrom(raw, normalized)?.let { return it }
        phoneFrom(raw, normalized)?.let { return it }
        sceneFrom(normalized)?.let { return LauncherCommand.SwitchScene(it) }

        launchPrefixes.firstOrNull { normalized.startsWith(it) }?.let { prefix ->
            val query = raw.drop(prefix.length).trim()
            if (query.isNotEmpty()) return LauncherCommand.LaunchApp(query)
        }

        explicitAppPrefixes.firstOrNull { normalized.startsWith(it) }?.let { prefix ->
            val query = raw.drop(prefix.length).trim()
            if (query.isNotEmpty()) return LauncherCommand.LaunchApp(query)
        }

        promptPrefixes.firstOrNull { normalized.startsWith(it) }?.let { prefix ->
            val prompt = raw.drop(prefix.length).trim()
            if (prompt.isNotEmpty()) return LauncherCommand.RoutePrompt(prompt)
        }

        return LauncherCommand.RoutePrompt(raw)
    }

    private fun matches(normalized: String, semantic: String, commands: Set<String>): Boolean =
        normalized in commands || semantic in commands

    private fun systemPanelFrom(value: String): SystemPanel? = when (value) {
        "wlan", "wifi", "wi-fi", "wlan einstellungen", "wifi einstellungen" -> SystemPanel.WIFI
        "bluetooth", "bluetooth einstellungen" -> SystemPanel.BLUETOOTH
        "benachrichtigungen", "notification settings", "notifications" -> SystemPanel.NOTIFICATIONS
        "benachrichtigungspunkte", "notification dots", "app punkte" -> SystemPanel.NOTIFICATION_ACCESS
        "hintergrund", "hintergrundbild", "wallpaper" -> SystemPanel.WALLPAPER
        "anzeige", "display", "bildschirm" -> SystemPanel.DISPLAY
        "ton", "sound", "lautstarke", "lautstärke", "audio" -> SystemPanel.SOUND
        "akku", "batterie", "battery" -> SystemPanel.BATTERY
        "datenschutz", "privacy" -> SystemPanel.PRIVACY
        "bedienungshilfen", "barrierefreiheit", "accessibility" -> SystemPanel.ACCESSIBILITY
        "standard apps", "standard-apps", "default apps" -> SystemPanel.DEFAULT_APPS
        "speicher", "storage" -> SystemPanel.STORAGE
        "android einstellungen", "systemeinstellungen", "settings", "einstellungen" -> SystemPanel.ANDROID_SETTINGS
        "home auswahl", "launcher auswahl", "start app auswahl", "standard launcher", "home app" -> SystemPanel.HOME_SELECTION
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
        val candidate = sceneSuffixes.fold(withoutPrefix) { result, suffix ->
            result.removeSuffix(suffix).trim()
        }
        return SceneId.entries.firstOrNull { scene ->
            candidate == scene.name.lowercase(Locale.ROOT) || candidate == scene.title.normalized()
        }
    }

    private fun stripActionVerb(value: String): String {
        val prefix = semanticActionPrefixes.firstOrNull { value.startsWith(it) } ?: return value
        return value.removePrefix(prefix).trim()
    }

    private fun cleanSurfaceText(input: String): String = input
        .trim()
        .replaceFirst(WAKE_PREFIX_REGEX, "")
        .replace(TRAILING_POLITENESS_REGEX, "")
        .trim()
        .trimEnd('.', '!', '?', ',', ';', ':')
        .trim()

    private fun String.normalized(): String = Normalizer
        .normalize(lowercase(Locale.GERMAN), Normalizer.Form.NFD)
        .replace("\\p{M}+".toRegex(), "")
        .replace("[.!?,;:]+".toRegex(), " ")
        .replace("\\s+".toRegex(), " ")
        .trim()

    private companion object {
        val WAKE_PREFIX_REGEX = Regex("^(?i)(?:hey\\s+)?(?:computer|kosch)[,:]?\\s+")
        val TRAILING_POLITENESS_REGEX = Regex("(?i)\\s+(?:bitte|please)[.!?]*$")

        val semanticActionPrefixes = listOf(
            "offne ", "zeige ", "starte ", "gehe zu ",
            "open ", "show ", "launch ", "go to ",
        )
        val drawerCommands = setOf(
            "apps", "alle apps", "app drawer", "app-drawer", "zeige apps", "anwendungen",
        )
        val voiceCommands = setOf(
            "voice", "sprache", "zuhoren", "hor zu", "listen", "sprachmodus", "voice mode",
        )
        val fileCommands = setOf(
            "datei", "dateien", "datei offnen", "datei analysieren", "file", "files", "dokument auswahlen",
        )
        val fileWorkspaceCommands = setOf(
            "arbeitsordner", "dateien verwalten", "dateimanager", "file manager", "file workspace", "datei workspace",
        )
        val controlCommands = setOf(
            "kontrollzentrum", "schnelleinstellungen", "systemsteuerung", "quick controls", "control center",
        )
        val widgetCommands = setOf(
            "widget", "widgets", "widget board", "widget-bereich", "widget board offnen",
        )
        val faqCommands = setOf(
            "faq", "hilfe", "hilfebereich", "haufige fragen", "häufige fragen", "help",
        )
        val penCommands = setOf(
            "pen space", "penspace", "stift", "smartpen", "notiz", "zeichnen", "skizze", "canvas",
        )
        val backupCommands = setOf(
            "backup", "sicherung", "workspace sichern", "backup exportieren", "backup importieren", "wiederherstellung",
        )
        val auditCommands = setOf(
            "audit", "audit log", "aktionsverlauf", "sicherheitsverlauf", "audit protokoll",
        )
        val proDeskCommands = setOf(
            "pro desk", "prodesk", "kommandozentrale", "professional dashboard", "power desk",
        )
        val contactCommands = setOf(
            "kontakt", "kontakte", "kontakt auswahlen", "kontakt wählen", "kontakt waehlen", "contact picker",
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
            "systemnotiz", "android notiz", "system note", "create note", "neue notiz",
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
            "szene ", "scene ", "offne szene ", "wechsle zu ", "workspace ", "profil ",
        )
        val sceneSuffixes = listOf(
            " modus", " mode", " workspace", " profil", " profile",
        )
        val launchPrefixes = listOf(
            "öffne ", "offne ", "starte ", "open ", "launch ",
        )
        val explicitAppPrefixes = listOf(
            "app ", "app: ",
        )
        val promptPrefixes = listOf(
            "frage ", "frag ", "ask ", "ai ", "ki ",
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
