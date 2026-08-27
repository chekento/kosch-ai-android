package cloud.kosch.aiandroid.assistant

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
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
    fun observationPermissionBudget_containsCameraAndProjectionButNoMicOrInternet() {
        val packageInfo = context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong()),
        )
        val requested = packageInfo.requestedPermissions.orEmpty().toSet()

        assertTrue(Manifest.permission.CAMERA in requested)
        assertTrue(Manifest.permission.FOREGROUND_SERVICE in requested)
        assertTrue(Manifest.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION in requested)
        assertFalse(Manifest.permission.RECORD_AUDIO in requested)
        assertFalse(Manifest.permission.INTERNET in requested)
        assertEquals(4, requested.size)
    }
}
