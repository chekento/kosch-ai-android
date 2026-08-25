package cloud.kosch.aiandroid.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import cloud.kosch.aiandroid.model.HomePage
import cloud.kosch.aiandroid.model.SceneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkspaceStoreInstrumentationTest {
    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Test
    fun sceneAndHomePage_roundTripThroughDevicePreferences() {
        val store = WorkspaceStore(context)
        val originalScene = store.loadScene()
        val originalHomePage = store.loadHomePage()

        try {
            store.saveScene(SceneId.WORK)
            store.saveHomePage(HomePage.SMART_SPACE)

            val reloaded = WorkspaceStore(context)
            assertEquals(SceneId.WORK, reloaded.loadScene())
            assertEquals(HomePage.SMART_SPACE, reloaded.loadHomePage())
        } finally {
            store.saveScene(originalScene)
            store.saveHomePage(originalHomePage)
        }
    }

    @Test
    fun onboardingCompletion_isDurableAcrossStoreInstances() {
        val store = WorkspaceStore(context)
        store.completeOnboarding()

        assertTrue(WorkspaceStore(context).isOnboardingComplete())
    }
}
