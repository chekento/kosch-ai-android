package cloud.kosch.aiandroid.ui

/**
 * Single shutdown gate for the visible Assistant master toggle.
 *
 * Disabling the Assistant must terminate every live modality before both state owners are marked disabled.
 * The callbacks are deliberately injected so this policy stays testable without Android capture primitives.
 */
internal object AssistantEnableCoordinator {
    fun apply(
        enabled: Boolean,
        stopSpeech: () -> Unit,
        stopScreenSession: () -> Unit,
        stopCameraSession: () -> Unit,
        setSessionEnabled: (Boolean) -> Unit,
        setAgentEnabled: (Boolean) -> Unit,
    ) {
        if (!enabled) {
            stopSpeech()
            stopScreenSession()
            stopCameraSession()
        }
        setSessionEnabled(enabled)
        setAgentEnabled(enabled)
    }
}
