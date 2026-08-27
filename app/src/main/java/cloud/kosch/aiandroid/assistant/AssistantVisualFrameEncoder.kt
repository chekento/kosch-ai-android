package cloud.kosch.aiandroid.assistant

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import android.media.Image
import androidx.camera.core.ImageProxy
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt

/** CPU work is only entered after AssistantVisualContextRuntime grants a one-shot request. */
object AssistantVisualFrameEncoder {
    data class EncodedJpeg(
        val width: Int,
        val height: Int,
        val rotationDegrees: Int,
        val bytes: ByteArray,
    )

    fun encodeScreenImage(image: Image): EncodedJpeg? {
        val plane = image.planes.firstOrNull() ?: return null
        val width = image.width.coerceAtLeast(1)
        val height = image.height.coerceAtLeast(1)
        val pixelStride = plane.pixelStride
        val rowStride = plane.rowStride
        if (pixelStride < 4 || rowStride < width * pixelStride) return null

        val paddedWidth = width + ((rowStride - width * pixelStride) / pixelStride)
        val paddedBitmap = runCatching {
            Bitmap.createBitmap(paddedWidth, height, Bitmap.Config.ARGB_8888).also { bitmap ->
                val buffer = plane.buffer.duplicate().apply { rewind() }
                bitmap.copyPixelsFromBuffer(buffer)
            }
        }.getOrNull() ?: return null

        val croppedBitmap = if (paddedWidth == width) {
            paddedBitmap
        } else {
            val cropped = runCatching { Bitmap.createBitmap(paddedBitmap, 0, 0, width, height) }.getOrNull()
            paddedBitmap.recycle()
            cropped ?: return null
        }

        return try {
            compressBitmap(croppedBitmap, rotationDegrees = 0)
        } finally {
            croppedBitmap.recycle()
        }
    }

