package cloud.kosch.aiandroid.data

import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherApps
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Process
import android.os.UserHandle
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import cloud.kosch.aiandroid.model.LaunchableApp
import java.util.Locale

class AppCatalog(
    context: Context,
    private val callbackHandler: Handler,
    private val onCatalogChanged: () -> Unit,
) {
    private val launcherApps = context.getSystemService(LauncherApps::class.java)
    private var listening = false

    private val callback = object : LauncherApps.Callback() {
        override fun onPackageRemoved(packageName: String, user: UserHandle) = onCatalogChanged()
        override fun onPackageAdded(packageName: String, user: UserHandle) = onCatalogChanged()
        override fun onPackageChanged(packageName: String, user: UserHandle) = onCatalogChanged()
        override fun onPackagesAvailable(
            packageNames: Array<out String>,
            user: UserHandle,
            replacing: Boolean,
        ) = onCatalogChanged()

        override fun onPackagesUnavailable(
            packageNames: Array<out String>,
            user: UserHandle,
            replacing: Boolean,
        ) = onCatalogChanged()
    }

    fun startListening() {
        if (listening) return
        launcherApps.registerCallback(callback, callbackHandler)
        listening = true
    }

    fun stopListening() {
        if (!listening) return
        launcherApps.unregisterCallback(callback)
        listening = false
    }

    fun loadApps(): List<LaunchableApp> = launcherApps
        .getActivityList(null, Process.myUserHandle())
        .mapNotNull { activity ->
            runCatching {
                val component = activity.componentName
                LaunchableApp(
                    key = "${activity.user.hashCode()}:${component.flattenToShortString()}",
                    label = activity.label?.toString()?.ifBlank { component.packageName }
                        ?: component.packageName,
                    packageName = component.packageName,
                    componentName = component,
                    user = activity.user,
                    icon = activity.getBadgedIcon(0).safeBitmap().asImageBitmap(),
                )
            }.getOrNull()
        }
        .sortedBy { it.label.lowercase(Locale.getDefault()) }

    fun launch(app: LaunchableApp) {
        launcherApps.startMainActivity(
            app.componentName,
            app.user,
            null,
            null,
        )
    }

    private fun Drawable.safeBitmap(): Bitmap = runCatching {
        toBitmap(width = ICON_SIZE_PX, height = ICON_SIZE_PX, config = Bitmap.Config.ARGB_8888)
    }.getOrElse {
        Bitmap.createBitmap(ICON_SIZE_PX, ICON_SIZE_PX, Bitmap.Config.ARGB_8888)
    }

    private companion object {
        const val ICON_SIZE_PX = 96
    }
}

