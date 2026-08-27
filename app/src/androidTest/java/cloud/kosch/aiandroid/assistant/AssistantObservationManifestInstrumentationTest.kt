package cloud.kosch.aiandroid.assistant

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AssistantObservationManifestInstrumentationTest {
    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Test
    fun screenShareService_isPrivateAndMediaProjectionTyped() {
        val info = context.packageManager.getServiceInfo(
            ComponentName(context, AssistantScreenShareService::class.java),
            PackageManager.ComponentInfoFlags.of(0L),
        )

        assertFalse(info.exported)
        assertTrue(
            info.foregroundServiceType and ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION != 0,
        )
    }

    @Test
    fun observationPermissionBudget_isExplicitAndForbidsSensitiveExpansion() {
        val packageInfo = context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong()),
        )
        val requested = packageInfo.requestedPermissions.orEmpty().toSet()

        val required = setOf(
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.CAMERA,
            Manifest.permission.FOREGROUND_SERVICE,
            Manifest.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION,
        )
        val toolingAndFrameworkAllowed = setOf(
            Manifest.permission.DUMP,
            "${context.packageName}.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION",
        )
        val forbidden = setOf(
            Manifest.permission.INTERNET,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.WRITE_CONTACTS,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.WRITE_CALL_LOG,
            Manifest.permission.READ_SMS,
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.QUERY_ALL_PACKAGES,
        )

        assertTrue("Missing required observation permissions: ${required - requested}", requested.containsAll(required))
        assertTrue(
            "Unexpected permission expansion in debug APK: ${requested - required - toolingAndFrameworkAllowed}",
            requested.all { it in required || it in toolingAndFrameworkAllowed },
        )
        assertTrue("Forbidden observation permissions requested: ${requested intersect forbidden}", (requested intersect forbidden).isEmpty())
        assertFalse(Manifest.permission.INTERNET in requested)
        assertFalse(Manifest.permission.RECORD_AUDIO in requested)
    }
}
