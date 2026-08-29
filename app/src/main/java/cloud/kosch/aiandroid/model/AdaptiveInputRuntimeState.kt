package cloud.kosch.aiandroid.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Process-local presentation signal for permission-free desktop-class input hot-plug.
 *
 * Only aggregate counts are retained. No input-device id, name, descriptor, vendor/product id or event payload is
 * stored here. Reading [capabilities] from Compose registers the normal snapshot dependency, so callers that use the
 * AdaptiveLauncherEnvironment defaults re-plan automatically when a mouse, trackpad or hardware keyboard changes.
 */
object AdaptiveInputRuntimeState {
    var capabilities by mutableStateOf(AdaptiveInputCapabilities())
        private set

    internal fun publish(next: AdaptiveInputCapabilities) {
        capabilities = next
    }
}
