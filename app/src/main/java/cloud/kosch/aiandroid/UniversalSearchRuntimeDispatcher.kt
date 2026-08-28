package cloud.kosch.aiandroid

import android.content.Context
import android.content.Intent
import android.net.Uri
import cloud.kosch.aiandroid.ai.AiHubQuickAction
import cloud.kosch.aiandroid.ai.AiHubQuickActionPolicy
import cloud.kosch.aiandroid.ai.UniversalQueryResult
import cloud.kosch.aiandroid.ai.UniversalSearchExecutionPlan
import cloud.kosch.aiandroid.ai.UniversalSearchExecutionPolicy
import cloud.kosch.aiandroid.model.AppProfile
import cloud.kosch.aiandroid.model.CustomLauncherActionValidation
import cloud.kosch.aiandroid.model.CustomLauncherActionValidator
import cloud.kosch.aiandroid.model.CustomLauncherTarget
import cloud.kosch.aiandroid.model.HomePage
import cloud.kosch.aiandroid.model.LauncherInternalAction
import cloud.kosch.aiandroid.model.LauncherPresentationPlanner
import cloud.kosch.aiandroid.model.SettingsFeatureRegistry
import cloud.kosch.aiandroid.model.SettingsSection
import cloud.kosch.aiandroid.model.SystemPanel
import cloud.kosch.aiandroid.model.WorkspaceMode

/**
 * Resolves Universal Search plans against current runtime state immediately before execution.
 *
 * No search result is trusted as an authorization decision. Apps/shortcuts/actions/settings/pages are looked up again;
 * custom actions are validated again; ambiguous multi-profile package launches fail closed instead of guessing a user.
 */
