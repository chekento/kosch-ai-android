package cloud.kosch.aiandroid.assistant

import cloud.kosch.aiandroid.LauncherSettingsController
import cloud.kosch.aiandroid.WorkspaceHomeController
import cloud.kosch.aiandroid.model.AssistantAnchor
import cloud.kosch.aiandroid.model.LabelMode
import cloud.kosch.aiandroid.model.MotionProfile
import cloud.kosch.aiandroid.model.ThemeMode

/**
 * Applies the small reversible launcher-control vocabulary planned by [AssistantLauncherControlPlanner].
 * No network access, Android permission grant, provider connection, capture session or destructive workspace mutation
 * can be initiated through this engine.
 */
class AssistantLauncherControlEngine(
    private val settings: LauncherSettingsController,
    private val home: WorkspaceHomeController,
    private val planner: AssistantLauncherControlPlanner = AssistantLauncherControlPlanner(),
) {
    fun handle(input: String): String? {
        val action = planner.plan(input) ?: return null
        return when (action) {
            is AssistantLauncherControlAction.SetThemeMode -> {
                val ok = settings.applyAppearance(settings.document.appearance.copy(mode = action.mode))
                result(ok, "Darstellung auf ${action.mode.userLabel()} gestellt.")
            }
            is AssistantLauncherControlAction.SetMaterialYou -> {
                val ok = settings.applyAppearance(
                    settings.document.appearance.copy(useMaterialYouAccents = action.enabled),
                )
                result(ok, "Material-You-Farben ${action.enabled.onOffLabel()}.")
            }
            is AssistantLauncherControlAction.SetMotionProfile -> {
                val ok = settings.applyAppearance(
                    settings.document.appearance.copy(motionProfile = action.profile),
                )
                result(ok, "Bewegungsprofil auf ${action.profile.userLabel()} gestellt.")
            }
            is AssistantLauncherControlAction.SetDockEnabled -> {
                val ok = settings.applyDock(settings.document.dock.copy(enabled = action.enabled))
                result(ok, "Dock ${action.enabled.onOffLabel()}.")
            }
            is AssistantLauncherControlAction.SetDockAskEnabled -> {
                val ok = settings.applyDock(settings.document.dock.copy(showAskButton = action.enabled))
                result(ok, "Ask im Dock ${action.enabled.onOffLabel()}.")
            }
            is AssistantLauncherControlAction.SetDockItemLimit -> {
                val count = action.count.coerceIn(0, 12)
                val ok = settings.applyDock(settings.document.dock.copy(maxItems = count))
                result(ok, "Dock auf maximal $count App${if (count == 1) "" else "s"} gestellt.")
            }
            is AssistantLauncherControlAction.SetPageIndicator -> {
                val ok = settings.applyHome(
                    settings.document.home.copy(showPageIndicator = action.enabled),
                    home,
                )
                result(ok, "Seitenindikator ${action.enabled.onOffLabel()}.")
            }
            is AssistantLauncherControlAction.SetHomeLabels -> {
                val ok = settings.applyHome(settings.document.home.copy(labelMode = action.mode), home)
                result(ok, "App-Beschriftungen auf ${action.mode.userLabel()} gestellt.")
            }
            is AssistantLauncherControlAction.AdjustHomeIconScale -> {
                val next = (settings.document.home.iconScale + action.delta).coerceIn(0.5f, 1.75f)
                val ok = settings.applyHome(settings.document.home.copy(iconScale = next), home)
                result(ok, "Home-Icons auf ${(next * 100).toInt()} % gestellt.")
            }
            is AssistantLauncherControlAction.SetAssistantAnchor -> {
                val ok = settings.applyAssistant(settings.document.assistant.copy(anchor = action.anchor))
                result(ok, "Assistant-Position auf ${action.anchor.userLabel()} gestellt.")
            }
            is AssistantLauncherControlAction.OpenSettings -> {
                settings.open(action.section)
                "Ich öffne ${action.section.title}. Nur weil du das ausdrücklich angefordert hast."
            }
            AssistantLauncherControlAction.DescribeAppearance -> describeAppearance()
            AssistantLauncherControlAction.DescribeDock -> describeDock()
            AssistantLauncherControlAction.UndoLastSettingsChange -> {
                if (settings.undo(home)) {
                    "Die letzte Launcher-Einstellungsänderung ist rückgängig gemacht."
                } else {
                    "Es gibt gerade keine Launcher-Einstellungsänderung zum Rückgängigmachen."
                }
            }
        }
    }

    private fun describeAppearance(): String {
        val appearance = settings.document.appearance
        val homeSettings = settings.document.home
        return "Darstellung: ${appearance.mode.userLabel()}, Bewegung ${appearance.motionProfile.userLabel()}, " +
            "Material You ${appearance.useMaterialYouAccents.onOffLabel()}, Icons ${(homeSettings.iconScale * 100).toInt()} %."
    }

    private fun describeDock(): String {
        val dock = settings.document.dock
        return if (!dock.enabled) {
            "Das Dock ist aus. Ask im Dock ist ${dock.showAskButton.onOffLabel()} und das gespeicherte Slot-Limit ist ${dock.maxItems}."
        } else {
            "Das Dock ist an, zeigt bis zu ${dock.maxItems} Apps und Ask ist ${dock.showAskButton.onOffLabel()}. " +
                "Adaptive Vorschläge sind ${dock.adaptiveSuggestions.onOffLabel()}."
        }
    }

    private fun result(success: Boolean, message: String): String =
        if (success) message else settings.notice ?: "Die Änderung konnte nicht gespeichert werden."

    private fun Boolean.onOffLabel(): String = if (this) "an" else "aus"

    private fun ThemeMode.userLabel(): String = when (this) {
        ThemeMode.SYSTEM -> "System"
        ThemeMode.LIGHT -> "Hell"
        ThemeMode.DARK -> "Dunkel"
        ThemeMode.THEME_DEFINED -> "Theme-Vorgabe"
    }

    private fun MotionProfile.userLabel(): String = when (this) {
        MotionProfile.OFF -> "Aus"
        MotionProfile.REDUCED -> "Reduziert"
        MotionProfile.BALANCED -> "Ausgeglichen"
        MotionProfile.EXPRESSIVE -> "Expressiv"
    }

    private fun LabelMode.userLabel(): String = when (this) {
        LabelMode.ALWAYS -> "Immer"
        LabelMode.SMART -> "Smart"
        LabelMode.NEVER -> "Nie"
    }

    private fun AssistantAnchor.userLabel(): String = when (this) {
        AssistantAnchor.LEFT -> "Links"
        AssistantAnchor.CENTER -> "Mitte"
        AssistantAnchor.RIGHT -> "Rechts"
        AssistantAnchor.FREE -> "Frei"
    }
}
