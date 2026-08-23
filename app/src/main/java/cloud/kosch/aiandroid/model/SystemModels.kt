package cloud.kosch.aiandroid.model

import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import android.os.UserHandle

enum class SystemPanel(val title: String) {
    WIFI("WLAN"),
    BLUETOOTH("Bluetooth"),
    NOTIFICATIONS("Benachrichtigungen"),
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

data class LaunchableShortcut(
    val id: String,
    val packageName: String,
    val label: String,
    val user: UserHandle,
    val icon: ImageBitmap?,
) {
    val key: String = "${user.hashCode()}:$packageName:$id"
}

data class WidgetRecord(
    val appWidgetId: Int,
)
