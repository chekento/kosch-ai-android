package cloud.kosch.aiandroid.model

import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import android.os.UserHandle

enum class SystemPanel(val title: String) {
    WIFI("WLAN"),
    BLUETOOTH("Bluetooth"),
    NOTIFICATIONS("Benachrichtigungen"),
    NOTIFICATION_ACCESS("Benachrichtigungspunkte"),
    WALLPAPER("Hintergrund"),
    DISPLAY("Anzeige"),
    SOUND("Ton"),
    BATTERY("Akku"),
    PRIVACY("Datenschutz"),
    ACCESSIBILITY("Bedienungshilfen"),
    DEFAULT_APPS("Standard-Apps"),
    STORAGE("Speicher"),
    ANDROID_SETTINGS("Android-Einstellungen"),
    HOME_SELECTION("Start-App-Auswahl"),
}

data class FileInsight(
    val uri: Uri,
    val displayName: String,
    val mimeType: String,
    val sizeBytes: Long?,
    val category: String,
    val summary: String,
    val preview: String?,
    val suggestedName: String?,
    val safetyNote: String,
)

data class FileWorkspaceEntry(
    val uri: Uri,
    val documentId: String,
    val displayName: String,
    val mimeType: String,
    val sizeBytes: Long?,
    val lastModifiedEpochMillis: Long?,
    val isDirectory: Boolean,
    val canRename: Boolean,
    val canDelete: Boolean,
    val canCreateChildren: Boolean,
    val category: String,
)

data class FileWorkspaceSummary(
    val fileCount: Int,
    val directoryCount: Int,
    val knownBytes: Long,
    val duplicateNameGroups: Int,
    val categoryCounts: Map<String, Int>,
    val largestFiles: List<String>,
)

data class LaunchableShortcut(
    val id: String,
    val packageName: String,
    val label: String,
    val user: UserHandle,
    val userSerialNumber: Long,
    val icon: ImageBitmap?,
) {
    val key: String = "$userSerialNumber:$packageName:$id"
}

data class WidgetRecord(
    val appWidgetId: Int,
)

enum class WidgetSizePreset(
    val title: String,
    val minWidthDp: Int,
    val minHeightDp: Int,
    val boardHeightDp: Int,
) {
    COMPACT("Kompakt", 180, 110, 150),
    STANDARD("Standard", 280, 150, 210),
    TALL("Hoch", 220, 300, 350),
}
