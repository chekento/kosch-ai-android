package cloud.kosch.aiandroid.ai

enum class RuntimeStage(val label: String) {
    ACTIVE("AKTIV"),
    ADAPTER_READY("ADAPTER"),
    EVALUATING("PRÜFUNG"),
}

data class LocalRuntimeProfile(
    val id: String,
    val name: String,
    val stage: RuntimeStage,
    val description: String,
    val license: String,
    val projectUrl: String?,
)

object LocalRuntimeRegistry {
    val runtimes = listOf(
        LocalRuntimeProfile(
            id = "kosch-local-core",
            name = "KoSch Local Core",
            stage = RuntimeStage.ACTIVE,
            description = "Befehle, App-Ranking, Szenen, Kontext und Dateiklassifikation ohne Modell",
            license = "Apache-2.0",
            projectUrl = null,
        ),
        LocalRuntimeProfile(
            id = "litert-lm",
            name = "LiteRT-LM",
            stage = RuntimeStage.EVALUATING,
            description = "Beschleunigte On-Device-Inferenz; Modell und Geräteklasse bleiben wählbar",
            license = "Apache-2.0",
            projectUrl = "https://github.com/google-ai-edge/LiteRT-LM",
        ),
        LocalRuntimeProfile(
            id = "llama-cpp",
            name = "llama.cpp",
            stage = RuntimeStage.ADAPTER_READY,
            description = "GGUF-Ökosystem und offizielles Android-Beispiel als native Integrationsroute",
            license = "MIT",
            projectUrl = "https://github.com/ggml-org/llama.cpp",
        ),
        LocalRuntimeProfile(
            id = "mlc-llm",
            name = "MLC LLM",
            stage = RuntimeStage.EVALUATING,
            description = "GPU-orientierte Android-Inferenz für passende Geräteprofile",
            license = "Apache-2.0",
            projectUrl = "https://github.com/mlc-ai/mlc-llm",
        ),
    )
}
