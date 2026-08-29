package cloud.kosch.aiandroid.model

/** Hex parser used by Home Studio expert controls. Output is a signed Android/Compose ARGB Int. */
object WorkspaceStyleColor {
    fun parse(raw: String): Int? {
        val text = raw.trim().removePrefix("#").removePrefix("0x").removePrefix("0X")
        val normalized = when (text.length) {
            3 -> "FF" + text.flatMap { listOf(it, it) }.joinToString("")
            4 -> text.flatMap { listOf(it, it) }.joinToString("")
            6 -> "FF$text"
            8 -> text
            else -> return null
        }
        if (!normalized.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }) return null
        return normalized.toLongOrNull(16)?.toInt()
    }

    fun format(argb: Int?): String = argb?.let { "#%08X".format(it) }.orEmpty()
}
