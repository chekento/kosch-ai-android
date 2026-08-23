package cloud.kosch.aiandroid.model

import android.content.ComponentName
import android.os.UserHandle
import androidx.compose.ui.graphics.ImageBitmap

data class LaunchableApp(
    val key: String,
    val label: String,
    val packageName: String,
    val componentName: ComponentName,
    val user: UserHandle,
    val icon: ImageBitmap,
)

