package cloud.kosch.aiandroid.data

import java.nio.charset.StandardCharsets
import java.util.Base64

/**
 * Plaintext payload that is authenticated/encrypted by PortableBackupCodec.
 *
 * Keeping this inner container versioned lets the encryption envelope remain stable while new portable launcher
 * domains are added. Every section is independently bounded and validated by its owning codec/store before restore.
 */
data class PortableLauncherBackupBundle(
    val schemaVersion: Int = SCHEMA_VERSION,
    val createdAtEpochMillis: Long,
    val workspacePayload: String,
    val launcherSettingsPayload: String,
    val scopedSettingsPayload: String,
    val customActionsPayload: String,
    val assistantPreferencesPayload: String,
) {
    init {
        require(schemaVersion == SCHEMA_VERSION) { "Unsupported launcher backup bundle schema" }
        require(createdAtEpochMillis > 0L) { "Invalid launcher backup creation time" }
        require(workspacePayload.isNotBlank()) { "Workspace backup section is required" }
    }

    companion object {
        const val SCHEMA_VERSION = 1
    }
}

data class PortableBackupSectionDiff(
    val section: String,
    val present: Boolean,
    val changed: Boolean,
)

data class PortableLauncherBackupDiff(
    val legacyWorkspaceOnly: Boolean,
    val sections: List<PortableBackupSectionDiff>,
) {
    val changedSectionCount: Int get() = sections.count(PortableBackupSectionDiff::changed)
}

/** Deterministic, bounded wire codec for the inner portable backup bundle. */
object PortableLauncherBackupBundleCodec {
    private const val MAGIC = "KOSCH-LAUNCHER-BUNDLE"
    private const val MAX_BUNDLE_BYTES = 4 * 1024 * 1024
    private const val MAX_SECTION_BYTES = 3 * 1024 * 1024

    fun encode(bundle: PortableLauncherBackupBundle): ByteArray {
        require(bundle.schemaVersion == PortableLauncherBackupBundle.SCHEMA_VERSION)
        val sections = linkedMapOf(
            "workspace" to bundle.workspacePayload,
            "settings" to bundle.launcherSettingsPayload,
            "scoped" to bundle.scopedSettingsPayload,
            "actions" to bundle.customActionsPayload,
            "assistant" to bundle.assistantPreferencesPayload,
        )
        sections.values.forEach(::requireSectionBudget)
        val payload = buildString {
            append(MAGIC).append('|')
                .append(bundle.schemaVersion).append('|')
                .append(bundle.createdAtEpochMillis).append('\n')
            sections.forEach { (key, value) ->
                append(key).append('|').append(b64(value)).append('\n')
            }
        }.toByteArray(StandardCharsets.UTF_8)
        require(payload.size <= MAX_BUNDLE_BYTES) { "Launcher backup bundle is too large" }
        return payload
    }

    /** Returns null when the payload is a pre-bundle legacy Workspace backup. */
    fun decodeOrNull(payload: ByteArray): PortableLauncherBackupBundle? {
        require(payload.isNotEmpty()) { "Backup payload is empty" }
        require(payload.size <= MAX_BUNDLE_BYTES) { "Launcher backup bundle is too large" }
        val text = payload.toString(StandardCharsets.UTF_8)
        val lines = text.lineSequence().filter(String::isNotBlank).toList()
        val header = lines.firstOrNull() ?: return null
        if (!header.startsWith("$MAGIC|")) return null
        val headerParts = header.split('|')
        require(headerParts.size == 3) { "Malformed launcher backup header" }
        val version = headerParts[1].toIntOrNull()
            ?: throw IllegalArgumentException("Invalid launcher backup schema")
        require(version == PortableLauncherBackupBundle.SCHEMA_VERSION) {
            "Unsupported launcher backup bundle schema: $version"
        }
        val createdAt = headerParts[2].toLongOrNull()?.takeIf { it > 0L }
            ?: throw IllegalArgumentException("Invalid launcher backup creation time")

        val sectionLines = lines.drop(1)
        require(sectionLines.size == ALLOWED_SECTIONS.size) { "Launcher backup bundle has an invalid section count" }
        val records = linkedMapOf<String, String>()
        sectionLines.forEach { line ->
            val separator = line.indexOf('|')
            require(separator > 0) { "Malformed launcher backup section" }
            val key = line.substring(0, separator)
            require(key in ALLOWED_SECTIONS) { "Unknown launcher backup section: $key" }
            require(key !in records) { "Duplicate launcher backup section: $key" }
            records[key] = line.substring(separator + 1)
        }
        require(records.keys == ALLOWED_SECTIONS) { "Launcher backup bundle is incomplete" }
        fun section(name: String): String = unb64(records.getValue(name)).also(::requireSectionBudget)

        return PortableLauncherBackupBundle(
            schemaVersion = version,
            createdAtEpochMillis = createdAt,
            workspacePayload = section("workspace"),
            launcherSettingsPayload = section("settings"),
            scopedSettingsPayload = section("scoped"),
            customActionsPayload = section("actions"),
            assistantPreferencesPayload = section("assistant"),
        )
    }

    fun isBundle(payload: ByteArray): Boolean = payload
        .toString(StandardCharsets.UTF_8)
        .lineSequence()
        .firstOrNull()
        ?.startsWith("$MAGIC|") == true

    private fun requireSectionBudget(value: String) {
        require(value.toByteArray(StandardCharsets.UTF_8).size <= MAX_SECTION_BYTES) {
            "Launcher backup section is too large"
        }
    }

    private fun b64(value: String): String = Base64.getUrlEncoder().withoutPadding()
        .encodeToString(value.toByteArray(StandardCharsets.UTF_8))

    private fun unb64(value: String): String = try {
        String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8)
    } catch (exception: IllegalArgumentException) {
        throw IllegalArgumentException("Invalid launcher backup section encoding", exception)
    }

    private val ALLOWED_SECTIONS = linkedSetOf("workspace", "settings", "scoped", "actions", "assistant")
}
