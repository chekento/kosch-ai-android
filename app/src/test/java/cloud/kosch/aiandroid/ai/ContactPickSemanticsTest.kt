package cloud.kosch.aiandroid.ai

import cloud.kosch.aiandroid.model.SelectedContact
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class ContactPickSemanticsTest {
    @Test
    fun usableContact_isAccepted() {
        val contact = SelectedContact("Ada", "+491234567")

        val resolution = ContactPickSemantics.resolve(Result.success(contact))

        val accepted = resolution as ContactPickResolution.Accepted
        assertEquals(contact, accepted.contact)
    }

    @Test
    fun successfulQueryWithoutUsableNumber_isRejected() {
        val resolution = ContactPickSemantics.resolve(Result.success(null))

        assertSame(ContactPickResolution.Rejected, resolution)
    }

    @Test
    fun providerFailure_staysFailed() {
        val cause = IllegalStateException("provider unavailable")

        val resolution = ContactPickSemantics.resolve(Result.failure(cause))

        val failed = resolution as ContactPickResolution.Failed
        assertSame(cause, failed.cause)
    }
}
