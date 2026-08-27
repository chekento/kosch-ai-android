package cloud.kosch.aiandroid.ui.components

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import cloud.kosch.aiandroid.assistant.AssistantObservationRuntime
import java.util.concurrent.Executors

/**
 * Visible-only CameraX session for Assistant Camera Awareness.
 *
 * Images are deliberately not converted, retained or persisted in Stage G. The analyzer proves that
 * live frames are flowing and closes every ImageProxy immediately. Leaving this composable unbinds
 * the camera and tears down the analyzer executor.
 */
@Composable
fun AssistantCameraSessionPreview(
    onStarted: () -> Unit,
    onStopped: () -> Unit,
    onFailure: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findCameraActivity() }
    val previewView = remember(context) {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    val cameraProviderFuture = remember(context.applicationContext) {
        ProcessCameraProvider.getInstance(context.applicationContext)
    }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(activity, previewView, cameraProviderFuture) {
        if (activity == null) {
            AssistantObservationRuntime.cameraStopped()
            onFailure("Keine sichtbare Activity für die Kamera-Session verfügbar")
            onDispose { analysisExecutor.shutdownNow() }
        } else {
            var provider: ProcessCameraProvider? = null
            var analysis: ImageAnalysis? = null
            var started = false

            val listener = Runnable {
                runCatching {
                    val resolvedProvider = cameraProviderFuture.get()
                    val resolvedPreview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    val resolvedAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { useCase ->
                            useCase.setAnalyzer(analysisExecutor) { image ->
                                AssistantObservationRuntime.cameraFrameObserved()
                                image.close()
                            }
                        }
                    resolvedProvider.unbindAll()
                    resolvedProvider.bindToLifecycle(
                        activity,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        resolvedPreview,
                        resolvedAnalysis,
                    )
                    provider = resolvedProvider
                    analysis = resolvedAnalysis
                    started = true
                    AssistantObservationRuntime.cameraStarted()
                    onStarted()
                }.onFailure { error ->
                    AssistantObservationRuntime.cameraStopped()
                    onFailure(error.message ?: "CameraX-Session konnte nicht gestartet werden")
                }
            }
            cameraProviderFuture.addListener(listener, ContextCompat.getMainExecutor(context))

            onDispose {
                analysis?.clearAnalyzer()
                provider?.unbindAll()
                analysisExecutor.shutdownNow()
                AssistantObservationRuntime.cameraStopped()
                if (started) onStopped()
            }
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(4f / 3f)
            .clip(RoundedCornerShape(18.dp)),
    )
}

private tailrec fun Context.findCameraActivity(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.findCameraActivity()
    else -> null
}
