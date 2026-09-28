package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig
import com.example.data.local.dao.ChatDao
import com.example.data.local.entity.ConversationEntity
import com.example.data.local.entity.MessageEntity
import com.example.data.remote.RetrofitClient
import com.example.data.remote.model.GeminiContent
import com.example.data.remote.model.GeminiGenerationConfig
import com.example.data.remote.model.GeminiPart
import com.example.data.remote.model.GeminiRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.UUID

class ChatRepository(
    private val chatDao: ChatDao,
    context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("turkmen_ai_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val PREF_KEY_CUSTOM_API_KEY = "custom_gemini_api_key"
        private const val SYSTEM_PROMPT =
            "Sen Türkmen AI adında, TEKNOVA tarafından geliştirilmiş son derece gelişmiş, kibar, zeki ve yardımsever bir yapay zekâ asistanısın. " +
            "Kullanıcılara Türkçe, Türkmence ve diğer dillerde en üst düzeyde kaliteli, anlaşılır ve doğru bilgiler sağlarsın. " +
            "Cevaplarında gerekirse maddeler, temiz listeler ve biçimlendirmeler kullan. " +
            "Her zaman profesyonel, yardımsever ve çözüm odaklı ol."
    }

    val allConversations: Flow<List<ConversationEntity>> = chatDao.getAllConversations()

    fun getMessagesForConversation(convId: String): Flow<List<MessageEntity>> =
        chatDao.getMessagesForConversation(convId)

    fun getActiveApiKey(): String {
        val customKey = prefs.getString(PREF_KEY_CUSTOM_API_KEY, "")?.trim() ?: ""
        if (customKey.isNotEmpty()) {
            return customKey
        }
        val buildKey = BuildConfig.GEMINI_API_KEY.trim()
        if (buildKey.isNotEmpty() && buildKey != "MY_GEMINI_API_KEY") {
            return buildKey
        }
        return ""
    }

    fun isApiKeyConfigured(): Boolean {
        return getActiveApiKey().isNotEmpty()
    }

    fun setCustomApiKey(key: String) {
        prefs.edit().putString(PREF_KEY_CUSTOM_API_KEY, key.trim()).apply()
    }

    fun getStoredCustomApiKey(): String {
        return prefs.getString(PREF_KEY_CUSTOM_API_KEY, "") ?: ""
    }

    suspend fun createNewConversation(initialTitle: String = "Yeni Sohbet"): String = withContext(Dispatchers.IO) {
        val convId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val newConv = ConversationEntity(
            id = convId,
            title = initialTitle,
            createdAt = now,
            updatedAt = now,
            previewText = ""
        )
        chatDao.insertConversation(newConv)
        convId
    }

    suspend fun sendMessage(
        conversationId: String?,
        userText: String
    ): Result<MessageEntity> = withContext(Dispatchers.IO) {
        val apiKey = getActiveApiKey()
        if (apiKey.isEmpty()) {
            return@withContext Result.failure(
                IllegalStateException("Gemini API anahtarı bulunamadı. Lütfen ayarlar simgesine dokunarak API anahtarınızı girin veya AI Studio Secrets üzerinden yapılandırın.")
            )
        }

        // 1. Ensure conversation exists
        val currentConvId = conversationId ?: createNewConversation(
            initialTitle = if (userText.length > 32) userText.take(32) + "..." else userText
        )

        val now = System.currentTimeMillis()
        val userMessageId = UUID.randomUUID().toString()
        val userMessage = MessageEntity(
            id = userMessageId,
            conversationId = currentConvId,
            role = "user",
            content = userText.trim(),
            timestamp = now,
            isError = false
        )
        chatDao.insertMessage(userMessage)

        // Update conversation title if still default
        val conv = chatDao.getConversationById(currentConvId)
        if (conv != null && (conv.title == "Yeni Sohbet" || conv.title.isBlank())) {
            val autoTitle = if (userText.length > 32) userText.take(32) + "..." else userText
            chatDao.updateConversation(conv.copy(title = autoTitle, updatedAt = now, previewText = userText))
        } else if (conv != null) {
            chatDao.updateConversation(conv.copy(updatedAt = now, previewText = userText))
        }

        // 2. Fetch recent conversation history to provide contextual chat
        val pastMessages = chatDao.getMessagesListForConversation(currentConvId)
        // Keep up to 14 latest turns to avoid huge payloads
        val contextSlice = pastMessages.takeLast(14)

        val contents = contextSlice.map { msg ->
            GeminiContent(
                role = if (msg.role == "user") "user" else "model",
                parts = listOf(GeminiPart(text = msg.content))
            )
        }

        val request = GeminiRequest(
            contents = contents,
            systemInstruction = GeminiContent(
                role = "system",
                parts = listOf(GeminiPart(text = SYSTEM_PROMPT))
            ),
            generationConfig = GeminiGenerationConfig(
                temperature = 0.7f,
                topP = 0.95f,
                topK = 40
            )
        )

        try {
            val response = RetrofitClient.geminiService.generateContent(apiKey, request)
            if (response.isSuccessful) {
                val body = response.body()
                val responseText = body?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (!responseText.isNullOrBlank()) {
                    val aiMsg = MessageEntity(
                        id = UUID.randomUUID().toString(),
                        conversationId = currentConvId,
                        role = "model",
                        content = responseText.trim(),
                        timestamp = System.currentTimeMillis(),
                        isError = false
                    )
                    chatDao.insertMessage(aiMsg)

                    val updatedConv = chatDao.getConversationById(currentConvId)
                    if (updatedConv != null) {
                        chatDao.updateConversation(
                            updatedConv.copy(
                                updatedAt = System.currentTimeMillis(),
                                previewText = responseText.take(50)
                            )
                        )
                    }

                    Result.success(aiMsg)
                } else {
                    val errorMsg = body?.error?.message ?: "Türkmen AI yanıt üretemedi."
                    val errEntity = MessageEntity(
                        id = UUID.randomUUID().toString(),
                        conversationId = currentConvId,
                        role = "model",
                        content = "Hata: $errorMsg",
                        timestamp = System.currentTimeMillis(),
                        isError = true
                    )
                    chatDao.insertMessage(errEntity)
                    Result.failure(Exception(errorMsg))
                }
            } else {
                val errorBodyStr = response.errorBody()?.string()
                val parsedMsg = RetrofitClient.parseErrorBody(errorBodyStr)
                val finalError = parsedMsg ?: "Sunucu hatası: HTTP ${response.code()}"

                val errEntity = MessageEntity(
                    id = UUID.randomUUID().toString(),
                    conversationId = currentConvId,
                    role = "model",
                    content = "Bağlantı Hatası: $finalError",
                    timestamp = System.currentTimeMillis(),
                    isError = true
                )
                chatDao.insertMessage(errEntity)
                Result.failure(Exception(finalError))
            }
        } catch (e: Exception) {
            val errEntity = MessageEntity(
                id = UUID.randomUUID().toString(),
                conversationId = currentConvId,
                role = "model",
                content = "Ağ Hatası: ${e.localizedMessage ?: "Bağlantı kurulamadı. İnternet bağlantınızı kontrol edin."}",
                timestamp = System.currentTimeMillis(),
                isError = true
            )
            chatDao.insertMessage(errEntity)
            Result.failure(e)
        }
    }

    suspend fun regenerateLastResponse(conversationId: String): Result<MessageEntity> = withContext(Dispatchers.IO) {
        val messages = chatDao.getMessagesListForConversation(conversationId)
        if (messages.isEmpty()) {
            return@withContext Result.failure(IllegalStateException("Yeniden denenecek mesaj bulunamadı."))
        }

        // If the last message was from the model, remove it
        val lastMsg = messages.last()
        if (lastMsg.role == "model") {
            chatDao.deleteMessage(lastMsg.id)
        }

        // Find the last user message
        val remaining = chatDao.getMessagesListForConversation(conversationId)
        val lastUserMsg = remaining.lastOrNull { it.role == "user" }
            ?: return@withContext Result.failure(IllegalStateException("Kullanıcı sorusu bulunamadı."))

        val apiKey = getActiveApiKey()
        if (apiKey.isEmpty()) {
            return@withContext Result.failure(
                IllegalStateException("Gemini API anahtarı gerekli.")
            )
        }

        val contents = remaining.takeLast(14).map { msg ->
            GeminiContent(
                role = if (msg.role == "user") "user" else "model",
                parts = listOf(GeminiPart(text = msg.content))
            )
        }

        val request = GeminiRequest(
            contents = contents,
            systemInstruction = GeminiContent(
                role = "system",
                parts = listOf(GeminiPart(text = SYSTEM_PROMPT))
            ),
            generationConfig = GeminiGenerationConfig(
                temperature = 0.8f,
                topP = 0.95f,
                topK = 40
            )
        )

        try {
            val response = RetrofitClient.geminiService.generateContent(apiKey, request)
            if (response.isSuccessful) {
                val body = response.body()
                val responseText = body?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (!responseText.isNullOrBlank()) {
                    val aiMsg = MessageEntity(
                        id = UUID.randomUUID().toString(),
                        conversationId = conversationId,
                        role = "model",
                        content = responseText.trim(),
                        timestamp = System.currentTimeMillis(),
                        isError = false
                    )
                    chatDao.insertMessage(aiMsg)
                    Result.success(aiMsg)
                } else {
                    val errorMsg = body?.error?.message ?: "Türkmen AI yanıt veremedi."
                    Result.failure(Exception(errorMsg))
                }
            } else {
                val errorBodyStr = response.errorBody()?.string()
                val parsedMsg = RetrofitClient.parseErrorBody(errorBodyStr)
                Result.failure(Exception(parsedMsg ?: "HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteConversation(convId: String) = withContext(Dispatchers.IO) {
        chatDao.deleteMessagesForConversation(convId)
        chatDao.deleteConversation(convId)
    }

    suspend fun clearAllHistory() = withContext(Dispatchers.IO) {
        chatDao.clearAllMessages()
        chatDao.clearAllConversations()
    }
}
