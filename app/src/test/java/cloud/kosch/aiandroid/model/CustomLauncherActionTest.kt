package cloud.kosch.aiandroid.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomLauncherActionTest {
    @Test
    fun httpsLink_isNormalizedWithoutCredentials() {
        val result = CustomLauncherActionValidator.validate(
            CustomLauncherAction(
                id = " Work.Docs ",
                name = "  Docs  ",
                target = CustomLauncherTarget.WebUrl("https://example.com/a/../docs"),
            ),
        )
        assertTrue(result is CustomLauncherActionValidation.Valid)
        val action = (result as CustomLauncherActionValidation.Valid).normalized
        assertEquals("work.docs", action.id)
        assertEquals("Docs", action.name)
        assertEquals(CustomLauncherTarget.WebUrl("https://example.com/docs"), action.target)
    }

    @Test
    fun arbitraryRawIntentAndLocalSchemes_areRejected() {
        listOf(
            "intent://scan/#Intent;scheme=zxing;end",
            "file:///sdcard/private.txt",
            "content://contacts/1",
            "javascript:alert(1)",
            "data:text/plain,secret",
        ).forEach { raw ->
            val result = CustomLauncherActionValidator.validate(
                CustomLauncherAction("test.action", "Test", target = CustomLauncherTarget.DeepLink(raw)),
            )
            assertTrue("Expected rejection for $raw", result is CustomLauncherActionValidation.Invalid)
        }
    }

    @Test
    fun customAppDeepLink_isAllowedWithoutRawIntentExtras() {
        val result = CustomLauncherActionValidator.validate(
            CustomLauncherAction(
                id = "notes.new",
                name = "Neue Notiz",
                target = CustomLauncherTarget.DeepLink("notes://new/item"),
            ),
        )
        assertTrue(result is CustomLauncherActionValidation.Valid)
    }

    @Test
    fun appLaunch_requiresRealPackageName() {
        assertTrue(
            CustomLauncherActionValidator.validate(
                CustomLauncherAction("app.mail", "Mail", target = CustomLauncherTarget.AppLaunch("com.example.mail")),
            ) is CustomLauncherActionValidation.Valid,
        )
        assertTrue(
            CustomLauncherActionValidator.validate(
                CustomLauncherAction("app.bad", "Bad", target = CustomLauncherTarget.AppLaunch("../evil")),
            ) is CustomLauncherActionValidation.Invalid,
        )
    }

    @Test
    fun internalActions_areTypedAndPortable() {
        val result = CustomLauncherActionValidator.validate(
            CustomLauncherAction(
                id = "kosch.assistant",
                name = "Assistant",
                iconKey = "internal:auto_awesome",
                target = CustomLauncherTarget.Internal(LauncherInternalAction.OPEN_ASSISTANT),
            ),
        )
        assertTrue(result is CustomLauncherActionValidation.Valid)
    }
}
