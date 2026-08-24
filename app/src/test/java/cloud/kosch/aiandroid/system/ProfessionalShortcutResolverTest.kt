package cloud.kosch.aiandroid.system

import android.view.KeyEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProfessionalShortcutResolverTest {
    @Test
    fun ctrlKOpensCommandBar() {
        assertEquals(
            ProfessionalShortcut.COMMAND,
            ProfessionalShortcutResolver.resolve(KeyEvent.KEYCODE_K, true, false, false),
        )
    }

    @Test
    fun metaSpaceOpensApps() {
        assertEquals(
            ProfessionalShortcut.APPS,
            ProfessionalShortcutResolver.resolve(KeyEvent.KEYCODE_SPACE, false, true, false),
        )
    }

    @Test
    fun penSpaceRequiresShift() {
        assertNull(ProfessionalShortcutResolver.resolve(KeyEvent.KEYCODE_P, true, false, false))
        assertEquals(
            ProfessionalShortcut.PEN_SPACE,
            ProfessionalShortcutResolver.resolve(KeyEvent.KEYCODE_P, true, false, true),
        )
    }

    @Test
    fun plainKeysAreNeverIntercepted() {
        assertNull(ProfessionalShortcutResolver.resolve(KeyEvent.KEYCODE_K, false, false, false))
    }
}
