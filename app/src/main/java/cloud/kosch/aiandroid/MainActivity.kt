package cloud.kosch.aiandroid

import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import cloud.kosch.aiandroid.system.HomeRoleController
import cloud.kosch.aiandroid.system.WidgetHostController
import cloud.kosch.aiandroid.ui.LauncherRoot
import cloud.kosch.aiandroid.ui.theme.KoSchLauncherTheme

class MainActivity : ComponentActivity() {
    private lateinit var controller: LauncherController
    private lateinit var widgetHostController: WidgetHostController

    private val homeRoleRequest = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        controller.refreshSystemState()
    }

    private val voiceRequest = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val spoken = result.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
        if (spoken.isNullOrBlank()) {
            controller.postNotice("Keine Spracheingabe übernommen")
        } else {
            controller.submitCommand(spoken, ::requestVoiceInput)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        controller = LauncherController(applicationContext)
        widgetHostController = WidgetHostController(applicationContext)
        controller.start()

        setContent {
            KoSchLauncherTheme {
                LauncherRoot(
                    controller = controller,
                    requestHomeRole = ::requestHomeRole,
                    requestVoiceInput = ::requestVoiceInput,
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        widgetHostController.startListening()
    }

    override fun onResume() {
        super.onResume()
        controller.refreshSystemState()
    }

    override fun onStop() {
        widgetHostController.stopListening()
        super.onStop()
    }

    override fun onDestroy() {
        controller.close()
        super.onDestroy()
    }

    private fun requestHomeRole() {
        val intent = HomeRoleController.requestIntent(this)
        if (intent == null) {
            controller.refreshSystemState()
        } else {
            homeRoleRequest.launch(intent)
        }
    }

    private fun requestVoiceInput() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Was möchtest du tun?")
        }
        runCatching { voiceRequest.launch(intent) }
            .onFailure { controller.postNotice("Auf diesem Gerät ist keine Spracheingabe verfügbar") }
    }
}
