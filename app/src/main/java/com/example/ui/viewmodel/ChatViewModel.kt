package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entity.ConversationEntity
import com.example.data.local.entity.MessageEntity
import com.example.data.repository.ChatRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModel(
    application: Application,
    private val repository: ChatRepository
) : AndroidViewModel(application) {

    val conversations: StateFlow<List<ConversationEntity>> = repository.allConversations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _activeConversationId = MutableStateFlow<String?>(null)
    val activeConversationId: StateFlow<String?> = _activeConversationId.asStateFlow()

    val activeMessages: StateFlow<List<MessageEntity>> = _activeConversationId
        .flatMapLatest { convId ->
            if (convId != null) {
                repository.getMessagesForConversation(convId)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _showHistory = MutableStateFlow(false)
    val showHistory: StateFlow<Boolean> = _showHistory.asStateFlow()

    private val _showSettings = MutableStateFlow(false)
    val showSettings: StateFlow<Boolean> = _showSettings.asStateFlow()

    private val _snackbarEvent = MutableStateFlow<String?>(null)
    val snackbarEvent: StateFlow<String?> = _snackbarEvent.asStateFlow()

    private val _apiKeyConfigured = MutableStateFlow(repository.isApiKeyConfigured())
    val apiKeyConfigured: StateFlow<Boolean> = _apiKeyConfigured.asStateFlow()

    fun updateInputText(newText: String) {
        _inputText.value = newText
    }

    fun setShowHistory(show: Boolean) {
        _showHistory.value = show
    }

    fun setShowSettings(show: Boolean) {
        _showSettings.value = show
    }

    fun clearSnackbar() {
        _snackbarEvent.value = null
    }

    fun triggerSnackbar(message: String) {
        _snackbarEvent.value = message
    }

    fun startNewChat() {
        _activeConversationId.value = null
        _inputText.value = ""
        _showHistory.value = false
    }

    fun selectConversation(convId: String) {
        _activeConversationId.value = convId
        _showHistory.value = false
    }

    fun deleteConversation(convId: String) {
        viewModelScope.launch {
            repository.deleteConversation(convId)
            if (_activeConversationId.value == convId) {
                _activeConversationId.value = null
            }
            _snackbarEvent.value = "Sohbet silindi"
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearAllHistory()
            _activeConversationId.value = null
            _snackbarEvent.value = "Tüm sohbet geçmişi temizlendi"
        }
    }

    fun saveCustomApiKey(key: String) {
        repository.setCustomApiKey(key)
        _apiKeyConfigured.value = repository.isApiKeyConfigured()
        _snackbarEvent.value = if (repository.isApiKeyConfigured()) {
            "API Anahtarı başarıyla güncellendi!"
        } else {
            "API Anahtarı kaldırıldı"
        }
        _showSettings.value = false
    }

    fun getStoredCustomApiKey(): String = repository.getStoredCustomApiKey()

    fun sendMessage(explicitText: String? = null) {
        val messageToSend = (explicitText ?: _inputText.value).trim()
        if (messageToSend.isBlank() || _isLoading.value) return

        if (!repository.isApiKeyConfigured()) {
            _showSettings.value = true
            _snackbarEvent.value = "Devam etmek için lütfen Gemini API Anahtarınızı girin."
            return
        }

        if (explicitText == null) {
            _inputText.value = ""
        }

        _isLoading.value = true

        viewModelScope.launch {
            val result = repository.sendMessage(_activeConversationId.value, messageToSend)
            result.onSuccess { message ->
                if (_activeConversationId.value == null) {
                    _activeConversationId.value = message.conversationId
                }
            }.onFailure { error ->
                _snackbarEvent.value = error.localizedMessage ?: "Bir hata oluştu"
            }
            _isLoading.value = false
        }
    }

    fun regenerateLastResponse() {
        val currentConvId = _activeConversationId.value ?: return
        if (_isLoading.value) return

        if (!repository.isApiKeyConfigured()) {
            _showSettings.value = true
            _snackbarEvent.value = "Gemini API Anahtarı gerekli."
            return
        }

        _isLoading.value = true
        viewModelScope.launch {
            val result = repository.regenerateLastResponse(currentConvId)
            result.onFailure { error ->
                _snackbarEvent.value = error.localizedMessage ?: "Yeniden üretme başarısız oldu"
            }
            _isLoading.value = false
        }
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val database = AppDatabase.getDatabase(application)
            val repository = ChatRepository(database.chatDao(), application)
            return ChatViewModel(application, repository) as T
        }
    }
}
