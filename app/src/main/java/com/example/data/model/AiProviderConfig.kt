package com.example.data.model

data class AiProviderConfig(
    val providerType: AiProviderType = AiProviderType.GEMINI,
    val geminiApiKey: String = "",
    val geminiModel: String = "gemini-3.5-flash",
    val ollamaEndpoint: String = "http://10.0.2.2:11434",
    val ollamaModel: String = "llama3",
    val openAiEndpoint: String = "https://api.openai.com/v1",
    val openAiApiKey: String = "",
    val openAiModel: String = "gpt-4o"
)
