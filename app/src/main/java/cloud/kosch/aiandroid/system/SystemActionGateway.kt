package cloud.kosch.aiandroid.system

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import cloud.kosch.aiandroid.model.FileInsight
import cloud.kosch.aiandroid.model.SystemPanel

class SystemActionGateway(context: Context) {
    private val appContext = context.applicationContext

    fun openDialer(number: String? = null): Result<Unit> = start(
        Intent(
            Intent.ACTION_DIAL,
            number?.takeIf(String::isNotBlank)?.let { Uri.fromParts("tel", it, null) },
        ),
    )

    fun openPanel(panel: SystemPanel): Result<Unit> = when (panel) {
        SystemPanel.WIFI -> startWithFallback(
            Intent(Settings.ACTION_WIFI_SETTINGS),
            Intent(Settings.ACTION_SETTINGS),
        )

        SystemPanel.BLUETOOTH -> startWithFallback(
            Intent(Settings.ACTION_BLUETOOTH_SETTINGS),
            Intent(Settings.ACTION_SETTINGS),
        )

        SystemPanel.NOTIFICATIONS -> startWithFallback(
            Intent(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Settings.ACTION_ALL_APPS_NOTIFICATION_SETTINGS
                } else {
                    Settings.ACTION_APPLICATION_SETTINGS
                },
            ),
            Intent(Settings.ACTION_SETTINGS),
        )

        SystemPanel.NOTIFICATION_ACCESS -> startWithFallback(
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS),
            Intent(Settings.ACTION_SETTINGS),
        )

        SystemPanel.WALLPAPER -> startWithFallback(
            Intent(Intent.ACTION_SET_WALLPAPER),
            Intent(Settings.ACTION_DISPLAY_SETTINGS),
        )

        SystemPanel.DISPLAY -> startWithFallback(
            Intent(Settings.ACTION_DISPLAY_SETTINGS),
            Intent(Settings.ACTION_SETTINGS),
        )

        SystemPanel.SOUND -> startWithFallback(
            Intent(Settings.ACTION_SOUND_SETTINGS),
            Intent(Settings.ACTION_SETTINGS),
        )

        SystemPanel.BATTERY -> startWithFallback(
            Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS),
            Intent(Settings.ACTION_SETTINGS),
        )

        SystemPanel.PRIVACY -> startWithFallback(
            Intent(Settings.ACTION_PRIVACY_SETTINGS),
            Intent(Settings.ACTION_SETTINGS),
        )

        SystemPanel.ACCESSIBILITY -> startWithFallback(
            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS),
            Intent(Settings.ACTION_SETTINGS),
        )

        SystemPanel.DEFAULT_APPS -> startWithFallback(
            Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS),
            Intent(Settings.ACTION_SETTINGS),
        )

        SystemPanel.STORAGE -> startWithFallback(
            Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS),
            Intent(Settings.ACTION_SETTINGS),
        )

        SystemPanel.ANDROID_SETTINGS -> start(Intent(Settings.ACTION_SETTINGS))
        SystemPanel.HOME_SELECTION -> startWithFallback(
            Intent(Settings.ACTION_HOME_SETTINGS),
            Intent(Settings.ACTION_SETTINGS),
        )
    }

    fun openAppInfo(packageName: String): Result<Unit> = start(
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", packageName, null),
        ),
    )

    fun openStoreListing(packageName: String): Result<Unit> = startWithFallback(
        Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")),
        Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName")),
    )

    fun requestUninstall(packageName: String): Result<Unit> = start(
        Intent(Intent.ACTION_DELETE, Uri.fromParts("package", packageName, null)),
    )

    fun openFile(insight: FileInsight): Result<Unit> = start(
        documentIntent(insight.uri, insight.mimeType),
    )

    fun openDocument(uri: Uri, mimeType: String): Result<Unit> = start(documentIntent(uri, mimeType))

    private fun documentIntent(uri: Uri, mimeType: String) = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, mimeType)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    private fun startWithFallback(primary: Intent, fallback: Intent): Result<Unit> =
        start(primary).recoverCatching { start(fallback).getOrThrow() }

    private fun start(intent: Intent): Result<Unit> = runCatching {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        appContext.startActivity(intent)
    }
}
