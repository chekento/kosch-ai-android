package cloud.kosch.aiandroid.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import cloud.kosch.aiandroid.AssistantAgentController
import cloud.kosch.aiandroid.ai.AssistantPolicyDecision
import cloud.kosch.aiandroid.model.AssistantAgentPreferences
import cloud.kosch.aiandroid.model.AssistantAgentState
import cloud.kosch.aiandroid.model.AssistantObservationSource
import cloud.kosch.aiandroid.model.AssistantPresenceMode
import cloud.kosch.aiandroid.model.AssistantWakeWordMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AssistantAgentStoreInstrumentationTest {
    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Test
    fun portablePreferences_roundTripWithoutRuntimeConsent() {
        val store = AssistantAgentStore(context)
        val original = store.load()
        try {
            val expected = AssistantAgentPreferences(
                characterId = "anime_female",
                presenceMode = AssistantPresenceMode.FULL_COMPANION,
                wakeWordMode = AssistantWakeWordMode.COMPUTER,
                localWakeWordOnly = true,
                screenObservationEnabled = true,
                cameraObservationEnabled = true,
                actionExecutionEnabled = true,
                confirmationRequiredForExternalActions = true,
            )
            store.saveUserObservationOptIn(expected)
            assertEquals(expected, AssistantAgentStore(context).load())

            val controller = AssistantAgentController(context)
            controller.setAssistantEnabled(true)
            assertNull(controller.activeObservation)
            assertEquals(AssistantAgentState.IDLE, controller.state)
        } finally {
            store.saveUserObservationOptIn(original)
        }
    }

    @Test
    fun genericPersistenceCannotEnableScreenOrCameraAwareness() {
        val store = AssistantAgentStore(context)
        val original = store.load()
        try {
            store.saveUserObservationOptIn(original.copy(screenObservationEnabled = false, cameraObservationEnabled = false))
            val screenAttempt = runCatching {
                store.save(store.load().copy(screenObservationEnabled = true))
            }
            assertTrue(screenAttempt.isFailure)
            assertFalse(store.load().screenObservationEnabled)

            val cameraAttempt = runCatching {
                store.save(store.load().copy(cameraObservationEnabled = true))
            }
            assertTrue(cameraAttempt.isFailure)
            assertFalse(store.load().cameraObservationEnabled)
        } finally {
            store.saveUserObservationOptIn(original)
        }
    }

    @Test
    fun observation_neverStartsWithoutVisiblePlatformConsent() {
        val store = AssistantAgentStore(context)
        val original = store.load()
        try {
            store.saveUserObservationOptIn(original.copy(screenObservationEnabled = true))
            val controller = AssistantAgentController(context)
            controller.setAssistantEnabled(true)

            assertEquals(
                AssistantPolicyDecision.REQUIRE_USER_CONSENT,
                controller.beginObservation(
                    assistantEnabled = true,
                    source = AssistantObservationSource.SCREEN,
                    platformConsentGranted = false,
                    sessionVisible = true,
                ),
            )
            assertNull(controller.activeObservation)

            assertEquals(
                AssistantPolicyDecision.BLOCK_SESSION_NOT_VISIBLE,
                controller.beginObservation(
                    assistantEnabled = true,
                    source = AssistantObservationSource.SCREEN,
                    platformConsentGranted = true,
                    sessionVisible = false,
                ),
            )
            assertNull(controller.activeObservation)
            assertEquals(AssistantAgentState.PRIVACY_BLOCKED, controller.state)
        } finally {
            store.saveUserObservationOptIn(original)
        }
    }
}
