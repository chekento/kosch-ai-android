package cloud.kosch.aiandroid.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import cloud.kosch.aiandroid.AssistantVoiceController
import cloud.kosch.aiandroid.model.AssistantSystemVoiceOption
import cloud.kosch.aiandroid.model.AssistantVoiceGender
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AssistantDeviceVoiceStoreInstrumentationTest {
    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Test
    fun femaleAndMaleSlots_areIndependentAndDeviceLocal() {
        val store = AssistantDeviceVoiceStore(context)
        val original = store.load()
        try {
            store.assign(AssistantVoiceGender.FEMALE, "voice_f_test")
            store.assign(AssistantVoiceGender.MALE, "voice_m_test")

            val loaded = AssistantDeviceVoiceStore(context).load()
            assertEquals("voice_f_test", loaded.femaleVoiceName)
            assertEquals("voice_m_test", loaded.maleVoiceName)
        } finally {
            store.assign(AssistantVoiceGender.FEMALE, original.femaleVoiceName)
            store.assign(AssistantVoiceGender.MALE, original.maleVoiceName)
            store.assign(AssistantVoiceGender.NEUTRAL, original.neutralVoiceName)
        }
    }

    @Test
    fun controller_onlyAssignsVoicesReportedByCurrentTtsRuntime_andKeepsGenderSlotsDistinct() {
        val store = AssistantDeviceVoiceStore(context)
        val original = store.load()
        try {
            store.assign(AssistantVoiceGender.FEMALE, null)
            store.assign(AssistantVoiceGender.MALE, null)
            val controller = AssistantVoiceController(context)
            controller.updateAvailableVoices(
                listOf(
                    AssistantSystemVoiceOption(
                        name = "local_voice_one",
                        languageTag = "de-DE",
                        quality = 400,
                        latency = 100,
                        networkRequired = false,
                    ),
                    AssistantSystemVoiceOption(
                        name = "network_voice_two",
                        languageTag = "de-DE",
                        quality = 500,
                        latency = 200,
                        networkRequired = true,
                    ),
                ),
            )

            assertTrue(controller.assignFromUser(AssistantVoiceGender.FEMALE, "local_voice_one"))
            assertEquals("local_voice_one", controller.assignedVoiceName(AssistantVoiceGender.FEMALE))
            assertEquals(false, controller.assignedVoice(AssistantVoiceGender.FEMALE)?.networkRequired)

            assertFalse(controller.assignFromUser(AssistantVoiceGender.MALE, "local_voice_one"))
            assertNull(controller.assignedVoiceName(AssistantVoiceGender.MALE))
            assertNotNull(controller.statusMessage)

            assertTrue(controller.assignFromUser(AssistantVoiceGender.MALE, "network_voice_two"))
            assertEquals("network_voice_two", controller.assignedVoiceName(AssistantVoiceGender.MALE))

            assertTrue(controller.assignFromUser(AssistantVoiceGender.FEMALE, null))
            assertNull(controller.assignedVoiceName(AssistantVoiceGender.FEMALE))
        } finally {
            store.assign(AssistantVoiceGender.FEMALE, original.femaleVoiceName)
            store.assign(AssistantVoiceGender.MALE, original.maleVoiceName)
            store.assign(AssistantVoiceGender.NEUTRAL, original.neutralVoiceName)
        }
    }
}
