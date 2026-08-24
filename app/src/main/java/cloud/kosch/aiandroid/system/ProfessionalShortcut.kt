package cloud.kosch.aiandroid.system

import android.view.KeyEvent

enum class ProfessionalShortcut {
    COMMAND,
    APPS,
    PRO_DESK,
    CONTROL_CENTER,
    PHONE,
    FILES,
    BACKUP,
    AUDIT,
    PEN_SPACE,
}

object ProfessionalShortcutResolver {
    fun resolve(
        keyCode: Int,
        isCtrlPressed: Boolean,
        isMetaPressed: Boolean,
        isShiftPressed: Boolean,
    ): ProfessionalShortcut? {
        if (!isCtrlPressed && !isMetaPressed) return null
        return when (keyCode) {
            KeyEvent.KEYCODE_K -> ProfessionalShortcut.COMMAND
            KeyEvent.KEYCODE_SPACE -> ProfessionalShortcut.APPS
            KeyEvent.KEYCODE_H -> ProfessionalShortcut.PRO_DESK
            KeyEvent.KEYCODE_COMMA -> ProfessionalShortcut.CONTROL_CENTER
            KeyEvent.KEYCODE_D -> ProfessionalShortcut.PHONE
            KeyEvent.KEYCODE_O -> ProfessionalShortcut.FILES
            KeyEvent.KEYCODE_B -> ProfessionalShortcut.BACKUP
            KeyEvent.KEYCODE_L -> ProfessionalShortcut.AUDIT
            KeyEvent.KEYCODE_P -> if (isShiftPressed) ProfessionalShortcut.PEN_SPACE else null
            else -> null
        }
    }
}
