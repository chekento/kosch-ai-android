package cloud.kosch.aiandroid.model

/**
 * Focused extension registry for Home Studio object styling.
 *
 * Keeping these tokens explicit means ScopedSettingsDocument may still reject arbitrary feature ids. They use the
 * same GLOBAL → PAGE → OBJECT inheritance and portable-value contract as the main Settings catalog.
 */
object WorkspaceStyleFeatureCatalog {
    private val GPO = setOf(SettingScope.GLOBAL, SettingScope.PAGE, SettingScope.OBJECT)

    val all: List<SettingsFeatureDefinition> = listOf(
        SettingsFeatureDefinition(
            id = "workspace.style.opacity",
            section = SettingsSection.APPEARANCE,
            title = "Objekt-Deckkraft",
            scopes = GPO,
            portability = SettingPortability.PORTABLE,
            maturity = SettingMaturity.LIVE,
            keywords = setOf("home studio", "object", "alpha", "transparency"),
        ),
        SettingsFeatureDefinition(
            id = "workspace.style.corner_dp",
            section = SettingsSection.APPEARANCE,
            title = "Objekt-Eckenradius",
            scopes = GPO,
            portability = SettingPortability.PORTABLE,
            maturity = SettingMaturity.LIVE,
            keywords = setOf("home studio", "object", "corner", "radius"),
        ),
        SettingsFeatureDefinition(
            id = "workspace.style.rotation_deg",
            section = SettingsSection.APPEARANCE,
            title = "Objekt-Rotation",
            scopes = GPO,
            portability = SettingPortability.PORTABLE,
            maturity = SettingMaturity.LIVE,
            keywords = setOf("home studio", "object", "rotate", "rotation"),
        ),
    )
}

/** One lookup surface for persistence validation; duplicate ids are a programmer error. */
object SettingsFeatureRegistry {
    val all: List<SettingsFeatureDefinition> = (SettingsFeatureCatalog.all + WorkspaceStyleFeatureCatalog.all).also { definitions ->
        require(definitions.map(SettingsFeatureDefinition::id).distinct().size == definitions.size) {
            "Duplicate settings feature id"
        }
    }
}
