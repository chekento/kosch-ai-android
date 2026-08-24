package cloud.kosch.aiandroid.data

import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Build
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import cloud.kosch.aiandroid.model.LaunchableApp
import cloud.kosch.aiandroid.model.LaunchableShortcut
import cloud.kosch.aiandroid.model.AppProfile
import java.util.Locale

class AppCatalog(
    context: Context,
    private val callbackHandler: Handler,
    private val onCatalogChanged: () -> Unit,
) {
    private val launcherApps = context.getSystemService(LauncherApps::class.java)
    private val userManager = context.getSystemService(UserManager::class.java)
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

    fun loadApps(): List<LaunchableApp> = launcherApps.profiles
        .flatMap { profile ->
            val serial = userManager.getSerialNumberForUser(profile)
            val profileType = profileType(profile)
            runCatching { launcherApps.getActivityList(null, profile) }
                .getOrDefault(emptyList())
                .mapNotNull { activity ->
                    runCatching {
                        val component = activity.componentName
                        LaunchableApp(
                            key = "$serial:${component.flattenToShortString()}",
                            label = activity.label?.toString()?.ifBlank { component.packageName }
                                ?: component.packageName,
                            packageName = component.packageName,
                            componentName = component,
                            user = activity.user,
                            userSerialNumber = serial,
                            profile = profileType,
                            icon = activity.getBadgedIcon(0).safeBitmap().asImageBitmap(),
                            legacyKeys = setOf("${activity.user.hashCode()}:${component.flattenToShortString()}"),
                        )
                    }.getOrNull()
                }
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

    fun loadShortcuts(app: LaunchableApp): List<LaunchableShortcut> {
        val query = LauncherApps.ShortcutQuery()
            .setPackage(app.packageName)
            .setQueryFlags(
                LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                    LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or
                    LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED,
            )
        return launcherApps.getShortcuts(query, app.user).orEmpty()
            .filter(ShortcutInfo::isEnabled)
            .sortedBy(ShortcutInfo::getRank)
            .map { shortcut ->
                LaunchableShortcut(
                    id = shortcut.id,
                    packageName = shortcut.`package`,
                    label = shortcut.shortLabel?.toString()?.ifBlank { shortcut.id } ?: shortcut.id,
                    user = app.user,
                    icon = launcherApps.getShortcutBadgedIconDrawable(
                        shortcut,
                        Resources.getSystem().displayMetrics.densityDpi,
                    )?.safeBitmap()?.asImageBitmap(),
                )
            }
    }

    fun launch(shortcut: LaunchableShortcut) {
        launcherApps.startShortcut(
            shortcut.packageName,
            shortcut.id,
            null,
            null,
            shortcut.user,
        )
    }

    private fun Drawable.safeBitmap(): Bitmap = runCatching {
        toBitmap(width = ICON_SIZE_PX, height = ICON_SIZE_PX, config = Bitmap.Config.ARGB_8888)
    }.getOrElse {
        Bitmap.createBitmap(ICON_SIZE_PX, ICON_SIZE_PX, Bitmap.Config.ARGB_8888)
    }

    private fun profileType(user: UserHandle): AppProfile {
        if (user == Process.myUserHandle()) return AppProfile.PERSONAL
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            val userType = runCatching { launcherApps.getLauncherUserInfo(user)?.userType }.getOrNull()
            return when (userType) {
                UserManager.USER_TYPE_PROFILE_MANAGED -> AppProfile.WORK
                else -> AppProfile.OTHER
            }
        }
        return AppProfile.WORK
    }

    private companion object {
        const val ICON_SIZE_PX = 96
    }
}
