package cloud.kosch.aiandroid.ai

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import cloud.kosch.aiandroid.security.SecureCredentialType
import cloud.kosch.aiandroid.security.SecureCredentialVault
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.URL
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import javax.net.ssl.HttpsURLConnection

sealed interface OpenRouterOAuthResult {
    data class Connected(val userId: String?) : OpenRouterOAuthResult
    data class Failed(val reason: String) : OpenRouterOAuthResult
    data object Cancelled : OpenRouterOAuthResult
}

data class OpenRouterOAuthLaunchPlan(
    val authorizationUrl: String,
    val callbackUrl: String,
)

/**
 * Real OpenRouter OAuth/PKCE connector for KAL.
 *
 * OpenRouter explicitly supports localhost callbacks on arbitrary ports. KAL binds a one-shot listener to
 * 127.0.0.1 only, puts an unguessable value in the callback path, opens the system browser, and exchanges the
 * returned authorization code for a user-controlled API key. No client secret is embedded in the APK.
 *
 * This class performs no background polling and accepts only one active authorization session at a time.
 */
class OpenRouterOAuthConnector(
    context: Context,
    private val vault: SecureCredentialVault = SecureCredentialVault(context),
    private val executor: ExecutorService = Executors.newSingleThreadExecutor(),
    private val callbackHandler: Handler = Handler(Looper.getMainLooper()),
) : AutoCloseable {
    private val appContext = context.applicationContext
    private val closed = AtomicBoolean(false)
    private val cancellationRequested = AtomicBoolean(false)
    @Volatile private var activeServer: ServerSocket? = null

    /**
     * Starts the local callback receiver and returns the browser URL. The caller remains in control of when the
     * authorization page is opened.
     */
    fun prepare(onResult: (OpenRouterOAuthResult) -> Unit): OpenRouterOAuthLaunchPlan {
        check(!closed.get()) { "Connector is closed" }
        check(activeServer == null) { "An OpenRouter authorization is already active" }
        cancellationRequested.set(false)

        val pkce = KalPkce.create()
        val server = ServerSocket().apply {
            reuseAddress = false
            soTimeout = CALLBACK_TIMEOUT_MS
            bind(InetSocketAddress(InetAddress.getByName(LOOPBACK_HOST), 0), 1)
        }
        activeServer = server

        val callbackPath = "/kal/oauth/openrouter/${pkce.state}"
        val callbackUrl = "http://$LOOPBACK_HOST:${server.localPort}$callbackPath"
        val authorizationUrl = OpenRouterOAuthProtocol.authorizationUrl(
            callbackUrl = callbackUrl,
            codeChallenge = pkce.codeChallenge,
        )

        executor.execute {
            val result = runCatching {
                val code = receiveAuthorizationCode(server, callbackPath)
                exchangeCode(code, pkce.codeVerifier)
            }.fold(
                onSuccess = { it },
                onFailure = { throwable ->
                    if (closed.get() || cancellationRequested.get()) OpenRouterOAuthResult.Cancelled
                    else OpenRouterOAuthResult.Failed(safeFailureReason(throwable))
                },
            )
            closeServer(server)
            callbackHandler.post { onResult(result) }
        }

        return OpenRouterOAuthLaunchPlan(
            authorizationUrl = authorizationUrl,
            callbackUrl = callbackUrl,
        )
    }

    fun openAuthorizationPage(plan: OpenRouterOAuthLaunchPlan): Boolean {
        if (closed.get()) return false
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(plan.authorizationUrl)).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val launched = runCatching {
            appContext.startActivity(intent)
            true
        }.getOrDefault(false)
        if (!launched) cancel()
        return launched
    }

    fun cancel() {
        cancellationRequested.set(true)
        activeServer?.let(::closeServer)
    }

    override fun close() {
        if (!closed.compareAndSet(false, true)) return
        cancellationRequested.set(true)
        activeServer?.let(::closeServer)
        executor.shutdownNow()
    }

    private fun receiveAuthorizationCode(server: ServerSocket, expectedPath: String): String {
        server.accept().use { socket ->
            require(socket.inetAddress.isLoopbackAddress) { "OAuth callback was not local" }
            socket.soTimeout = SOCKET_READ_TIMEOUT_MS
            val reader = BufferedReader(
                InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII),
            )
            val requestLine = reader.readLine() ?: error("Empty OAuth callback")
            require(requestLine.length <= MAX_REQUEST_LINE_LENGTH) { "OAuth callback was too large" }

            val code = OpenRouterOAuthProtocol.codeFromRequestLine(requestLine, expectedPath)
            writeBrowserResponse(socket, success = code != null)
            return code ?: error("OpenRouter callback did not contain a valid code")
        }
    }

    private fun exchangeCode(code: String, codeVerifier: String): OpenRouterOAuthResult {
        require(code.length in 1..MAX_AUTH_CODE_LENGTH) { "Invalid OpenRouter authorization code" }
        val connection = (URL(KEY_EXCHANGE_URL).openConnection() as HttpsURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = NETWORK_CONNECT_TIMEOUT_MS
            readTimeout = NETWORK_READ_TIMEOUT_MS
            doOutput = true
            useCaches = false
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
        }

        try {
            val body = JSONObject()
                .put("code", code)
                .put("code_verifier", codeVerifier)
                .put("code_challenge_method", "S256")
                .toString()
            OutputStreamWriter(connection.outputStream, StandardCharsets.UTF_8).use { writer ->
                writer.write(body)
            }

            val status = connection.responseCode
            if (status !in 200..299) {
                return OpenRouterOAuthResult.Failed("OpenRouter-Verbindung fehlgeschlagen (HTTP $status)")
            }

            val response = connection.inputStream.bufferedReader(StandardCharsets.UTF_8).use { reader ->
                val text = reader.readText()
                require(text.length <= MAX_RESPONSE_LENGTH) { "OpenRouter response was too large" }
                JSONObject(text)
            }
            val key = response.optString("key").takeIf(String::isNotBlank)
                ?: return OpenRouterOAuthResult.Failed("OpenRouter hat keinen verwendbaren Schlüssel geliefert")
            val userId = response.optString("user_id").takeIf(String::isNotBlank)

            vault.put(
                providerId = PROVIDER_ID,
                type = SecureCredentialType.OAUTH_GENERATED_KEY,
                secret = key.toCharArray(),
            )
            return OpenRouterOAuthResult.Connected(userId = userId)
        } finally {
            connection.disconnect()
        }
    }

    private fun writeBrowserResponse(socket: Socket, success: Boolean) {
        val title = if (success) "KAL · OpenRouter verbunden" else "KAL · Verbindung fehlgeschlagen"
        val message = if (success) {
            "Die Autorisierung wurde an KAL übergeben. Du kannst dieses Browserfenster schließen."
        } else {
            "KAL konnte diesen Callback nicht akzeptieren. Kehre zu KAL zurück und starte die Verbindung erneut."
        }
        val html = """
            <!doctype html><html><head><meta charset="utf-8"><title>$title</title></head>
            <body><h1>$title</h1><p>$message</p></body></html>
        """.trimIndent()
        val bytes = html.toByteArray(StandardCharsets.UTF_8)
        val header = buildString {
            append("HTTP/1.1 ")
            append(if (success) "200 OK" else "400 Bad Request")
            append("\r\nContent-Type: text/html; charset=utf-8")
            append("\r\nContent-Length: ${bytes.size}")
            append("\r\nCache-Control: no-store")
            append("\r\nConnection: close\r\n\r\n")
        }.toByteArray(StandardCharsets.US_ASCII)
        runCatching {
            socket.getOutputStream().apply {
                write(header)
                write(bytes)
                flush()
            }
        }
    }

    private fun closeServer(server: ServerSocket) {
        runCatching { server.close() }
        if (activeServer === server) activeServer = null
    }

    private fun safeFailureReason(throwable: Throwable): String = when (throwable) {
        is java.net.SocketTimeoutException -> "OpenRouter-Anmeldung ist abgelaufen"
        is java.net.UnknownHostException -> "OpenRouter ist derzeit nicht erreichbar"
        is javax.net.ssl.SSLException -> "Sichere OpenRouter-Verbindung konnte nicht aufgebaut werden"
        else -> "OpenRouter-Verbindung konnte nicht abgeschlossen werden"
    }

    private companion object {
        const val PROVIDER_ID = "openrouter"
        const val LOOPBACK_HOST = "127.0.0.1"
        const val KEY_EXCHANGE_URL = "https://openrouter.ai/api/v1/auth/keys"
        const val CALLBACK_TIMEOUT_MS = 180_000
        const val SOCKET_READ_TIMEOUT_MS = 10_000
        const val NETWORK_CONNECT_TIMEOUT_MS = 15_000
        const val NETWORK_READ_TIMEOUT_MS = 30_000
        const val MAX_REQUEST_LINE_LENGTH = 4096
        const val MAX_AUTH_CODE_LENGTH = 2048
        const val MAX_RESPONSE_LENGTH = 64 * 1024
    }
}

