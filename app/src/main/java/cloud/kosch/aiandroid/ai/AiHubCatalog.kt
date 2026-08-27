package cloud.kosch.aiandroid.ai

import cloud.kosch.aiandroid.model.LaunchableApp

enum class AiHubEntryKind(val title: String) {
    LLM_APP("KI-App"),
    LOCAL_LLM_APP("Lokale KI"),
    BROWSER("Browser"),
    SYSTEM_BROWSER("Systembrowser"),
}

enum class AiHubInstallState { INSTALLED, STORE_AVAILABLE, WEB_ONLY, SYSTEM_AVAILABLE, UNAVAILABLE }

data class AiHubEntry(
    val stableId: String,
    val title: String,
    val subtitle: String,
    val kind: AiHubEntryKind,
    val installState: AiHubInstallState,
    val installedApp: LaunchableApp? = null,
    val playStorePackageName: String? = null,
    val webUrl: String? = null,
    val aiCapabilities: Set<String> = emptySet(),
    val aiStatusLabel: String? = null,
    val dismissible: Boolean = true,
)

/** Pure projection used by UI, search, recommendation and assistant-routing surfaces. */
object AiHubCatalog {
    fun entries(
        apps: List<LaunchableApp>,
        hiddenSuggestionIds: Set<String> = emptySet(),
    ): List<AiHubEntry> = buildList {
        AiProviderRegistry.providers.forEach { provider ->
            val stableId = "ai:${provider.id}"
            if (stableId in hiddenSuggestionIds) return@forEach
            val installed = AiProviderRegistry.installedApp(provider, apps)
            val state = when {
                installed != null -> AiHubInstallState.INSTALLED
                provider.playStorePackageName != null -> AiHubInstallState.STORE_AVAILABLE
                provider.webUrl.isNotBlank() -> AiHubInstallState.WEB_ONLY
                else -> AiHubInstallState.UNAVAILABLE
            }
            add(
                AiHubEntry(
                    stableId = stableId,
                    title = provider.name,
                    subtitle = provider.description,
                    kind = if (provider.kind == AiProviderKind.LOCAL_OPEN_SOURCE) {
                        AiHubEntryKind.LOCAL_LLM_APP
                    } else {
                        AiHubEntryKind.LLM_APP
                    },
                    installState = state,
                    installedApp = installed,
                    playStorePackageName = provider.playStorePackageName,
                    webUrl = provider.webUrl,
                    aiCapabilities = provider.capabilities.mapTo(linkedSetOf()) { it.title },
                    aiStatusLabel = provider.kind.title,
                ),
            )
        }

        BrowserProviderRegistry.browsers.forEach { browser ->
            val stableId = "browser:${browser.id}"
            if (stableId in hiddenSuggestionIds) return@forEach
            val installed = BrowserProviderRegistry.installedApp(browser, apps)
            val state = when {
                browser.isSystemDefaultRoute -> AiHubInstallState.SYSTEM_AVAILABLE
                installed != null -> AiHubInstallState.INSTALLED
                browser.playStorePackageName != null -> AiHubInstallState.STORE_AVAILABLE
                browser.webUrl != null -> AiHubInstallState.WEB_ONLY
                else -> AiHubInstallState.UNAVAILABLE
            }
            add(
                AiHubEntry(
                    stableId = stableId,
                    title = browser.name,
                    subtitle = browser.notes.ifBlank { browser.aiStatus.title },
                    kind = if (browser.isSystemDefaultRoute) {
                        AiHubEntryKind.SYSTEM_BROWSER
                    } else {
                        AiHubEntryKind.BROWSER
                    },
                    installState = state,
                    installedApp = installed,
                    playStorePackageName = browser.playStorePackageName,
                    webUrl = browser.webUrl,
                    aiCapabilities = browser.aiCapabilities.mapTo(linkedSetOf()) { it.title },
                    aiStatusLabel = browser.aiStatus.title,
                ),
            )
        }
    }
}
