package com.example.data.ai

import com.example.data.model.AiProviderConfig
import com.example.data.model.AiProviderType

class AiProviderManager(
    private var config: AiProviderConfig = AiProviderConfig()
) {
    private var activeService: AiService = createService(config)

    fun updateConfig(newConfig: AiProviderConfig) {
        this.config = newConfig
        this.activeService = createService(newConfig)
    }

    fun getConfig(): AiProviderConfig = config

    fun getService(): AiService = activeService

    val isCurrentProviderLocal: Boolean
        get() = config.providerType.isLocal

    val providerBadgeText: String
        get() = when (config.providerType) {
            AiProviderType.GEMINI -> "Cloud: ${config.geminiModel}"
            AiProviderType.OLLAMA -> "Local: Ollama (${config.ollamaModel})"
            AiProviderType.OPENAI_COMPATIBLE -> "Custom: ${config.openAiModel}"
            AiProviderType.OFFLINE_ENGINE -> "Local: On-Device Engine"
        }

    private fun createService(cfg: AiProviderConfig): AiService {
        return when (cfg.providerType) {
            AiProviderType.GEMINI -> GeminiAiService(
                customApiKey = cfg.geminiApiKey.takeIf { it.isNotBlank() },
                modelName = cfg.geminiModel
            )
            AiProviderType.OLLAMA -> OllamaAiService(
                endpointUrl = cfg.ollamaEndpoint,
                model = cfg.ollamaModel
            )
            AiProviderType.OPENAI_COMPATIBLE -> GeminiAiService(
                customApiKey = cfg.openAiApiKey,
                modelName = cfg.openAiModel
            )
            AiProviderType.OFFLINE_ENGINE -> BuiltinOfficeAiEngine
        }
    }
}
