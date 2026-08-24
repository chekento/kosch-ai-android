package cloud.kosch.aiandroid.model

enum class FaqCategory(val title: String) {
    START("Start & Sicherheit"),
    LAUNCHER("Launcher & Bedienung"),
    AI("KI & Datenschutz"),
    PEN("Smartpen & Pen Space"),
    ANDROID("Android & Profile"),
    TROUBLESHOOTING("Problemlösung"),
}

data class FaqEntry(
    val id: String,
    val category: FaqCategory,
    val question: String,
    val answer: String,
    val keywords: List<String> = emptyList(),
)
