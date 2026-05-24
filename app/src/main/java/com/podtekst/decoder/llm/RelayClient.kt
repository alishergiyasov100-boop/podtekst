package com.podtekst.decoder.llm

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Минимальный OpenAI-совместимый клиент для PocketQwal Relay
 * (или любого endpoint /v1/chat/completions).
 */
class RelayClient(
    private val baseUrl: String,
    private val apiKey: String,
    private val model: String,
) {
    private val http = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    suspend fun chat(
        system: String,
        user: String,
        temperature: Double = 0.4,
        maxTokens: Int = 800,
    ): String = withContext(Dispatchers.IO) {
        val payload = ChatRequest(
            model = model,
            messages = listOf(
                Message("system", system),
                Message("user", user),
            ),
            temperature = temperature,
            maxTokens = maxTokens,
            stream = false,
        )
        val body = json.encodeToString(ChatRequest.serializer(), payload)
            .toRequestBody("application/json; charset=utf-8".toMediaType())

        val url = baseUrl.trimEnd('/') + "/v1/chat/completions"
        val req = Request.Builder()
            .url(url)
            .apply { if (apiKey.isNotBlank()) addHeader("Authorization", "Bearer $apiKey") }
            .post(body)
            .build()

        http.newCall(req).execute().use { resp ->
            val raw = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                throw RelayException("HTTP ${resp.code}: ${raw.take(400)}")
            }
            val parsed = json.decodeFromString(ChatResponse.serializer(), raw)
            parsed.choices.firstOrNull()?.message?.content
                ?: throw RelayException("empty choices")
        }
    }

    @Serializable
    private data class ChatRequest(
        val model: String,
        val messages: List<Message>,
        val temperature: Double,
        @SerialName("max_tokens") val maxTokens: Int,
        val stream: Boolean,
    )

    @Serializable
    private data class Message(
        val role: String,
        val content: String,
    )

    @Serializable
    private data class ChatResponse(
        val choices: List<Choice> = emptyList(),
    )

    @Serializable
    private data class Choice(
        val message: Message,
    )
}

class RelayException(msg: String) : RuntimeException(msg)
