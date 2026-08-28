package cloud.kosch.aiandroid.model

/**
 * Explicit Home Studio style-token registry.
 *
 * Tokens remain whitelisted so portable scoped settings cannot smuggle arbitrary feature ids into backup/import.
 * They stay CORE_READY until renderer + editor + accessibility + persistence tests meet the full LIVE contract.
 */
object WorkspaceStyleFeatureCatalog {
    private val GPO = setOf(SettingScope.GLOBAL, SettingScope.PAGE, SettingScope.OBJECT)

    val all: List<SettingsFeatureDefinition> = listOf(
        style("workspace.style.visible", "Objekt-Sichtbarkeit", setOf("visible", "visibility", "hide", "show")),
        style("workspace.style.opacity", "Objekt-Deckkraft", setOf("alpha", "opacity", "transparency")),
        style("workspace.style.rotation_deg", "Objekt-Rotation", setOf("rotate", "rotation", "angle")),
        style("workspace.style.offset_x_dp", "Objekt-Feinversatz X", setOf("offset", "x", "nudge", "freeform")),
        style("workspace.style.offset_y_dp", "Objekt-Feinversatz Y", setOf("offset", "y", "nudge", "freeform")),
        style("workspace.style.z_index", "Objekt-Ebene / Z-Index", setOf("layer", "z", "front", "back")),
        style("workspace.style.corner_dp", "Objekt-Eckenradius", setOf("corner", "radius", "round")),
        style("workspace.style.content_scale", "Objekt-Inhaltsskalierung", setOf("scale", "content", "size")),
        style("workspace.style.content_padding_dp", "Objekt-Innenabstand", setOf("padding", "spacing", "content")),
        style("workspace.style.label_visible", "Objekt-Label sichtbar", setOf("label", "name", "text", "hide")),
        style("workspace.style.label_scale", "Objekt-Label-Skalierung", setOf("label", "font", "text", "scale")),
        style("workspace.style.background_alpha", "Objekt-Hintergrunddeckkraft", setOf("background", "surface", "alpha")),
        style("workspace.style.background_argb", "Objekt-Hintergrundfarbe", setOf("background", "color", "argb")),
        style("workspace.style.border_width_dp", "Objekt-Rahmenbreite", setOf("border", "stroke", "width")),
        style("workspace.style.border_argb", "Objekt-Rahmenfarbe", setOf("border", "stroke", "color", "argb")),
        style("workspace.style.elevation_dp", "Objekt-Elevation", setOf("shadow", "elevation", "depth")),
    )

    private fun style(
        id: String,
        title: String,
        keywords: Set<String>,
    ) = SettingsFeatureDefinition(
        id = id,
        section = SettingsSection.APPEARANCE,
        title = title,
        scopes = GPO,
        portability = SettingPortability.PORTABLE,
        maturity = SettingMaturity.CORE_READY,
        keywords = keywords + setOf("home studio", "object", "style"),
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
