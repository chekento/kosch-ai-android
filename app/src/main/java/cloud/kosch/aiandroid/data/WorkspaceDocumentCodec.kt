package cloud.kosch.aiandroid.data

import cloud.kosch.aiandroid.model.SceneId
import cloud.kosch.aiandroid.model.TileAction
import cloud.kosch.aiandroid.model.WORKSPACE_SCHEMA_VERSION
import cloud.kosch.aiandroid.model.WorkspaceCellBounds
import cloud.kosch.aiandroid.model.WorkspaceDocument
import cloud.kosch.aiandroid.model.WorkspaceGridSpec
import cloud.kosch.aiandroid.model.WorkspaceItem
import cloud.kosch.aiandroid.model.WorkspaceItemContent
import cloud.kosch.aiandroid.model.WorkspaceItemKind
import cloud.kosch.aiandroid.model.WorkspacePage
import org.json.JSONArray
import org.json.JSONObject
import java.nio.charset.StandardCharsets

/**
 * Strict portable representation of the unified workspace.
 *
 * Android widget host IDs and other device-bound bindings are deliberately rejected here. They live in a
 * separate local store and must be remapped after a cross-device restore.
 */
object WorkspaceDocumentCodec {
    const val MAX_ENCODED_BYTES = 1024 * 1024
    const val MAX_PAGES = 32
    const val MAX_ITEMS_PER_PAGE = 512
    const val MAX_TOTAL_ITEMS = 4096
    private const val MAX_ID_LENGTH = 240
    private const val MAX_TITLE_LENGTH = 160
    private const val MAX_REFERENCE_LENGTH = 512

    fun encode(document: WorkspaceDocument): String {
        val normalized = document.normalized()
        require(normalized.pages.size <= MAX_PAGES) { "Workspace contains too many pages" }
        require(normalized.pages.sumOf { it.items.size } <= MAX_TOTAL_ITEMS) { "Workspace contains too many items" }
        normalized.pages.forEach { page ->
            require(page.items.size <= MAX_ITEMS_PER_PAGE) { "Workspace page contains too many items" }
        }

        val encoded = toJson(normalized).toString()
        require(encoded.toByteArray(StandardCharsets.UTF_8).size <= MAX_ENCODED_BYTES) {
            "Workspace document is too large"
        }
        return encoded
    }

    fun decode(raw: String): WorkspaceDocument {
        require(raw.isNotBlank()) { "Workspace document is empty" }
        require(raw.toByteArray(StandardCharsets.UTF_8).size <= MAX_ENCODED_BYTES) {
            "Workspace document is too large"
        }

        val root = JSONObject(raw)
        require(!root.has("widgetBindings")) { "Device widget bindings are not portable workspace data" }
        val schema = root.optInt("schemaVersion", -1)
        require(schema == WORKSPACE_SCHEMA_VERSION) { "Unsupported workspace schema $schema" }

        val gridJson = root.optJSONObject("grid") ?: error("Workspace grid is missing")
        val grid = WorkspaceGridSpec(
            columns = gridJson.requiredInt("columns"),
            rows = gridJson.requiredInt("rows"),
        )
        val activePageId = boundedString(root, "activePageId", MAX_ID_LENGTH)
        val pagesJson = root.optJSONArray("pages") ?: error("Workspace pages are missing")
        require(pagesJson.length() in 1..MAX_PAGES) { "Workspace page count is out of range" }

        val seenPageIds = mutableSetOf<String>()
        val seenItemIds = mutableSetOf<String>()
        var totalItems = 0
        val pages = buildList {
            repeat(pagesJson.length()) { pageIndex ->
                val pageJson = pagesJson.optJSONObject(pageIndex) ?: error("Workspace page is invalid")
                val id = boundedString(pageJson, "id", MAX_ID_LENGTH)
                require(seenPageIds.add(id)) { "Duplicate workspace page id" }
                val title = boundedString(pageJson, "title", MAX_TITLE_LENGTH)
                val order = pageJson.requiredInt("order")
                val sceneAdapter = pageJson.optString("sceneAdapter")
                    .takeIf(String::isNotBlank)
                    ?.let { enumValue<SceneId>(it, "scene adapter") }
                val itemsJson = pageJson.optJSONArray("items") ?: JSONArray()
                require(itemsJson.length() <= MAX_ITEMS_PER_PAGE) { "Workspace page contains too many items" }
                totalItems += itemsJson.length()
                require(totalItems <= MAX_TOTAL_ITEMS) { "Workspace contains too many items" }

                val items = buildList {
                    repeat(itemsJson.length()) { itemIndex ->
                        val itemJson = itemsJson.optJSONObject(itemIndex) ?: error("Workspace item is invalid")
                        require(!itemJson.has("appWidgetId")) { "Android widget host IDs are device-bound" }
                        val itemId = boundedString(itemJson, "id", MAX_ID_LENGTH)
                        require(seenItemIds.add(itemId)) { "Duplicate workspace item id" }
                        val boundsJson = itemJson.optJSONObject("bounds") ?: error("Workspace item bounds are missing")
                        val contentJson = itemJson.optJSONObject("content") ?: error("Workspace item content is missing")
                        require(!contentJson.has("appWidgetId")) { "Android widget host IDs are device-bound" }
                        add(
                            WorkspaceItem(
                                id = itemId,
                                bounds = WorkspaceCellBounds(
                                    column = boundsJson.requiredInt("column"),
                                    row = boundsJson.requiredInt("row"),
                                    columnSpan = boundsJson.requiredInt("columnSpan"),
                                    rowSpan = boundsJson.requiredInt("rowSpan"),
                                ),
                                content = decodeContent(contentJson),
                            ),
                        )
                    }
                }
                add(WorkspacePage(id, title, order, sceneAdapter, items))
            }
        }

        require(seenPageIds.contains(activePageId)) { "Workspace active page does not exist" }
        return WorkspaceDocument(
            schemaVersion = schema,
            grid = grid,
            activePageId = activePageId,
            pages = pages,
        ).normalized()
    }

