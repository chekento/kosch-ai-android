package cloud.kosch.aiandroid.model

/**
 * Pure inheritance core for the Settings Center contract "Global → Seite → Objekt".
 *
 * It deliberately handles values, not persistence. Device/session capabilities are excluded by catalog policy and
 * therefore cannot be smuggled into portable page/object overrides. UI can always offer an explicit INHERIT action.
 */
sealed interface SettingOverride<out T> {
    data object Inherit : SettingOverride<Nothing>
    data class Value<T>(val value: T) : SettingOverride<T>
}

data class ScopedSettingValue<T>(
    val featureId: String,
    val global: T,
    val page: SettingOverride<T> = SettingOverride.Inherit,
    val objectValue: SettingOverride<T> = SettingOverride.Inherit,
)

data class ResolvedSettingValue<T>(
    val value: T,
    val source: SettingScope,
)

object SettingsScopeResolver {
    fun feature(featureId: String): SettingsFeatureDefinition = SettingsFeatureCatalog.all
        .firstOrNull { it.id == featureId }
        ?: throw IllegalArgumentException("Unknown settings feature: $featureId")

    fun canOverride(featureId: String, scope: SettingScope): Boolean {
        val feature = feature(featureId)
        if (scope == SettingScope.DEVICE || scope == SettingScope.SESSION) return false
        if (feature.portability != SettingPortability.PORTABLE) return false
        return scope in feature.scopes
    }

    fun <T> resolve(scoped: ScopedSettingValue<T>): ResolvedSettingValue<T> {
        val feature = feature(scoped.featureId)
        require(SettingScope.GLOBAL in feature.scopes) {
            "${scoped.featureId} has no portable global setting contract"
        }
        require(feature.portability == SettingPortability.PORTABLE) {
            "${scoped.featureId} is not a portable scoped setting"
        }

        val objectOverride = scoped.objectValue
        if (objectOverride is SettingOverride.Value) {
            require(SettingScope.OBJECT in feature.scopes) {
                "${scoped.featureId} does not support object overrides"
            }
            return ResolvedSettingValue(objectOverride.value, SettingScope.OBJECT)
        }

        val pageOverride = scoped.page
        if (pageOverride is SettingOverride.Value) {
            require(SettingScope.PAGE in feature.scopes) {
                "${scoped.featureId} does not support page overrides"
            }
            return ResolvedSettingValue(pageOverride.value, SettingScope.PAGE)
        }

        return ResolvedSettingValue(scoped.global, SettingScope.GLOBAL)
    }
}
