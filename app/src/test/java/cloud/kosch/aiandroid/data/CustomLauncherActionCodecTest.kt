package cloud.kosch.aiandroid.data

import cloud.kosch.aiandroid.model.CustomLauncherAction
import cloud.kosch.aiandroid.model.CustomLauncherTarget
import cloud.kosch.aiandroid.model.LauncherInternalAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomLauncherActionCodecTest {
    @Test
    fun roundTrip_isDeterministicAndSortedByStableId() {
        val source = listOf(
            CustomLauncherAction("web.docs", "Docs", target = CustomLauncherTarget.WebUrl("https://example.com/docs")),
            CustomLauncherAction("app.mail", "Mail", target = CustomLauncherTarget.AppLaunch("com.example.mail")),
            CustomLauncherAction(
                "kosch.settings",
                "Settings",
                target = CustomLauncherTarget.Internal(LauncherInternalAction.OPEN_SETTINGS),
            ),
            CustomLauncherAction("notes.new", "Neue Notiz", target = CustomLauncherTarget.DeepLink("notes://new/item")),
        )
        val first = CustomLauncherActionCodec.encode(source)
        val decoded = CustomLauncherActionCodec.decode(first)
        val second = CustomLauncherActionCodec.encode(decoded)

        assertEquals(first, second)
        assertEquals(source.map { it.id }.sorted(), decoded.map { it.id })
    }

    @Test
    fun duplicateIds_areRejected() {
        val duplicate = CustomLauncherAction("same.id", "One", target = CustomLauncherTarget.WebUrl("https://example.com"))
        assertThrows(IllegalArgumentException::class.java) {
            CustomLauncherActionCodec.encode(listOf(duplicate, duplicate.copy(name = "Two")))
        }
    }

    @Test
    fun importedBlockedScheme_isRejectedAfterDecode() {
        val safe = CustomLauncherActionCodec.encode(
            listOf(CustomLauncherAction("notes.new", "Notes", target = CustomLauncherTarget.DeepLink("notes://new/item"))),
        )
        val encodedSafeTarget = java.util.Base64.getUrlEncoder().withoutPadding()
            .encodeToString("notes://new/item".toByteArray())
        val encodedUnsafeTarget = java.util.Base64.getUrlEncoder().withoutPadding()
            .encodeToString("content://private/item".toByteArray())
        val tampered = safe.replace(encodedSafeTarget, encodedUnsafeTarget)

        assertThrows(IllegalArgumentException::class.java) { CustomLauncherActionCodec.decode(tampered) }
    }

    @Test
    fun formatNeverContainsRawIntentExtras() {
        val payload = CustomLauncherActionCodec.encode(
            listOf(CustomLauncherAction("app.mail", "Mail", target = CustomLauncherTarget.AppLaunch("com.example.mail"))),
        )
        assertTrue(!payload.contains("Intent;"))
        assertTrue(!payload.contains("component="))
        assertTrue(!payload.contains("extra="))
    }
}