    fun toJson(document: WorkspaceDocument): JSONObject {
        val normalized = document.normalized()
        val pages = JSONArray()
        normalized.pages.forEach { page ->
            val items = JSONArray()
            page.items.forEach { item ->
                items.put(
                    JSONObject()
                        .put("id", item.id)
                        .put(
                            "bounds",
                            JSONObject()
                                .put("column", item.bounds.column)
                                .put("row", item.bounds.row)
                                .put("columnSpan", item.bounds.columnSpan)
                                .put("rowSpan", item.bounds.rowSpan),
                        )
                        .put("content", encodeContent(item.content)),
                )
            }
            pages.put(
                JSONObject()
                    .put("id", page.id)
                    .put("title", page.title)
                    .put("order", page.order)
                    .apply { page.sceneAdapter?.let { put("sceneAdapter", it.name) } }
                    .put("items", items),
            )
        }
        return JSONObject()
            .put("schemaVersion", normalized.schemaVersion)
            .put(
                "grid",
                JSONObject()
                    .put("columns", normalized.grid.columns)
                    .put("rows", normalized.grid.rows),
            )
            .put("activePageId", normalized.activePageId)
            .put("pages", pages)
    }

    private fun encodeContent(content: WorkspaceItemContent): JSONObject = when (content) {
        is WorkspaceItemContent.ActionTile -> JSONObject()
            .put("kind", content.kind.name)
            .put("scene", content.scene.name)
            .put("legacyTileId", content.legacyTileId)
            .put("action", content.action.name)

        is WorkspaceItemContent.App -> JSONObject()
            .put("kind", content.kind.name)
            .put("appKey", content.appKey)

        is WorkspaceItemContent.Folder -> JSONObject()
            .put("kind", content.kind.name)
            .put("folderId", content.folderId)

        is WorkspaceItemContent.Widget -> JSONObject()
            .put("kind", content.kind.name)
            .apply { content.providerComponent?.let { put("providerComponent", it) } }
    }

    private fun decodeContent(root: JSONObject): WorkspaceItemContent {
        val kind = enumValue<WorkspaceItemKind>(root.optString("kind"), "workspace item kind")
        return when (kind) {
            WorkspaceItemKind.ACTION_TILE -> WorkspaceItemContent.ActionTile(
                scene = enumValue(root.optString("scene"), "action tile scene"),
                legacyTileId = boundedString(root, "legacyTileId", MAX_REFERENCE_LENGTH),
                action = enumValue<TileAction>(root.optString("action"), "action tile action"),
            )

            WorkspaceItemKind.APP -> WorkspaceItemContent.App(
                appKey = boundedString(root, "appKey", MAX_REFERENCE_LENGTH),
            )

            WorkspaceItemKind.FOLDER -> WorkspaceItemContent.Folder(
                folderId = boundedString(root, "folderId", MAX_REFERENCE_LENGTH),
            )

            WorkspaceItemKind.WIDGET -> WorkspaceItemContent.Widget(
                providerComponent = root.optString("providerComponent")
                    .takeIf(String::isNotBlank)
                    ?.also { require(it.length <= MAX_REFERENCE_LENGTH) { "Widget provider reference is too long" } },
            )
        }
    }

    private fun boundedString(root: JSONObject, key: String, maxLength: Int): String {
        val value = root.optString(key).trim()
        require(value.isNotBlank()) { "$key is missing" }
        require(value.length <= maxLength) { "$key is too long" }
        return value
    }

    private fun JSONObject.requiredInt(key: String): Int {
        require(has(key)) { "$key is missing" }
        return getInt(key)
    }

    private inline fun <reified T : Enum<T>> enumValue(value: String, label: String): T =
        enumValues<T>().firstOrNull { it.name == value }
            ?: throw IllegalArgumentException("Invalid $label")
}
