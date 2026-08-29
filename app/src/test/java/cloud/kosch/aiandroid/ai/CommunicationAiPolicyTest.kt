package cloud.kosch.aiandroid.ai

import cloud.kosch.aiandroid.model.SelectedContact
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CommunicationAiPolicyTest {
    private val contact = SelectedContact("Alex Beispiel", "+49 40 1234567")

    @Test
    fun callPrepNeverLeaksPhoneNumber() {
        val prompt = CommunicationAiPolicy.callPrep(contact, "Projektstatus und nächster Termin")

        assertFalse(prompt.containsPhoneNumber)
        assertFalse(prompt.text.contains(contact.phoneNumber))
        assertTrue(prompt.text.contains("Alex Beispiel"))
    }

    @Test
    fun messageDraftNeverLeaksPhoneNumber() {
        val prompt = CommunicationAiPolicy.messageDraft(
            contact = contact,
            intent = "Termin auf Dienstag verschieben",
            tone = "kurz und freundlich",
        )

        assertFalse(prompt.text.contains(contact.phoneNumber))
        assertTrue(prompt.text.contains("Termin auf Dienstag verschieben"))
        assertTrue(prompt.text.contains("kurz und freundlich"))
    }

    @Test
    fun postCallNoteExplicitlyForbidsInventedPersonalData() {
        val prompt = CommunicationAiPolicy.postCallNote("Budget abgestimmt. Angebot bis Freitag senden.")
        assertTrue(prompt.text.contains("Erfinde keine personenbezogenen Daten"))
    }
}
