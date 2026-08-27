package cloud.kosch.aiandroid.assistant

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import androidx.core.content.ContextCompat
import cloud.kosch.aiandroid.MainActivity
import cloud.kosch.aiandroid.R
import cloud.kosch.aiandroid.model.AssistantObservationSource
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Explicit MediaProjection session boundary for Assistant Screen Awareness.
 *
 * The Activity must first receive Android's capture consent result. Only then may it start this
 * foreground service and pass that one session result. The service is non-sticky and never persists
 * the result Intent. Continuous frames are closed immediately. Stage H may encode exactly one frame
 * only after AssistantVisualContextRuntime contains an explicit one-shot SCREEN request.
 */
class AssistantScreenShareService : Service() {
    private val projectionManager by lazy {
        getSystemService(MediaProjectionManager::class.java)
    }
    private val notificationManager by lazy {
        getSystemService(NotificationManager::class.java)
    }

    private var projection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var captureThread: HandlerThread? = null
    private var captureHandler: Handler? = null

    private val projectionCallback = object : MediaProjection.Callback() {
        override fun onStop() {
            releaseProjection(stopProjection = false)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSession()
                return START_NOT_STICKY
            }

            ACTION_START -> {
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
                val resultData = intent.parcelableIntentExtra(EXTRA_RESULT_DATA)
                if (resultCode != Activity.RESULT_OK || resultData == null) {
                    AssistantObservationRuntime.screenFailed("Android hat keinen gültigen Screen-Share-Consent geliefert")
                    stopSelfResult(startId)
                    return START_NOT_STICKY
                }
                startSession(resultCode, resultData)
            }

            else -> stopSelfResult(startId)
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        releaseProjection(stopProjection = true)
        super.onDestroy()
    }

    private fun startSession(resultCode: Int, resultData: Intent) {
        releaseProjection(stopProjection = true)
        ensureNotificationChannel()
        startForeground(
            NOTIFICATION_ID,
            buildNotification(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION,
        )

        val mediaProjection = runCatching {
            projectionManager.getMediaProjection(resultCode, resultData)
        }.getOrNull()
        if (mediaProjection == null) {
            AssistantObservationRuntime.screenFailed("MediaProjection konnte nicht erzeugt werden")
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return
        }

        val metrics = resources.displayMetrics
        val sourceWidth = metrics.widthPixels.coerceAtLeast(2)
        val sourceHeight = metrics.heightPixels.coerceAtLeast(2)
        val longest = max(sourceWidth, sourceHeight).toFloat()
        val scale = min(1f, MAX_CAPTURE_EDGE / longest)
        val width = (sourceWidth * scale).roundToInt().coerceAtLeast(2)
        val height = (sourceHeight * scale).roundToInt().coerceAtLeast(2)

        val thread = HandlerThread("KoSchAssistantScreenShare").apply { start() }
        val handler = Handler(thread.looper)
        val reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, MAX_IMAGES)

        reader.setOnImageAvailableListener({ source ->
            val image = runCatching { source.acquireLatestImage() }.getOrNull()
            if (image != null) {
                try {
                    AssistantObservationRuntime.screenFrameObserved()
                    val requestId = AssistantVisualContextRuntime.claimCapture(AssistantObservationSource.SCREEN)
                    if (requestId != null) {
                        val encoded = runCatching {
                            AssistantVisualFrameEncoder.encodeScreenImage(image)
                        }.getOrNull()
                        if (encoded == null) {
                            AssistantVisualContextRuntime.fail(
                                requestId,
                                "Der angeforderte Bildschirm-Kontextframe konnte nicht komprimiert werden",
                            )
                        } else {
                            AssistantVisualContextRuntime.publishJpeg(
                                requestId = requestId,
                                source = AssistantObservationSource.SCREEN,
                                width = encoded.width,
                                height = encoded.height,
                                rotationDegrees = encoded.rotationDegrees,
                                jpegBytes = encoded.bytes,
                            )
                        }
                    }
                } finally {
                    image.close()
                }
            }
        }, handler)

        mediaProjection.registerCallback(projectionCallback, handler)
        val display = runCatching {
            mediaProjection.createVirtualDisplay(
                DISPLAY_NAME,
                width,
                height,
                metrics.densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                reader.surface,
                null,
                handler,
            )
        }.getOrNull()

        if (display == null) {
            runCatching { mediaProjection.unregisterCallback(projectionCallback) }
            reader.close()
            thread.quitSafely()
            runCatching { mediaProjection.stop() }
            AssistantObservationRuntime.screenFailed("Virtuelles Display für Screen Share konnte nicht gestartet werden")
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return
        }

        captureThread = thread
        captureHandler = handler
        imageReader = reader
        virtualDisplay = display
        projection = mediaProjection
        AssistantObservationRuntime.screenStarted(width, height)
    }

