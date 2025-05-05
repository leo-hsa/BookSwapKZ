package com.example.bookswapkz.viewmodels

import android.util.Log // Импорт Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bookswapkz.data.FirebaseRepository
import com.example.bookswapkz.models.Message
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject
import kotlin.Result

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repository: FirebaseRepository, // <-- Правильный репозиторий
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val auth = FirebaseAuth.getInstance()

    // Получаем аргументы из SavedStateHandle
    val chatId: String = savedStateHandle.get<String>("chatId") ?: error("Chat ID is required")
    val otherUserId: String = savedStateHandle.get<String>("otherUserId") ?: "" // Может быть пустым, если не передан
    val otherUserName: String = savedStateHandle.get<String>("otherUserName") ?: "Чат" // Используем как заголовок

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages

    private val _isLoading = MutableStateFlow<Boolean>(false)
    val isLoading: StateFlow<Boolean> get() = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _sendMessageResult = MutableStateFlow<Result<Unit>?>(null)
    val sendMessageResult: StateFlow<Result<Unit>?> get() = _sendMessageResult

    init {
        loadMessages()
    }

    private fun loadMessages() {
        if (chatId.isBlank()) {
            _errorMessage.value = "Неверный ID чата для загрузки сообщений"
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            // --- ИСПРАВЛЕНО: Вызываем правильный метод репозитория и обрабатываем Result<List<Message>> ---
            repository.getChatMessagesFlow(chatId)
                .catch { e ->
                    Log.e("ChatViewModel", "Error collecting messages flow", e)
                    _errorMessage.value = "Ошибка загрузки сообщений: ${e.localizedMessage}"
                    _isLoading.value = false
                }
                .collect { result -> // Получаем Result из Flow
                    result.onSuccess { messageList -> _messages.value = messageList }
                        .onFailure { e -> _errorMessage.value = "Ошибка обработки сообщений: ${e.localizedMessage}" }
                    _isLoading.value = false // Скрываем после первого сбора или ошибки
                }
        }
    }
    
    // Public method to load messages with a specific chat ID
    fun loadMessages(chatId: String) {
        if (chatId.isBlank()) {
            _errorMessage.value = "Неверный ID чата для загрузки сообщений"
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            repository.getChatMessagesFlow(chatId)
                .catch { e ->
                    Log.e("ChatViewModel", "Error collecting messages flow", e)
                    _errorMessage.value = "Ошибка загрузки сообщений: ${e.localizedMessage}"
                    _isLoading.value = false
                }
                .collect { result ->
                    result.onSuccess { messageList -> _messages.value = messageList }
                        .onFailure { e -> _errorMessage.value = "Ошибка обработки сообщений: ${e.localizedMessage}" }
                    _isLoading.value = false
                }
        }
    }

    fun sendMessage(text: String, senderId: String) {
        val trimmedText = text.trim()
        if (trimmedText.isBlank()) return

        if (senderId.isBlank() || chatId.isBlank()) {
            _errorMessage.value = "Ошибка отправки: ${if(senderId.isBlank()) "Пользователь не авторизован" else "Неверный ID чата"}"
            _sendMessageResult.value = Result.failure(IllegalStateException("Invalid sender or chat ID"))
            return
        }

        val message = Message(
            chatId = chatId,
            senderId = senderId,
            text = trimmedText,
            timestamp = Date()
        )

        _sendMessageResult.value = null // Сброс
        _errorMessage.value = null

        viewModelScope.launch {
            // Можно добавить _isSending LiveData/StateFlow
            val sendResult = repository.sendMessage(chatId, message) // Вызываем suspend репозитория
            _sendMessageResult.value = sendResult // Сохраняем результат
            if (sendResult.isFailure) {
                _errorMessage.value = sendResult.exceptionOrNull()?.localizedMessage ?: "Не удалось отправить сообщение"
            }
            // Можно скрыть _isSending
        }
    }

    fun clearSendMessageResult() {
        _sendMessageResult.value = null
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    // getOrCreateChat здесь не нужен, он вызывается извне
}