package cloud.kosch.aiandroid.model

import android.content.ComponentName
import android.os.UserHandle
import androidx.compose.ui.graphics.ImageBitmap

enum class AppProfile(val title: String) {
    PERSONAL("Privat"),
    WORK("Arbeit"),
    OTHER("Profil"),
}

data class LaunchableApp(
    val key: String,
    val label: String,
    val packageName: String,
    val componentName: ComponentName,
    val user: UserHandle,
    val userSerialNumber: Long,
    val profile: AppProfile,
    val icon: ImageBitmap,
    /** Keys used before M2.2; retained only long enough to repair persisted user choices. */
    val legacyKeys: Set<String> = emptySet(),
)

data class WorkProfileState(
    val user: UserHandle,
    val userSerialNumber: Long,
    val quietMode: Boolean,
)
