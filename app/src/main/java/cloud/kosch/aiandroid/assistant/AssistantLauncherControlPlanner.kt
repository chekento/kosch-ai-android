package cloud.kosch.aiandroid.assistant

import cloud.kosch.aiandroid.model.AssistantAnchor
import cloud.kosch.aiandroid.model.LabelMode
import cloud.kosch.aiandroid.model.MotionProfile
import cloud.kosch.aiandroid.model.SettingsSection
import cloud.kosch.aiandroid.model.ThemeMode
import java.text.Normalizer
import java.util.Locale

sealed interface AssistantLauncherControlAction {
    data class SetThemeMode(val mode: ThemeMode) : AssistantLauncherControlAction
    data class SetMaterialYou(val enabled: Boolean) : AssistantLauncherControlAction
    data class SetMotionProfile(val profile: MotionProfile) : AssistantLauncherControlAction
    data class SetDockEnabled(val enabled: Boolean) : AssistantLauncherControlAction
    data class SetDockAskEnabled(val enabled: Boolean) : AssistantLauncherControlAction
    data class SetDockItemLimit(val count: Int) : AssistantLauncherControlAction
    data class SetPageIndicator(val enabled: Boolean) : AssistantLauncherControlAction
    data class SetHomeLabels(val mode: LabelMode) : AssistantLauncherControlAction
    data class AdjustHomeIconScale(val delta: Float) : AssistantLauncherControlAction
    data class SetAssistantAnchor(val anchor: AssistantAnchor) : AssistantLauncherControlAction
    data class OpenSettings(val section: SettingsSection) : AssistantLauncherControlAction
    data object DescribeAppearance : AssistantLauncherControlAction
    data object DescribeDock : AssistantLauncherControlAction
    data object UndoLastSettingsChange : AssistantLauncherControlAction
}

/**
 * Closed-vocabulary parser for safe, local launcher customisation from Assistant chat/voice.
 * It intentionally handles only settings whose runtime contract is already explicit and reversible.
 */
class AssistantLauncherControlPlanner {
    fun plan(input: String): AssistantLauncherControlAction? {
        val normalized = input.normalized()
        if (normalized.isBlank()) return null

        if (normalized in undoPhrases) return AssistantLauncherControlAction.UndoLastSettingsChange

        if (asksForAppearanceStatus(normalized)) return AssistantLauncherControlAction.DescribeAppearance
        if (asksForDockStatus(normalized)) return AssistantLauncherControlAction.DescribeDock

        if (mentionsTheme(normalized)) {
            when {
                containsAny(normalized, "dunkel", "dark", "nachtmodus", "nacht modus") ->
                    return AssistantLauncherControlAction.SetThemeMode(ThemeMode.DARK)
                containsAny(normalized, "hell", "light", "tagmodus", "tag modus") ->
                    return AssistantLauncherControlAction.SetThemeMode(ThemeMode.LIGHT)
                containsAny(normalized, "system", "automatisch", "automatic") ->
                    return AssistantLauncherControlAction.SetThemeMode(ThemeMode.SYSTEM)
            }
        }

        if (containsAny(normalized, "material you", "dynamische farben", "dynamic color", "dynamic colors")) {
            parseOnOff(normalized)?.let { return AssistantLauncherControlAction.SetMaterialYou(it) }
        }

        if (containsAny(normalized, "animationen", "bewegung", "motion", "effekte")) {
            when {
                containsAny(normalized, "aus", "off", "keine", "abschalten") ->
                    return AssistantLauncherControlAction.SetMotionProfile(MotionProfile.OFF)
                containsAny(normalized, "reduziert", "reduzieren", "weniger", "reduced") ->
                    return AssistantLauncherControlAction.SetMotionProfile(MotionProfile.REDUCED)
                containsAny(normalized, "expressiv", "mehr", "stark", "expressive") ->
                    return AssistantLauncherControlAction.SetMotionProfile(MotionProfile.EXPRESSIVE)
                containsAny(normalized, "normal", "balanced", "ausgeglichen") ->
                    return AssistantLauncherControlAction.SetMotionProfile(MotionProfile.BALANCED)
            }
        }

        if (containsAny(normalized, "ask button", "ask-knopf", "ask knopf", "ask taste", "ask im dock")) {
            parseOnOff(normalized)?.let { return AssistantLauncherControlAction.SetDockAskEnabled(it) }
        }

        if (containsAny(normalized, "dock", "schnellzugriff")) {
            dockLimit(normalized)?.let { return AssistantLauncherControlAction.SetDockItemLimit(it) }
            parseOnOff(normalized)?.let { return AssistantLauncherControlAction.SetDockEnabled(it) }
            if (opensSettings(normalized)) return AssistantLauncherControlAction.OpenSettings(SettingsSection.DOCK)
        }

        if (containsAny(normalized, "seitenpunkte", "seitenindikator", "page indicator", "page dots")) {
            parseOnOff(normalized)?.let { return AssistantLauncherControlAction.SetPageIndicator(it) }
        }

        if (containsAny(normalized, "app namen", "app-namen", "beschriftungen", "labels", "icon labels")) {
            when {
                hasAnyToken(normalized, "nie", "never", "aus", "off") ->
                    return AssistantLauncherControlAction.SetHomeLabels(LabelMode.NEVER)
                containsAny(normalized, "smart", "automatisch", "automatic") ->
                    return AssistantLauncherControlAction.SetHomeLabels(LabelMode.SMART)
                hasAnyToken(normalized, "immer", "always", "an", "ein") ->
                    return AssistantLauncherControlAction.SetHomeLabels(LabelMode.ALWAYS)
            }
        }

        if (containsAny(normalized, "icons", "symbole", "app symbole", "app-symbole")) {
            when {
                containsAny(normalized, "grosser", "groesser", "mehr", "bigger") ->
                    return AssistantLauncherControlAction.AdjustHomeIconScale(+0.10f)
                containsAny(normalized, "kleiner", "weniger", "smaller") ->
                    return AssistantLauncherControlAction.AdjustHomeIconScale(-0.10f)
            }
        }

        if (containsAny(normalized, "assistent", "assistant", "avatar")) {
            when {
                hasAnyToken(normalized, "links", "left") ->
                    return AssistantLauncherControlAction.SetAssistantAnchor(AssistantAnchor.LEFT)
                hasAnyToken(normalized, "mitte", "zentriert", "center", "centre") ->
                    return AssistantLauncherControlAction.SetAssistantAnchor(AssistantAnchor.CENTER)
                hasAnyToken(normalized, "rechts", "right") ->
                    return AssistantLauncherControlAction.SetAssistantAnchor(AssistantAnchor.RIGHT)
            }
            if (opensSettings(normalized)) return AssistantLauncherControlAction.OpenSettings(SettingsSection.ASSISTANT)
        }

        if (containsAny(normalized, "theme einstellungen", "darstellung einstellungen", "appearance settings")) {
            return AssistantLauncherControlAction.OpenSettings(SettingsSection.APPEARANCE)
        }
        if (containsAny(normalized, "ki einstellungen", "ai settings", "modelle einstellungen")) {
            return AssistantLauncherControlAction.OpenSettings(SettingsSection.AI)
        }

        return null
    }

