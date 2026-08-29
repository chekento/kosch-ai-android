package cloud.kosch.aiandroid

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import cloud.kosch.aiandroid.ai.AiContextHandoffPolicy
import cloud.kosch.aiandroid.ai.AiHubOrigin
import cloud.kosch.aiandroid.ai.AiHubRoutingContext
import cloud.kosch.aiandroid.data.AppCatalog
import cloud.kosch.aiandroid.model.LaunchableApp
import cloud.kosch.aiandroid.ui.AiContextHandoffConsentSurface
import cloud.kosch.aiandroid.ui.AiHubSurface
import cloud.kosch.aiandroid.ui.theme.KoSchLauncherTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Official Android user-driven ingress for selected/shared text.
 *
 * It receives only the text Android explicitly gives after PROCESS_TEXT or ACTION_SEND. No Accessibility service,
 * clipboard history, source-app inventory or surrounding document content is read. The selected text is still held
 * behind AiContextHandoffConsentSurface and is not part of the default disclosure selection.
 */
class AiTextIngressActivity : ComponentActivity() {
    private lateinit var handoff: AiContextHandoffController
    private lateinit var hub: AiHubController
    private var apps by mutableStateOf<List<LaunchableApp>>(emptyList())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val selectedText = extractExplicitText(intent)
        if (selectedText.isNullOrBlank()) {
            finish()
            return
        }

        handoff = AiContextHandoffController().also {
            it.prepare(
                AiContextHandoffPolicy.fromSelectedText(
                    text = selectedText,
                    title = intent.getStringExtra(Intent.EXTRA_SUBJECT),
                ),
            )
        }
        hub = AiHubController(applicationContext)

        lifecycleScope.launch {
            val loaded = withContext(Dispatchers.Default) {
                AppCatalog(
                    context = applicationContext,
                    callbackHandler = Handler(Looper.getMainLooper()),
                    onCatalogChanged = {},
                ).loadApps()
            }
            apps = loaded
        }

        setContent {
            KoSchLauncherTheme {
                Box(Modifier.fillMaxSize()) {
                    val draft = handoff.draft
                    when {
                        draft != null -> AiContextHandoffConsentSurface(
                            draft = draft,
                            onCancel = {
                                handoff.cancel()
                                finish()
                            },
                            onConfirm = { question, selection ->
                                val confirmed = handoff.confirm(
                                    userPrompt = question,
                                    userConfirmed = true,
                                    selection = selection,
                                )
                                if (confirmed != null) {
                                    hub.open(
                                        initialPrompt = confirmed.prompt,
                                        context = AiHubRoutingContext(origin = AiHubOrigin.COMMAND),
                                    )
                                }
                            },
                        )

                        hub.visible -> AiHubSurface(
                            hub = hub,
                            apps = apps,
                        )

                        else -> LaunchedEffect(Unit) { finish() }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        if (::handoff.isInitialized) handoff.cancel()
        if (::hub.isInitialized) hub.close()
        super.onDestroy()
    }

    private fun extractExplicitText(sourceIntent: Intent): CharSequence? = when (sourceIntent.action) {
        Intent.ACTION_PROCESS_TEXT -> sourceIntent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)
        Intent.ACTION_SEND -> if (sourceIntent.type == "text/plain") {
            sourceIntent.getCharSequenceExtra(Intent.EXTRA_TEXT)
        } else {
            null
        }
        else -> null
    }?.toString()?.trim()?.take(MAX_INGRESS_CHARS)

    private companion object {
        const val MAX_INGRESS_CHARS = 8_000
    }
}