/** Pure protocol helpers kept testable without Android/network I/O. */
object OpenRouterOAuthProtocol {
    fun authorizationUrl(callbackUrl: String, codeChallenge: String): String {
        require(callbackUrl.startsWith("http://127.0.0.1:")) { "OpenRouter mobile callback must be loopback" }
        require(codeChallenge.isNotBlank())
        return buildString {
            append("https://openrouter.ai/auth?callback_url=")
            append(urlEncode(callbackUrl))
            append("&code_challenge=")
            append(urlEncode(codeChallenge))
            append("&code_challenge_method=S256")
        }
    }

    fun codeFromRequestLine(requestLine: String, expectedPath: String): String? {
        val parts = requestLine.split(' ')
        if (parts.size < 3 || parts[0] != "GET") return null
        val target = parts[1]
        val questionMark = target.indexOf('?')
        val path = if (questionMark >= 0) target.substring(0, questionMark) else target
        if (path != expectedPath || questionMark < 0) return null

        return target.substring(questionMark + 1)
            .split('&')
            .asSequence()
            .mapNotNull { parameter ->
                val separator = parameter.indexOf('=')
                if (separator <= 0) null
                else parameter.substring(0, separator) to parameter.substring(separator + 1)
            }
            .firstOrNull { (name, _) -> name == "code" }
            ?.second
            ?.let { URLDecoder.decode(it, StandardCharsets.UTF_8.name()) }
            ?.takeIf { it.isNotBlank() }
    }

    private fun urlEncode(value: String): String = URLEncoder
        .encode(value, StandardCharsets.UTF_8.name())
        .replace("+", "%20")
}
