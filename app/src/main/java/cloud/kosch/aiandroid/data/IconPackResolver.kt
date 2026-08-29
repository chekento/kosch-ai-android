package cloud.kosch.aiandroid.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream
import java.util.Locale

/** Minimal installed icon-pack descriptor; no app inventory or icon mapping leaves the device. */
data class InstalledIconPack(
    val packageName: String,
    val label: String,
)

/**
 * Resolver for the de-facto ADW/Nova appfilter.xml icon-pack format.
 *
 * Only a package explicitly selected in launcher settings is used for icon replacement. Unsupported/malformed packs,
 * missing entries and missing drawables fail back to the app's Android-provided icon. The resolver never broad-scans
 * arbitrary installed packages; discovery is limited to the icon-pack intent families declared in manifest <queries>.
 */
class IconPackResolver(context: Context) {
    private val appContext = context.applicationContext
    private val packageManager = appContext.packageManager
    private val cache = mutableMapOf<String, CachedPack>()

    fun resolve(
        iconPackPackage: String?,
        componentName: ComponentName,
        fallback: Drawable,
    ): Drawable {
        val packageName = iconPackPackage?.trim()?.takeIf(String::isNotBlank) ?: return fallback
        val mapping = loadPack(packageName) ?: return fallback
        val drawableName = mapping.entries[IconPackComponentKey.from(componentName)] ?: return fallback
        val identifier = mapping.resources.getIdentifier(drawableName, "drawable", packageName)
            .takeIf { it != 0 }
            ?: mapping.resources.getIdentifier(drawableName, "mipmap", packageName).takeIf { it != 0 }
            ?: return fallback
        return runCatching { mapping.resources.getDrawable(identifier, null) }.getOrNull() ?: fallback
    }

    fun discover(): List<InstalledIconPack> {
        val packages = ICON_PACK_ACTIONS
            .flatMap { action ->
                packageManager.queryIntentActivities(Intent(action), PackageManager.MATCH_DEFAULT_ONLY)
            }
            .mapNotNull { it.activityInfo?.packageName }
            .distinct()
            .sorted()
        return packages.mapNotNull { packageName ->
            runCatching {
                val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
                InstalledIconPack(
                    packageName = packageName,
                    label = packageManager.getApplicationLabel(applicationInfo).toString().ifBlank { packageName },
                )
            }.getOrNull()
        }.sortedBy { it.label.lowercase(Locale.getDefault()) }
    }

    fun invalidate(packageName: String? = null) {
        if (packageName == null) cache.clear() else cache.remove(packageName)
    }

    private fun loadPack(packageName: String): LoadedPack? {
        val lastUpdate = runCatching { packageManager.getPackageInfo(packageName, 0).lastUpdateTime }.getOrNull()
            ?: return null
        cache[packageName]?.takeIf { it.lastUpdateTime == lastUpdate }?.let { return it.loaded }

        val loaded = runCatching {
            val resources = packageManager.getResourcesForApplication(packageName)
            val entries = readEntries(packageName, resources)
            LoadedPack(resources, entries)
        }.getOrNull()
        cache[packageName] = CachedPack(lastUpdate, loaded)
        return loaded
    }

    private fun readEntries(packageName: String, resources: android.content.res.Resources): Map<String, String> {
        val xmlId = resources.getIdentifier("appfilter", "xml", packageName)
        if (xmlId != 0) {
            return resources.getXml(xmlId).use { parser -> parse(parser) }
        }

        val rawId = resources.getIdentifier("appfilter", "raw", packageName)
        if (rawId != 0) {
            return resources.openRawResource(rawId).use(::parseStream)
        }

        return runCatching { resources.assets.open("appfilter.xml").use(::parseStream) }
            .getOrDefault(emptyMap())
    }

    private fun parseStream(input: InputStream): Map<String, String> {
        val parser = XmlPullParserFactory.newInstance().newPullParser().apply {
            setInput(input, "UTF-8")
        }
        return parse(parser)
    }

    private fun parse(parser: XmlPullParser): Map<String, String> {
        val entries = LinkedHashMap<String, String>()
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT && entries.size < MAX_PACK_ENTRIES) {
            if (event == XmlPullParser.START_TAG && parser.name.equals("item", ignoreCase = true)) {
                val component: String? = parser.getAttributeValue(null, "component")
                val drawable: String? = parser.getAttributeValue(null, "drawable")
                    ?.trim()
                    ?.take(MAX_DRAWABLE_NAME)
                val key = component?.let { IconPackComponentKey.normalize(it) }
                if (key != null && !drawable.isNullOrBlank() && DRAWABLE_NAME.matches(drawable)) {
                    entries.putIfAbsent(key, drawable)
                }
            }
            event = parser.next()
        }
        return entries
    }

    private data class LoadedPack(
        val resources: android.content.res.Resources,
        val entries: Map<String, String>,
    )

    private data class CachedPack(
        val lastUpdateTime: Long,
        val loaded: LoadedPack?,
    )

    private companion object {
        val ICON_PACK_ACTIONS = listOf(
            "com.novalauncher.THEME",
            "org.adw.launcher.THEMES",
            "app.lawnchair.icons.THEMED_ICON",
            "ch.deletescape.lawnchair.ICONPACK",
        )
        val DRAWABLE_NAME = Regex("[A-Za-z0-9_]{1,160}")
        const val MAX_DRAWABLE_NAME = 160
        const val MAX_PACK_ENTRIES = 50_000
    }
}

/** Canonical appfilter component key independent of shorthand class notation. */
object IconPackComponentKey {
    fun from(componentName: ComponentName): String =
        "${componentName.packageName}/${componentName.className}"

    fun normalize(raw: String): String? {
        val value = raw.trim()
            .removePrefix("ComponentInfo{")
            .removeSuffix("}")
        val split = value.split('/', limit = 2)
        if (split.size != 2) return null
        val packageName = split[0].trim()
        val classNameRaw = split[1].trim()
        if (!PACKAGE.matches(packageName) || classNameRaw.isBlank()) return null
        val className = if (classNameRaw.startsWith('.')) packageName + classNameRaw else classNameRaw
        if (!CLASS.matches(className)) return null
        return "$packageName/$className"
    }

    private val PACKAGE = Regex("[A-Za-z][A-Za-z0-9_]*(?:\\.[A-Za-z][A-Za-z0-9_]*)+")
    private val CLASS = Regex("[A-Za-z_$][A-Za-z0-9_$.]*")
}