    private fun asksForAppearanceStatus(value: String): Boolean =
        containsAny(value, "wie ist meine darstellung", "welches theme", "welcher theme modus", "darstellungsstatus")

    private fun asksForDockStatus(value: String): Boolean =
        containsAny(value, "wie ist mein dock", "dock status", "dock einstellung") &&
            containsAny(value, "wie", "status", "eingestellt")

    private fun mentionsTheme(value: String): Boolean = containsAny(
        value,
        "theme",
        "darstellung",
        "modus",
        "oberflache",
        "farbschema",
    )

    private fun opensSettings(value: String): Boolean =
        containsAny(value, "einstellungen", "settings", "konfigurieren", "konfiguration")

    private fun dockLimit(value: String): Int? {
        val match = DOCK_LIMIT_REGEX.find(value) ?: return null
        return match.groupValues[1].toIntOrNull()?.coerceIn(0, 12)
    }

    private fun parseOnOff(value: String): Boolean? = when {
        containsAny(value, " ausschalten", " abschalten", " deaktivieren", " verstecken") ||
            hasAnyToken(value, "aus", "off") -> false
        containsAny(value, " einschalten", " aktivieren", " zeigen", " einblenden") ||
            hasAnyToken(value, "an", "on") -> true
        else -> null
    }

    private fun hasAnyToken(value: String, vararg tokens: String): Boolean {
        val words = value.split(' ', '-').filter(String::isNotBlank).toSet()
        return tokens.any(words::contains)
    }

    private fun containsAny(value: String, vararg needles: String): Boolean = needles.any(value::contains)

    private fun String.normalized(): String = Normalizer
        .normalize(lowercase(Locale.GERMAN), Normalizer.Form.NFD)
        .replace("\\p{M}+".toRegex(), "")
        .replace("[^a-z0-9ß -]".toRegex(), " ")
        .replace("\\s+".toRegex(), " ")
        .trim()

    private companion object {
        val undoPhrases = setOf(
            "ruckgangig",
            "mach das ruckgangig",
            "letzte anderung ruckgangig",
            "undo",
            "undo settings",
        )
        val DOCK_LIMIT_REGEX = Regex("(?:dock|schnellzugriff).*?(?:auf|mit)?\\s*(\\d{1,2})\\s*(?:apps|icons|symbole|platze|slots)?")
    }
}
