package cloud.kosch.aiandroid.ui.components

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import cloud.kosch.aiandroid.model.AssistantVisualState
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap

/**
 * Safe runtime boundary for the matrix-defined Assistant WebP assets.
 *
 * Android's `src/main/assets/` directory is exposed through AssetManager without the leading
 * `assets/` segment. Missing, oversized, malformed or wrongly sized sprites are treated exactly like
 * absent sprites so HOME can always fall back to the Canvas avatar.
 */
class AssistantAssetRuntime(context: Context) {
    private val assets = context.applicationContext.assets
    private val decoded = ConcurrentHashMap<String, AssistantDecodedAsset>()
    private val unavailable = ConcurrentHashMap.newKeySet<String>()

    fun loadState(
        state: AssistantVisualState,
        assistantId: String = AssistantAssetCatalog.DEFAULT_ASSISTANT_ID,
    ): AssistantSpriteFrame? {
        val bodyFile = AssistantAssetCatalog.bodyFile(state, assistantId) ?: return null
        val body = load(
            path = AssistantAssetPaths.body(assistantId, bodyFile),
            contract = AssistantAssetContract.BODY,
        ) ?: return null

        val eye = load(
            path = AssistantAssetPaths.overlay(assistantId, AssistantAssetCatalog.eyeFile(state, assistantId)),
            contract = AssistantAssetContract.OVERLAY,
        )
        val mouth = load(
            path = AssistantAssetPaths.overlay(
                assistantId,
                AssistantAssetCatalog.mouthVisemeFile(
                    if (state == AssistantVisualState.SPEAKING) "aa" else "sil",
                    assistantId,
                ),
            ),
            contract = AssistantAssetContract.OVERLAY,
        )

        return AssistantSpriteFrame(body = body, eye = eye, mouth = mouth)
    }

    fun loadSpawn(
        frame: Int,
        assistantId: String = AssistantAssetCatalog.DEFAULT_ASSISTANT_ID,
    ): AssistantDecodedAsset? {
        require(frame in 0..15) { "Spawn-Frame außerhalb 000..015: $frame" }
        return load(
            AssistantAssetPaths.body(assistantId, AssistantAssetCatalog.spawnFiles(assistantId)[frame]),
            AssistantAssetContract.BODY,
        )
    }

    fun loadTurnY(
        degrees: Int,
        assistantId: String = AssistantAssetCatalog.DEFAULT_ASSISTANT_ID,
    ): AssistantDecodedAsset? {
        require(degrees in 0..345 && degrees % 15 == 0) { "Y-Rotation muss 000..345 in 15°-Schritten sein" }
        val file = "asst_${assistantId}_turn_y_${degrees.toString().padStart(3, '0')}.webp"
        return load(AssistantAssetPaths.body(assistantId, file), AssistantAssetContract.BODY)
    }

    fun loadPortal(
        frame: Int,
        themeId: String = AssistantAssetCatalog.DEFAULT_THEME_ID,
    ): AssistantDecodedAsset? {
        require(frame in 0..7) { "Portal-Frame außerhalb 000..007: $frame" }
        return load(
            AssistantAssetPaths.commonFx(AssistantAssetCatalog.portalFiles(themeId)[frame]),
            AssistantAssetContract.PORTAL,
        )
    }

    private fun load(path: String, contract: AssistantAssetContract): AssistantDecodedAsset? {
        decoded[path]?.let { return it }
        if (path in unavailable) return null

        val result = runCatching {
            val bytes = assets.open(path, android.content.res.AssetManager.ACCESS_STREAMING).use { stream ->
                stream.readBounded(contract.maxBytes)
            }
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                ?: error("Sprite konnte nicht dekodiert werden")
            if (bitmap.width != contract.pixelSize || bitmap.height != contract.pixelSize) {
                bitmap.recycle()
                error("Unerwartete Sprite-Größe ${bitmap.width}x${bitmap.height}")
            }
            AssistantDecodedAsset(
                image = bitmap.asImageBitmap(),
                path = path,
                byteCount = bytes.size,
                pixelSize = contract.pixelSize,
            )
        }

        return result.getOrNull()?.also { decoded[path] = it }
            ?: null.also { unavailable += path }
    }
}

data class AssistantSpriteFrame(
    val body: AssistantDecodedAsset,
    val eye: AssistantDecodedAsset?,
    val mouth: AssistantDecodedAsset?,
)

data class AssistantDecodedAsset(
    val image: ImageBitmap,
    val path: String,
    val byteCount: Int,
    val pixelSize: Int,
)

enum class AssistantAssetContract(
    val pixelSize: Int,
    val maxBytes: Int,
) {
    BODY(pixelSize = 384, maxBytes = 35 * 1024),
    OVERLAY(pixelSize = 128, maxBytes = 12 * 1024),
    PORTAL(pixelSize = 256, maxBytes = 20 * 1024),
}

/** Pure path resolver; rejects traversal and non-matrix Android filenames before AssetManager sees them. */
object AssistantAssetPaths {
    private val idPattern = Regex("^[a-z0-9_]+$")
    private val filePattern = Regex("^[a-z0-9_]+\\.webp$")

    fun body(assistantId: String, fileName: String): String =
        "assistant/${safeId(assistantId)}/body/${safeFile(fileName)}"

    fun overlay(assistantId: String, fileName: String): String =
        "assistant/${safeId(assistantId)}/overlay/${safeFile(fileName)}"

    fun assistantFx(assistantId: String, fileName: String): String =
        "assistant/${safeId(assistantId)}/fx/${safeFile(fileName)}"

    fun commonFx(fileName: String): String = "assistant/common/fx/${safeFile(fileName)}"

    private fun safeId(value: String): String {
        require(idPattern.matches(value)) { "Ungültige Assistant-ID" }
        return value
    }

    private fun safeFile(value: String): String {
        require(filePattern.matches(value)) { "Ungültiger Assistant-Asset-Dateiname" }
        return value
    }
}

private fun InputStream.readBounded(maxBytes: Int): ByteArray {
    val output = ByteArrayOutputStream(maxBytes.coerceAtMost(8 * 1024))
    val buffer = ByteArray(4 * 1024)
    var total = 0
    while (true) {
        val read = read(buffer)
        if (read < 0) break
        total += read
        require(total <= maxBytes) { "Assistant-Asset überschreitet das Größenbudget" }
        output.write(buffer, 0, read)
    }
    return output.toByteArray()
}
