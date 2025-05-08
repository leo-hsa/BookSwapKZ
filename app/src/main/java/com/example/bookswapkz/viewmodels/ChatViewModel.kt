package com.example.bookswapkz.viewmodels

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bookswapkz.data.FirebaseRepository
import com.example.bookswapkz.models.Message
import com.example.bookswapkz.models.User // Для информации о собеседнике
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject
import kotlin.Result

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repository: FirebaseRepository,
    private val savedStateHandle: SavedStateHandle // Hilt внедрит аргументы навигации сюда
) : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val currentUserId: String? = auth.currentUser?.uid

    // Аргументы из Navigation Component
    val chatId: String = savedStateHandle.get<String>("chatId") ?: ""
    val otherUserIdFromNav: String = savedStateHandle.get<String>("otherUserId") ?: ""
    val otherUserNameFromNav: String = savedStateHandle.get<String>("otherUserName") ?: "Чат"

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _sendMessageResult = MutableStateFlow<Result<Unit>?>(null)
    val sendMessageResult: StateFlow<Result<Unit>?> = _sendMessageResult.asStateFlow()

    // Информация о собеседнике (для отображения в Toolbar)
    private val _otherUserInfo = MutableStateFlow<User?>(null)
    val otherUserInfo: StateFlow<User?> = _otherUserInfo.asStateFlow()


    init {
        if (chatId.isBlank()) {
            _errorMessage.value = "ID чата не предоставлен."
            Log.e("ChatViewModel", "Chat ID is blank on init.")
        } else {
            loadMessages()
            fetchOtherUserInfo()
        }
    }

    private fun fetchOtherUserInfo() {
        if (otherUserIdFromNav.isNotBlank()) {
            viewModelScope.launch {
                repository.getUserById(otherUserIdFromNav).onSuccess { user ->
                    _otherUserInfo.value = user
                }.onFailure {
                    Log.e("ChatViewModel", "Failed to fetch other user info for $otherUserIdFromNav", it)
                    // Можно установить имя из аргументов навигации как fallback
                }
            }
        }
    }


    private fun loadMessages() {
        if (chatId.isBlank()) {
            _errorMessage.value = "Неверный ID чата для загрузки сообщений"
            Log.e("ChatViewModel", "loadMessages called with blank chatId.")
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            repository.getChatMessagesFlow(chatId)
                .catch { e ->
                    Log.e("ChatViewModel", "Error collecting messages flow for chatId: $chatId", e)
                    _errorMessage.value = "Ошибка загрузки сообщений: ${e.localizedMessage}"
                    _isLoading.value = false
                }
                .collect { result ->
                    _isLoading.value = false // Скрываем после первого сбора или ошибки
                    result.onSuccess { messageList ->
                        Log.d("ChatViewModel", "Received ${messageList.size} messages for chatId: $chatId")
                        _messages.value = messageList
                    }
                    result.onFailure { e ->
                        Log.e("ChatViewModel", "Failure in messages flow for chatId: $chatId", e)
                        _errorMessage.value = "Ошибка обработки сообщений: ${e.localizedMessage}"
                    }
                }
        }
    }

    fun sendMessage(text: String) {
        val trimmedText = text.trim()
        if (trimmedText.isBlank()) return

        if (currentUserId.isNullOrBlank() || chatId.isBlank()) {
            _errorMessage.value = "Ошибка отправки: ${if(currentUserId.isNullOrBlank()) "Пользователь не авторизован" else "Неверный ID чата"}"
            _sendMessageResult.value = Result.failure(IllegalStateException("Invalid sender or chat ID"))
            Log.e("ChatViewModel", "SendMessage failed: currentUserId=$currentUserId, chatId=$chatId")
            return
        }

        // Создаем объект Message. timestamp будет установлен Firestore через @ServerTimestamp
        val message = Message(
            chatId = chatId,      // id самого чата
            senderId = currentUserId,
            text = trimmedText
            // timestamp будет установлен Firestore
        )

        _sendMessageResult.value = null // Сброс предыдущего результата
        _errorMessage.value = null    // Сброс предыдущей ошибки

        viewModelScope.launch {
            // Можно добавить _isSending LiveData/StateFlow, если нужна индикация отправки
            Log.d("ChatViewModel", "Sending message: $message to chatId: $chatId")
            val sendResult = repository.sendMessage(chatId, message)
            _sendMessageResult.value = sendResult
            if (sendResult.isFailure) {
                val error = sendResult.exceptionOrNull()
                Log.e("ChatViewModel", "Failed to send message to chatId: $chatId", error)
                _errorMessage.value = error?.localizedMessage ?: "Не удалось отправить сообщение"
            } else {
                Log.d("ChatViewModel", "Message sent successfully to chatId: $chatId")
            }
        }
    }

    fun clearSendMessageResult() {
        _sendMessageResult.value = null
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }
}