package cloud.kosch.aiandroid.ai

/**
 * Machine-readable "AI everywhere" map.
 *
 * The catalog separates intelligence from transport. A feature can remain useful with no account and no API key;
 * richer on-device or installed-app lanes are additive. Sensitive surfaces never default to external handoff.
 */
enum class AiFeatureExecution {
    LOCAL_RULES,
    ANDROID_ON_DEVICE_GENAI,
    LOCAL_MODEL_PACK,
    INSTALLED_APP_HANDOFF,
    APP_SHORTCUT_OR_WIDGET,
    SYSTEM_INTENT,
}

enum class AiPrivacyClass {
    NON_SENSITIVE,
    USER_SELECTED_CONTENT,
    PERSONAL_CONTEXT,
    HIGHLY_SENSITIVE,
}

data class AiEverywhereFeature(
    val id: String,
    val surface: String,
    val title: String,
    val executions: Set<AiFeatureExecution>,
    val privacyClass: AiPrivacyClass,
    val backgroundSafe: Boolean,
    val externalPreviewRequired: Boolean,
)

object AiEverywhereFeatureCatalog {
    val features: List<AiEverywhereFeature> = listOf(
        feature("theme.prompt", "Theme", "Theme aus Sprache konfigurieren", true, AiPrivacyClass.NON_SENSITIVE,
            AiFeatureExecution.LOCAL_RULES, AiFeatureExecution.ANDROID_ON_DEVICE_GENAI, AiFeatureExecution.INSTALLED_APP_HANDOFF),
        feature("wallpaper.recipe", "Hintergrund", "Stimmung in Background-Rezept übersetzen", true, AiPrivacyClass.NON_SENSITIVE,
            AiFeatureExecution.LOCAL_RULES, AiFeatureExecution.ANDROID_ON_DEVICE_GENAI),
        feature("layout.optimize", "Home", "Layout nach Nutzung, Reichweite und Modus optimieren", true, AiPrivacyClass.PERSONAL_CONTEXT,
            AiFeatureExecution.LOCAL_RULES, AiFeatureExecution.LOCAL_MODEL_PACK),
        feature("apps.organize", "Apps", "Apps lokal clustern und Smart Groups vorschlagen", true, AiPrivacyClass.PERSONAL_CONTEXT,
            AiFeatureExecution.LOCAL_RULES, AiFeatureExecution.LOCAL_MODEL_PACK),
        feature("apps.ai_hub", "Apps", "KI-Apps erkennen, Fähigkeiten bündeln und Store-Fallback zeigen", true, AiPrivacyClass.NON_SENSITIVE,
            AiFeatureExecution.LOCAL_RULES, AiFeatureExecution.APP_SHORTCUT_OR_WIDGET),
        feature("search.intent", "Suche", "Suchtext als App-, System-, Theme- oder KI-Intent verstehen", true, AiPrivacyClass.NON_SENSITIVE,
            AiFeatureExecution.LOCAL_RULES, AiFeatureExecution.ANDROID_ON_DEVICE_GENAI),
        feature("search.rewrite", "Suche", "Komplexe Anfrage lokal präzisieren", false, AiPrivacyClass.USER_SELECTED_CONTENT,
            AiFeatureExecution.ANDROID_ON_DEVICE_GENAI, AiFeatureExecution.INSTALLED_APP_HANDOFF),
        feature("files.inspect", "Dateien", "Dateien lokal klassifizieren und Aktionen vorschlagen", true, AiPrivacyClass.USER_SELECTED_CONTENT,
            AiFeatureExecution.LOCAL_RULES, AiFeatureExecution.LOCAL_MODEL_PACK),
        feature("files.summarize", "Dateien", "Explizit gewählte Inhalte zusammenfassen", false, AiPrivacyClass.USER_SELECTED_CONTENT,
            AiFeatureExecution.ANDROID_ON_DEVICE_GENAI, AiFeatureExecution.LOCAL_MODEL_PACK, AiFeatureExecution.INSTALLED_APP_HANDOFF),
        feature("communication.call_prep", "Kommunikation", "Gespräch vorbereiten ohne Telefonnummer im Prompt", false, AiPrivacyClass.PERSONAL_CONTEXT,
            AiFeatureExecution.LOCAL_RULES, AiFeatureExecution.ANDROID_ON_DEVICE_GENAI, AiFeatureExecution.INSTALLED_APP_HANDOFF),
        feature("communication.message_draft", "Kommunikation", "Nachricht formulieren und Ton anpassen", false, AiPrivacyClass.PERSONAL_CONTEXT,
            AiFeatureExecution.ANDROID_ON_DEVICE_GENAI, AiFeatureExecution.LOCAL_MODEL_PACK, AiFeatureExecution.INSTALLED_APP_HANDOFF),
        feature("communication.follow_up", "Kommunikation", "Follow-up und Reminder vorbereiten", false, AiPrivacyClass.PERSONAL_CONTEXT,
            AiFeatureExecution.LOCAL_RULES, AiFeatureExecution.ANDROID_ON_DEVICE_GENAI, AiFeatureExecution.SYSTEM_INTENT),
        feature("notifications.cluster", "Benachrichtigungen", "Freigegebene Benachrichtigungen lokal gruppieren/priorisieren", true, AiPrivacyClass.HIGHLY_SENSITIVE,
            AiFeatureExecution.LOCAL_RULES, AiFeatureExecution.LOCAL_MODEL_PACK),
        feature("notifications.summary", "Benachrichtigungen", "Vom Nutzer geöffnete Gruppen lokal zusammenfassen", false, AiPrivacyClass.HIGHLY_SENSITIVE,
            AiFeatureExecution.ANDROID_ON_DEVICE_GENAI, AiFeatureExecution.LOCAL_MODEL_PACK),
        feature("pen.structure", "Smartpen", "Handschrift-/Skizzenkontext in Aktionen übersetzen", false, AiPrivacyClass.USER_SELECTED_CONTENT,
            AiFeatureExecution.LOCAL_RULES, AiFeatureExecution.ANDROID_ON_DEVICE_GENAI, AiFeatureExecution.LOCAL_MODEL_PACK),
        feature("screen.ask", "Screen", "Manuell freigegebenen Bildschirminhalt erklären", false, AiPrivacyClass.HIGHLY_SENSITIVE,
            AiFeatureExecution.ANDROID_ON_DEVICE_GENAI, AiFeatureExecution.LOCAL_MODEL_PACK, AiFeatureExecution.INSTALLED_APP_HANDOFF),
        feature("camera.ask", "Kamera", "Manuell freigegebenes Kamerabild erklären", false, AiPrivacyClass.HIGHLY_SENSITIVE,
            AiFeatureExecution.ANDROID_ON_DEVICE_GENAI, AiFeatureExecution.LOCAL_MODEL_PACK, AiFeatureExecution.INSTALLED_APP_HANDOFF),
        feature("accessibility.describe", "Barrierefreiheit", "Gewählte Bilder/Oberflächen beschreiben", false, AiPrivacyClass.USER_SELECTED_CONTENT,
            AiFeatureExecution.ANDROID_ON_DEVICE_GENAI, AiFeatureExecution.LOCAL_MODEL_PACK),
        feature("automation.nl_rule", "Automationen", "Natürliche Sprache in lokale Regeln + Dry Run übersetzen", true, AiPrivacyClass.PERSONAL_CONTEXT,
            AiFeatureExecution.LOCAL_RULES, AiFeatureExecution.LOCAL_MODEL_PACK),
        feature("backup.explain_diff", "Backup", "Restore-Diff verständlich erklären", true, AiPrivacyClass.PERSONAL_CONTEXT,
            AiFeatureExecution.LOCAL_RULES, AiFeatureExecution.ANDROID_ON_DEVICE_GENAI),
        feature("privacy.redact", "Datenschutz", "Kontext vor externer KI-Übergabe lokal minimieren", true, AiPrivacyClass.HIGHLY_SENSITIVE,
            AiFeatureExecution.LOCAL_RULES, AiFeatureExecution.LOCAL_MODEL_PACK),
        feature("device.adapt", "System", "Motion/Layout an Display, Akku und Eingabe anpassen", true, AiPrivacyClass.NON_SENSITIVE,
            AiFeatureExecution.LOCAL_RULES),
        feature("provider.capabilities", "KI Hub", "Veröffentlichte App-Shortcuts und Widgets entdecken", true, AiPrivacyClass.NON_SENSITIVE,
            AiFeatureExecution.APP_SHORTCUT_OR_WIDGET),
        feature("provider.route", "KI Hub", "Aufgabe zum passenden installierten KI-Ziel routen", true, AiPrivacyClass.NON_SENSITIVE,
            AiFeatureExecution.LOCAL_RULES, AiFeatureExecution.INSTALLED_APP_HANDOFF),
    )

    init {
        require(features.map(AiEverywhereFeature::id).distinct().size == features.size) {
            "AI feature ids must stay unique"
        }
    }

    fun forSurface(surface: String): List<AiEverywhereFeature> =
        features.filter { it.surface.equals(surface, ignoreCase = true) }

    private fun feature(
        id: String,
        surface: String,
        title: String,
        backgroundSafe: Boolean,
        privacyClass: AiPrivacyClass,
        vararg executions: AiFeatureExecution,
    ) = AiEverywhereFeature(
        id = id,
        surface = surface,
        title = title,
        executions = executions.toSet(),
        privacyClass = privacyClass,
        backgroundSafe = backgroundSafe,
        externalPreviewRequired = privacyClass != AiPrivacyClass.NON_SENSITIVE &&
            AiFeatureExecution.INSTALLED_APP_HANDOFF in executions,
    )
}
