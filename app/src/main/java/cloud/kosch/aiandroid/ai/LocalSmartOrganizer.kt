package cloud.kosch.aiandroid.ai

import cloud.kosch.aiandroid.model.FolderKind
import cloud.kosch.aiandroid.model.LauncherFolder
import cloud.kosch.aiandroid.model.SceneId
import java.util.Locale

data class SmartAppDescriptor(
    val key: String,
    val label: String,
    val packageName: String,
)

/**
 * A deterministic, offline organizer used before an optional LLM is installed.
 * It only sees launcher labels, package names and local recency metadata.
 */
object LocalSmartOrganizer {
    fun proposeFolders(apps: List<SmartAppDescriptor>): List<LauncherFolder> {
        val grouped = apps.groupBy(::kindFor)
        val preferredOrder = listOf(
            FolderKind.COMMUNICATION,
            FolderKind.WORK,
            FolderKind.MEDIA,
            FolderKind.TOOLS,
            FolderKind.AI,
            FolderKind.OTHER,
        )
        return preferredOrder.mapNotNull { kind ->
            val matches = grouped[kind].orEmpty()
                .sortedBy { it.label.lowercase(Locale.ROOT) }
                .take(MAX_FOLDER_APPS)
            if (matches.isEmpty()) return@mapNotNull null
            LauncherFolder(
                id = "local-${kind.name.lowercase(Locale.ROOT)}",
                title = kind.title,
                kind = kind,
                appKeys = matches.map(SmartAppDescriptor::key),
            )
        }
    }

    fun bestFolderKind(app: SmartAppDescriptor): FolderKind = kindFor(app)

    fun smartDockKeys(
        apps: List<SmartAppDescriptor>,
        pinnedKeys: List<String>,
        recentPackages: List<String>,
        usageKeys: List<String> = emptyList(),
        scene: SceneId,
        limit: Int = DEFAULT_DOCK_SIZE,
    ): List<String> {
        if (limit <= 0) return emptyList()
        val byKey = apps.associateBy(SmartAppDescriptor::key)
        val availablePinned = pinnedKeys.filter(byKey::containsKey)
        val recent = recentPackages.mapNotNull { packageName ->
            apps.firstOrNull { it.packageName == packageName }
        }
        val learned = usageKeys.mapNotNull(byKey::get)
        val preferredKinds = when (scene) {
            SceneId.WORK -> listOf(FolderKind.WORK, FolderKind.TOOLS, FolderKind.COMMUNICATION)
            SceneId.STUDIO -> listOf(FolderKind.MEDIA, FolderKind.AI, FolderKind.TOOLS)
            SceneId.SOCIAL -> listOf(FolderKind.COMMUNICATION, FolderKind.MEDIA, FolderKind.AI)
            SceneId.EVENING -> listOf(FolderKind.MEDIA, FolderKind.COMMUNICATION, FolderKind.OTHER)
            SceneId.AI -> listOf(FolderKind.AI, FolderKind.TOOLS, FolderKind.COMMUNICATION)
        }
        val contextual = preferredKinds.flatMap { preferred ->
            apps.filter { kindFor(it) == preferred }
                .sortedBy { it.label.lowercase(Locale.ROOT) }
        }
        return (availablePinned.mapNotNull(byKey::get) + recent + learned + contextual + apps)
            .distinctBy(SmartAppDescriptor::key)
            .take(limit)
            .map(SmartAppDescriptor::key)
    }

    private fun kindFor(app: SmartAppDescriptor): FolderKind {
        val haystack = "${app.label} ${app.packageName}".lowercase(Locale.ROOT)
        return when {
            aiTokens.any(haystack::contains) -> FolderKind.AI
            communicationTokens.any(haystack::contains) -> FolderKind.COMMUNICATION
            workTokens.any(haystack::contains) -> FolderKind.WORK
            mediaTokens.any(haystack::contains) -> FolderKind.MEDIA
            toolTokens.any(haystack::contains) -> FolderKind.TOOLS
            else -> FolderKind.OTHER
        }
    }

    private val aiTokens = setOf(
        "anthropic", "chatgpt", "chatterui", "claude", "gemini", "grok",
        "llama", "maid", "notebooklm", "ollama", "openai", "perplexity", "pocketpal",
    )
    private val communicationTokens = setOf(
        "chat", "contact", "discord", "mail", "message", "phone", "signal", "telegram", "whatsapp",
    )
    private val workTokens = setOf(
        "calendar", "docs", "drive", "office", "notion", "outlook", "sheets", "slack", "teams", "trello", "work",
    )
    private val mediaTokens = setOf(
        "audio", "camera", "gallery", "music", "photo", "spotify", "video", "youtube",
    )
    private val toolTokens = setOf(
        "authenticator", "calculator", "clock", "file", "scanner", "settings", "tool",
    )

    private const val MAX_FOLDER_APPS = 16
    private const val DEFAULT_DOCK_SIZE = 5
}
