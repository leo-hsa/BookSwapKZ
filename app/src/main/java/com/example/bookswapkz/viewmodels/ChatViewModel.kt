package com.example.bookswapkz.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bookswapkz.data.FirebaseRepository
import com.example.bookswapkz.models.Chat
import com.example.bookswapkz.models.Message
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repository: FirebaseRepository,
    private val auth: FirebaseAuth,
    application: Application
) : AndroidViewModel(application) {

    private val _userChats = MutableStateFlow<List<Chat>>(emptyList())
    val userChats: StateFlow<List<Chat>> = _userChats.asStateFlow()

    private val _currentChatMessages = MutableStateFlow<List<Message>>(emptyList())
    val currentChatMessages: StateFlow<List<Message>> = _currentChatMessages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _chatIdResult = MutableStateFlow<Result<String>?>(null)
    val chatIdResult: StateFlow<Result<String>?> = _chatIdResult.asStateFlow()

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadUserChats()
    }

    fun loadUserChats() {
        viewModelScope.launch {
            _isLoading.update { true }
            try {
                repository.currentUserId?.let { userId ->
                    repository.getUserChatsFlow(userId).collect { chats ->
                        _userChats.update { chats }
                    }
                } ?: run {
                    _errorMessage.update { "User not authenticated" }
                }
            } catch (e: Exception) {
                _errorMessage.update { "Failed to load chats: ${e.message}" }
            } finally {
                _isLoading.update { false }
            }
        }
    }

    fun loadMessages(chatId: String) {
        viewModelScope.launch {
            _isLoading.update { true }
            try {
                repository.getChatMessagesFlow(chatId).collect { messages ->
                    _currentChatMessages.update { messages }
                }
            } catch (e: Exception) {
                _errorMessage.update { "Failed to load messages: ${e.message}" }
            } finally {
                _isLoading.update { false }
            }
        }
    }

    fun sendMessage(chatId: String, text: String) {
        val currentUserId = auth.currentUser?.uid ?: return
        
        viewModelScope.launch {
            _isLoading.update { true }
            try {
                repository.sendMessage(chatId, currentUserId, text)
            } catch (e: Exception) {
                _errorMessage.update { "Failed to send message: ${e.message}" }
            } finally {
                _isLoading.update { false }
            }
        }
    }

    fun getOrCreateChatForNavigation(otherUserId: String) {
        val currentUserId = auth.currentUser?.uid ?: return
        
        viewModelScope.launch {
            _isLoading.update { true }
            try {
                val chat = repository.getOrCreateChat(currentUserId, otherUserId)
                _chatIdResult.update { Result.success(chat.chatId) }
            } catch (e: Exception) {
                _errorMessage.update { "Failed to create chat: ${e.message}" }
                _chatIdResult.update { Result.failure(e) }
            } finally {
                _isLoading.update { false }
            }
        }
    }

    fun getOrCreateChat(userId2: String) {
        val currentUserId = auth.currentUser?.uid ?: return
        
        viewModelScope.launch {
            try {
                val chat = repository.getOrCreateChat(currentUserId, userId2)
                _uiState.update { it.copy(currentChat = chat) }
            } catch (e: Exception) {
                _errorMessage.update { "Failed to get or create chat: ${e.message}" }
            }
        }
    }

    fun clearErrorMessage() {
        _errorMessage.update { null }
    }

    fun clearChatIdResult() {
        _chatIdResult.update { null }
    }

    fun clearError() {
        _error.value = null
    }
}

data class ChatUiState(
    val currentChat: Chat? = null,
    val isLoading: Boolean = false
) 