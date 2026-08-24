package cloud.kosch.aiandroid.data

import cloud.kosch.aiandroid.model.FolderKind
import cloud.kosch.aiandroid.model.LauncherFolder

/** Pure, bounded transformations for user-managed launcher collections. */
object WorkspaceCollectionEditor {
    const val MAX_TITLE_LENGTH = 40
    const val MAX_FOLDERS = 12
    const val MAX_APPS_PER_FOLDER = 32

    fun normalizedTitle(value: String): String? = value
        .trim()
        .replace("\\s+".toRegex(), " ")
        .take(MAX_TITLE_LENGTH)
        .takeIf(String::isNotBlank)

    fun create(
        folders: List<LauncherFolder>,
        id: String,
        title: String,
        kind: FolderKind,
    ): List<LauncherFolder> {
        val safeTitle = normalizedTitle(title) ?: return folders
        if (id.isBlank() || folders.size >= MAX_FOLDERS || folders.any { it.id == id }) return folders
        return folders + LauncherFolder(
            id = id,
            title = safeTitle,
            kind = kind,
            appKeys = emptyList(),
            generatedLocally = false,
        )
    }

    fun rename(
        folders: List<LauncherFolder>,
        folderId: String,
        title: String,
    ): List<LauncherFolder> {
        val safeTitle = normalizedTitle(title) ?: return folders
        return folders.map { folder ->
            if (folder.id == folderId) folder.copy(title = safeTitle, generatedLocally = false) else folder
        }
    }

    fun addApp(
        folders: List<LauncherFolder>,
        folderId: String,
        appKey: String,
    ): List<LauncherFolder> = folders.map { folder ->
        if (folder.id == folderId && appKey.isNotBlank()) {
            folder.copy(appKeys = (folder.appKeys + appKey).distinct().take(MAX_APPS_PER_FOLDER))
        } else {
            folder
        }
    }

    /** Empty folders are removed immediately so a deleted collection cannot reappear after restart. */
    fun removeApp(
        folders: List<LauncherFolder>,
        folderId: String,
        appKey: String,
    ): List<LauncherFolder> = folders.mapNotNull { folder ->
        if (folder.id != folderId) return@mapNotNull folder
        val remaining = folder.appKeys.filterNot { it == appKey }
        folder.copy(appKeys = remaining).takeIf { remaining.isNotEmpty() }
    }

    fun moveApp(
        folders: List<LauncherFolder>,
        folderId: String,
        appKey: String,
        delta: Int,
    ): List<LauncherFolder> = folders.map { folder ->
        if (folder.id != folderId) return@map folder
        folder.copy(appKeys = move(folder.appKeys, appKey, delta))
    }

    fun move(keys: List<String>, key: String, delta: Int): List<String> {
        val from = keys.indexOf(key)
        if (from < 0 || keys.size < 2 || delta == 0) return keys
        val to = (from + delta).coerceIn(0, keys.lastIndex)
        if (from == to) return keys
        return keys.toMutableList().apply { add(to, removeAt(from)) }
    }
}