    fun encodeCameraImage(image: ImageProxy): EncodedJpeg? {
        val rotation = normalizeRotation(image.imageInfo.rotationDegrees)
        val jpeg = when (image.format) {
            ImageFormat.JPEG -> image.planes.firstOrNull()?.buffer?.duplicate()?.let { buffer ->
                buffer.rewind()
                ByteArray(buffer.remaining()).also { bytes -> buffer.get(bytes) }
            }

            ImageFormat.YUV_420_888 -> yuv420ToJpeg(image)
            else -> null
        } ?: return null

        val bitmap = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size) ?: return null
        return try {
            compressBitmap(bitmap, rotation)
        } finally {
            bitmap.recycle()
        }
    }

    private fun yuv420ToJpeg(image: ImageProxy): ByteArray? {
        if (image.planes.size < 3) return null
        val width = image.width
        val height = image.height
        if (width <= 0 || height <= 0 || width % 2 != 0 || height % 2 != 0) return null

        val output = ByteArray(width * height * 3 / 2)
        val yPlane = image.planes[0]
        val uPlane = image.planes[1]
        val vPlane = image.planes[2]
        val yBuffer = yPlane.buffer.duplicate()
        val uBuffer = uPlane.buffer.duplicate()
        val vBuffer = vPlane.buffer.duplicate()

        var destination = 0
        for (row in 0 until height) {
            val rowStart = row * yPlane.rowStride
            for (column in 0 until width) {
                val index = rowStart + column * yPlane.pixelStride
                if (index !in 0 until yBuffer.limit()) return null
                output[destination++] = yBuffer.get(index)
            }
        }

        val chromaWidth = width / 2
        val chromaHeight = height / 2
        for (row in 0 until chromaHeight) {
            val uRowStart = row * uPlane.rowStride
            val vRowStart = row * vPlane.rowStride
            for (column in 0 until chromaWidth) {
                val uIndex = uRowStart + column * uPlane.pixelStride
                val vIndex = vRowStart + column * vPlane.pixelStride
                if (uIndex !in 0 until uBuffer.limit() || vIndex !in 0 until vBuffer.limit()) return null
                output[destination++] = vBuffer.get(vIndex)
                output[destination++] = uBuffer.get(uIndex)
            }
        }

        return runCatching {
            ByteArrayOutputStream().use { stream ->
                val success = YuvImage(output, ImageFormat.NV21, width, height, null)
                    .compressToJpeg(Rect(0, 0, width, height), CAMERA_INTERMEDIATE_QUALITY, stream)
                if (!success) null else stream.toByteArray()
            }
        }.getOrNull()
    }

    private fun compressBitmap(source: Bitmap, rotationDegrees: Int): EncodedJpeg? {
        val rotated = rotateIfNeeded(source, rotationDegrees)
        val scaled = scaleToMaxEdge(rotated, MAX_CONTEXT_EDGE)
        if (scaled !== rotated && rotated !== source) rotated.recycle()

        try {
            var bytes: ByteArray? = null
            for (quality in QUALITY_STEPS) {
                bytes = compress(scaled, quality) ?: continue
                if (bytes.size <= AssistantVisualContextRuntime.MAX_CONTEXT_BYTES) {
                    return EncodedJpeg(
                        width = scaled.width,
                        height = scaled.height,
                        rotationDegrees = 0,
                        bytes = bytes,
                    )
                }
            }

            val oversized = bytes ?: return null
            val scale = sqrt(
                AssistantVisualContextRuntime.MAX_CONTEXT_BYTES.toFloat() / oversized.size.toFloat(),
            ).coerceIn(MIN_SECOND_PASS_SCALE, MAX_SECOND_PASS_SCALE)
            val secondWidth = (scaled.width * scale).roundToInt().coerceAtLeast(MIN_CONTEXT_EDGE)
            val secondHeight = (scaled.height * scale).roundToInt().coerceAtLeast(MIN_CONTEXT_EDGE)
            val secondPass = Bitmap.createScaledBitmap(scaled, secondWidth, secondHeight, true)
            try {
                for (quality in SECOND_PASS_QUALITY_STEPS) {
                    val candidate = compress(secondPass, quality) ?: continue
                    if (candidate.size <= AssistantVisualContextRuntime.MAX_CONTEXT_BYTES) {
                        return EncodedJpeg(
                            width = secondPass.width,
                            height = secondPass.height,
                            rotationDegrees = 0,
                            bytes = candidate,
                        )
                    }
                }
            } finally {
                if (secondPass !== scaled) secondPass.recycle()
            }
            return null
        } finally {
            if (scaled !== source && !scaled.isRecycled) scaled.recycle()
        }
    }

    private fun rotateIfNeeded(source: Bitmap, rotationDegrees: Int): Bitmap {
        val rotation = normalizeRotation(rotationDegrees)
        if (rotation == 0) return source
        val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    private fun scaleToMaxEdge(source: Bitmap, maxEdge: Int): Bitmap {
        val longest = max(source.width, source.height)
        if (longest <= maxEdge) return source
        val scale = maxEdge.toFloat() / longest.toFloat()
        val width = (source.width * scale).roundToInt().coerceAtLeast(MIN_CONTEXT_EDGE)
        val height = (source.height * scale).roundToInt().coerceAtLeast(MIN_CONTEXT_EDGE)
        return Bitmap.createScaledBitmap(source, width, height, true)
    }

    private fun compress(bitmap: Bitmap, quality: Int): ByteArray? = runCatching {
        ByteArrayOutputStream().use { stream ->
            if (!bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)) null else stream.toByteArray()
        }
    }.getOrNull()

    private fun normalizeRotation(rotationDegrees: Int): Int =
        (((rotationDegrees % 360) + 360) % 360 / 90) * 90

    private const val MAX_CONTEXT_EDGE = 1280
    private const val MIN_CONTEXT_EDGE = 64
    private const val CAMERA_INTERMEDIATE_QUALITY = 90
    private const val MIN_SECOND_PASS_SCALE = 0.5f
    private const val MAX_SECOND_PASS_SCALE = 0.9f
    private val QUALITY_STEPS = intArrayOf(82, 74, 66, 58)
    private val SECOND_PASS_QUALITY_STEPS = intArrayOf(66, 58, 50)
}