class UniversalSearchRuntimeDispatcher(
    context: Context,
    private val viewModel: LauncherViewModel,
    private val requestVoiceInput: () -> Unit,
    private val requestDocument: () -> Unit,
) {
    private val appContext = context.applicationContext
    private val controller get() = viewModel.controller

    fun execute(result: UniversalQueryResult.Entity) {
        when (val plan = UniversalSearchExecutionPolicy.plan(result)) {
            is UniversalSearchExecutionPlan.LaunchApp -> launchApp(plan.appKey)
            is UniversalSearchExecutionPlan.LaunchShortcut -> launchShortcut(plan.appKey, plan.shortcutId)
            is UniversalSearchExecutionPlan.OpenFolder -> openFolder(plan.folderId)
            is UniversalSearchExecutionPlan.ActivatePage -> activatePage(plan.pageId)
            is UniversalSearchExecutionPlan.OpenSetting -> openSetting(plan.featureId)
            is UniversalSearchExecutionPlan.ExecuteCustomAction -> executeCustomAction(plan.actionId)
            is UniversalSearchExecutionPlan.OpenAiRoute -> openAiRoute(plan.routeId)
        }
    }

    private fun launchApp(appKey: String) {
        val app = controller.apps.firstOrNull { it.key == appKey }
        if (app == null) {
            stale("App")
            return
        }
        viewModel.universalSearch.close()
        controller.launch(app)
    }

    private fun launchShortcut(appKey: String, shortcutId: String) {
        val app = controller.apps.firstOrNull { it.key == appKey }
        val shortcut = app?.let { owner ->
            controller.appShortcuts.firstOrNull {
                it.id == shortcutId &&
                    it.packageName == owner.packageName &&
                    it.userSerialNumber == owner.userSerialNumber
            }
        }
        if (shortcut == null) {
            stale("App-Shortcut")
            return
        }
        viewModel.universalSearch.close()
        controller.launch(shortcut)
    }

    private fun openFolder(folderId: String) {
        if (controller.folders.none { it.id == folderId }) {
            stale("Ordner")
            return
        }
        viewModel.universalSearch.close()
        controller.openFolder(folderId)
    }

    private fun activatePage(pageId: String) {
        val home = viewModel.homeWorkspace
        if (home.document.pages.none { it.id == pageId }) {
            stale("Home-Seite")
            return
        }
        viewModel.universalSearch.close()
        controller.switchHomePage(HomePage.WORKSPACE)
        home.activatePage(pageId)
    }

    private fun openSetting(featureId: String) {
        val feature = SettingsFeatureRegistry.all.firstOrNull { it.id == featureId }
        if (feature == null) {
            stale("Setting")
            return
        }
        viewModel.universalSearch.close()
        viewModel.settings.open(feature.section)
    }

    private fun openAiRoute(routeId: String) {
        val quickAction = when (routeId) {
            "smart" -> null
            "research" -> AiHubQuickAction.RESEARCH
            "summarize" -> AiHubQuickAction.SUMMARIZE
            "local_private" -> AiHubQuickAction.LOCAL_PRIVATE
            "image" -> AiHubQuickAction.IMAGE
            "voice" -> AiHubQuickAction.VOICE
            "sources" -> AiHubQuickAction.SOURCE_NOTEBOOK
            else -> {
                stale("KI-Route")
                return
            }
        }
        viewModel.universalSearch.close()
        viewModel.openSmartAiHub(
            initialPrompt = quickAction?.let { AiHubQuickActionPolicy.apply(it, "") }.orEmpty(),
        )
    }

    private fun executeCustomAction(actionId: String) {
        val action = viewModel.customActions.find(actionId)
        if (action == null) {
            stale("Eigene Aktion")
            return
        }
        val normalized = when (val validation = CustomLauncherActionValidator.validate(action)) {
            is CustomLauncherActionValidation.Valid -> validation.normalized
            is CustomLauncherActionValidation.Invalid -> {
                controller.postNotice("Eigene Aktion wurde blockiert: ${validation.reason}")
                return
            }
        }
        viewModel.universalSearch.close()
        when (val target = normalized.target) {
            is CustomLauncherTarget.WebUrl -> openValidatedUri(target.url, normalized.name)
            is CustomLauncherTarget.DeepLink -> openValidatedUri(target.uri, normalized.name)
            is CustomLauncherTarget.AppLaunch -> launchPackage(target.packageName, normalized.name)
            is CustomLauncherTarget.Internal -> executeInternal(target.action)
        }
    }

    private fun openValidatedUri(uri: String, title: String) {
        runCatching {
            appContext.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(uri)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }.onSuccess {
            controller.postNotice("$title geöffnet")
        }.onFailure {
            controller.postNotice("$title ist auf diesem Gerät aktuell nicht startbar")
        }
    }

    private fun launchPackage(packageName: String, title: String) {
        val matches = controller.apps.filter { it.packageName == packageName }
        val app = when (matches.size) {
            0 -> null
            1 -> matches.single()
            else -> matches.singleOrNull { it.profile == AppProfile.PERSONAL }
        }
        if (app == null) {
            controller.postNotice(
                if (matches.isEmpty()) "$title: App ist nicht installiert" else
                    "$title: mehrere Profile gefunden – Ziel muss eindeutig gewählt werden",
            )
            return
        }
        controller.launch(app)
    }

    private fun executeInternal(action: LauncherInternalAction) {
        when (action) {
            LauncherInternalAction.OPEN_APPS -> controller.openDrawer()
            LauncherInternalAction.OPEN_SEARCH,
            LauncherInternalAction.OPEN_COMMAND_PALETTE -> viewModel.openUniversalSearch()
            LauncherInternalAction.OPEN_HOME_STUDIO -> {
                controller.switchHomePage(HomePage.WORKSPACE)
                controller.selectWorkspaceMode(WorkspaceMode.EDIT)
                controller.postNotice("Home Studio bereit · Drag/Resize/Stil")
            }
            LauncherInternalAction.OPEN_SETTINGS -> viewModel.settings.open()
            LauncherInternalAction.OPEN_ASSISTANT -> {
                if (viewModel.assistant.settings.enabled) requestVoiceInput()
                else viewModel.settings.open(SettingsSection.ASSISTANT)
            }
            LauncherInternalAction.OPEN_NOTIFICATIONS -> controller.openSystemPanel(SystemPanel.NOTIFICATIONS)
            LauncherInternalAction.OPEN_PEN_SPACE -> controller.openPenSpace()
            LauncherInternalAction.OPEN_FILES -> requestDocument()
            LauncherInternalAction.OPEN_BACKUP -> controller.openBackup()
            LauncherInternalAction.OPEN_AUDIT -> controller.openAudit()
            LauncherInternalAction.PREVIOUS_PAGE -> movePage(-1)
            LauncherInternalAction.NEXT_PAGE -> movePage(1)
        }
    }

    private fun movePage(direction: Int) {
        val home = viewModel.homeWorkspace
        val pages = home.document.pages
        val currentIndex = pages.indexOfFirst { it.id == home.document.activePageId }
        val nextIndex = LauncherPresentationPlanner.adjacentPageIndex(
            settings = viewModel.settings.document.pages,
            currentIndex = currentIndex,
            pageCount = pages.size,
            direction = direction,
        )
        if (nextIndex !in pages.indices || nextIndex == currentIndex) {
            controller.postNotice("Keine weitere Home-Seite in dieser Richtung")
            return
        }
        controller.switchHomePage(HomePage.WORKSPACE)
        home.activatePage(pages[nextIndex].id)
    }

    private fun stale(kind: String) {
        viewModel.universalSearch.refresh()
        controller.postNotice("$kind hat sich geändert – Suchergebnisse wurden aktualisiert")
    }
}
