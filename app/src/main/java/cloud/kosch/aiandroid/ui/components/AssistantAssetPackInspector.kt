package cloud.kosch.aiandroid.ui.components

import android.content.Context

/**
 * Inventories packaged Assistant WebP files without decoding them.
 *
 * APK assets are immutable for the process lifetime, so the result is cached. Other assistants and
 * common portal themes are intentionally left to their own manifests rather than being classified as
 * unexpected files for the Default Assistant.
 */
class AssistantAssetPackInspector(context: Context) {
    private val assets = context.applicationContext.assets

    private val defaultAudit: AssistantAssetPackAudit by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        val manifest = DefaultAssistantAssetManifest.manifest
        manifest.audit(discoverPresentWebpPaths(manifest))
    }

    fun auditDefault(): AssistantAssetPackAudit = defaultAudit

    private fun discoverPresentWebpPaths(manifest: AssistantAssetManifest): Set<String> = buildSet {
        collectWebpFiles("assistant/${manifest.assistantId}/body")
        collectWebpFiles("assistant/${manifest.assistantId}/overlay")
        collectWebpFiles("assistant/${manifest.assistantId}/fx")
        collectWebpFiles(
            directory = "assistant/common/fx",
            filePrefix = "portal_${manifest.portalThemeId}_",
        )
    }

    private fun MutableSet<String>.collectWebpFiles(
        directory: String,
        filePrefix: String? = null,
    ) {
        assets.list(directory)
            .orEmpty()
            .asSequence()
            .filter { it.endsWith(".webp") }
            .filter { filePrefix == null || it.startsWith(filePrefix) }
            .forEach { add("$directory/$it") }
    }
}
