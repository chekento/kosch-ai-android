package cloud.kosch.aiandroid.model

const val SCOPED_SETTINGS_SCHEMA_VERSION = 1
const val MAX_SCOPED_SETTING_RECORDS = 2_048
private const val MAX_SCOPE_OWNER_ID = 220
private const val MAX_SETTING_TEXT = 2_048

/** Typed portable value used by page/object setting overrides. */
sealed interface PortableSettingValue {
    data class Bool(val value: Boolean) : PortableSettingValue
    data class Integer(val value: Int) : PortableSettingValue
    data class Decimal(val value: Double) : PortableSettingValue
    data class Text(val value: String) : PortableSettingValue
}

/**
 * Portable overrides are deliberately separate from the Workspace layout schema and from device/session state.
 * Missing entries mean INHERIT. Global values continue to live in their owning settings domain.
 */
data class ScopedSettingsDocument(
    val schemaVersion: Int = SCOPED_SETTINGS_SCHEMA_VERSION,
    val pageOverrides: Map<String, Map<String, PortableSettingValue>> = emptyMap(),
    val objectOverrides: Map<String, Map<String, PortableSettingValue>> = emptyMap(),
) {
    fun normalized(): ScopedSettingsDocument {
        require(schemaVersion == SCOPED_SETTINGS_SCHEMA_VERSION) { "Unsupported scoped settings schema" }
        val pages = normalizeOwnerMap(pageOverrides, SettingScope.PAGE)
        val objects = normalizeOwnerMap(objectOverrides, SettingScope.OBJECT)
        require(pages.values.sumOf(Map<String, PortableSettingValue>::size) +
            objects.values.sumOf(Map<String, PortableSettingValue>::size) <= MAX_SCOPED_SETTING_RECORDS) {
            "Too many scoped setting overrides"
        }
        return copy(pageOverrides = pages, objectOverrides = objects)
    }

    fun pageOverride(pageId: String, featureId: String): PortableSettingValue? =
        pageOverrides[pageId]?.get(featureId)

    fun objectOverride(itemId: String, featureId: String): PortableSettingValue? =
        objectOverrides[itemId]?.get(featureId)

    fun withPageOverride(
        pageId: String,
        featureId: String,
        value: PortableSettingValue?,
    ): ScopedSettingsDocument = copy(
        pageOverrides = mutate(pageOverrides, pageId, featureId, value, SettingScope.PAGE),
    ).normalized()

    fun withObjectOverride(
        itemId: String,
        featureId: String,
        value: PortableSettingValue?,
    ): ScopedSettingsDocument = copy(
        objectOverrides = mutate(objectOverrides, itemId, featureId, value, SettingScope.OBJECT),
    ).normalized()

    fun prunedTo(workspace: WorkspaceDocument): ScopedSettingsDocument {
        val pageIds = workspace.pages.map(WorkspacePage::id).toSet()
        val itemIds = workspace.pages.flatMap(WorkspacePage::items).map(WorkspaceItem::id).toSet()
        return copy(
            pageOverrides = pageOverrides.filterKeys(pageIds::contains),
            objectOverrides = objectOverrides.filterKeys(itemIds::contains),
        ).normalized()
    }

    private fun normalizeOwnerMap(
        source: Map<String, Map<String, PortableSettingValue>>,
        scope: SettingScope,
    ): Map<String, Map<String, PortableSettingValue>> = source.mapNotNull { (rawOwner, rawValues) ->
        val owner = normalizeOwnerId(rawOwner)
        val values = rawValues.mapNotNull valueLoop@{ (rawFeatureId, rawValue) ->
            val featureId = rawFeatureId.trim()
            if (featureId.isBlank()) return@valueLoop null
            require(SettingsScopeResolver.canOverride(featureId, scope)) {
                "$featureId cannot be persisted at $scope scope"
            }
            featureId to normalizeValue(rawValue)
        }.toMap()
        if (values.isEmpty()) null else owner to values
    }.toMap()

    private fun mutate(
        source: Map<String, Map<String, PortableSettingValue>>,
        rawOwnerId: String,
        featureId: String,
        value: PortableSettingValue?,
        scope: SettingScope,
    ): Map<String, Map<String, PortableSettingValue>> {
        val ownerId = normalizeOwnerId(rawOwnerId)
        require(SettingsScopeResolver.canOverride(featureId, scope)) {
            "$featureId cannot be persisted at $scope scope"
        }
        val values = source[ownerId].orEmpty().toMutableMap()
        if (value == null) values.remove(featureId) else values[featureId] = normalizeValue(value)
        val result = source.toMutableMap()
        if (values.isEmpty()) result.remove(ownerId) else result[ownerId] = values
        return result
    }

    private fun normalizeOwnerId(raw: String): String {
        val id = raw.trim()
        require(id.isNotBlank() && id.length <= MAX_SCOPE_OWNER_ID) { "Invalid scoped setting owner id" }
        require(id.none(Char::isISOControl)) { "Scoped setting owner id contains control characters" }
        return id
    }

    private fun normalizeValue(value: PortableSettingValue): PortableSettingValue = when (value) {
        is PortableSettingValue.Bool -> value
        is PortableSettingValue.Integer -> value
        is PortableSettingValue.Decimal -> {
            require(value.value.isFinite()) { "Scoped decimal must be finite" }
            value
        }
        is PortableSettingValue.Text -> PortableSettingValue.Text(value.value.take(MAX_SETTING_TEXT))
    }
}
