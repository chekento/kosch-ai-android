package cloud.kosch.aiandroid.ui

import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import cloud.kosch.aiandroid.model.AdaptiveInputCapabilities
import cloud.kosch.aiandroid.system.AdaptiveInputDeviceMonitor

/** Live Compose bridge for permission-free mouse/trackpad/keyboard hot-plug state. */
@Composable
fun rememberAdaptiveInputCapabilities(): AdaptiveInputCapabilities {
    val context = LocalContext.current.applicationContext
    var capabilities by remember { mutableStateOf(AdaptiveInputCapabilities()) }
    val monitor = remember(context) {
        AdaptiveInputDeviceMonitor(
            context = context,
            callbackHandler = Handler(Looper.getMainLooper()),
            onChanged = { capabilities = it },
        )
    }

    DisposableEffect(monitor) {
        monitor.start()
        onDispose(monitor::stop)
    }
    return capabilities
}