    private fun stopSession() {
        releaseProjection(stopProjection = true)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun releaseProjection(stopProjection: Boolean) {
        val currentProjection = projection
        projection = null

        virtualDisplay?.release()
        virtualDisplay = null

        imageReader?.setOnImageAvailableListener(null, null)
        imageReader?.close()
        imageReader = null

        currentProjection?.let { value ->
            runCatching { value.unregisterCallback(projectionCallback) }
            if (stopProjection) runCatching { value.stop() }
        }

        captureHandler = null
        captureThread?.quitSafely()
        captureThread = null
        AssistantObservationRuntime.screenStopped()
    }

    private fun ensureNotificationChannel() {
        notificationManager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Assistant Screen Share",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Sichtbarer Status für aktive KoSch Assistant Bildschirmfreigabe"
                setShowBadge(false)
            },
        )
    }

    private fun buildNotification(): Notification {
        val openIntent = PendingIntent.getActivity(
            this,
            REQUEST_OPEN,
            Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val stopIntent = PendingIntent.getService(
            this,
            REQUEST_STOP,
            stopIntent(this),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_mark)
            .setContentTitle("KoSch Assistant · Screen Share aktiv")
            .setContentText("Bildschirmfreigabe läuft sichtbar und nur für diese Session.")
            .setContentIntent(openIntent)
            .setOngoing(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .addAction(Notification.Action.Builder(0, "Stoppen", stopIntent).build())
            .build()
    }

    companion object {
        const val ACTION_START = "cloud.kosch.aiandroid.assistant.action.START_SCREEN_SHARE"
        const val ACTION_STOP = "cloud.kosch.aiandroid.assistant.action.STOP_SCREEN_SHARE"
        private const val EXTRA_RESULT_CODE = "projection_result_code"
        private const val EXTRA_RESULT_DATA = "projection_result_data"
        private const val CHANNEL_ID = "assistant_screen_share_v1"
        private const val NOTIFICATION_ID = 4407
        private const val REQUEST_OPEN = 4408
        private const val REQUEST_STOP = 4409
        private const val DISPLAY_NAME = "KoSch Assistant Screen"
        private const val MAX_IMAGES = 2
        private const val MAX_CAPTURE_EDGE = 960f

        fun startIntent(context: Context, resultCode: Int, resultData: Intent): Intent =
            Intent(context, AssistantScreenShareService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_RESULT_CODE, resultCode)
                putExtra(EXTRA_RESULT_DATA, resultData)
            }

        fun stopIntent(context: Context): Intent =
            Intent(context, AssistantScreenShareService::class.java).apply {
                action = ACTION_STOP
            }

        fun start(context: Context, resultCode: Int, resultData: Intent) {
            ContextCompat.startForegroundService(context, startIntent(context, resultCode, resultData))
        }

        fun stop(context: Context) {
            context.startService(stopIntent(context))
        }
    }
}

private fun Intent.parcelableIntentExtra(name: String): Intent? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableExtra(name, Intent::class.java)
    } else {
        @Suppress("DEPRECATION")
        getParcelableExtra(name)
    }
