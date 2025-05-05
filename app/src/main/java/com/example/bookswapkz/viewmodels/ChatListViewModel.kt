package com.example.bookswapkz.viewmodels

import android.util.Log // Добавляем Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bookswapkz.data.FirebaseRepository
import com.example.bookswapkz.models.Chat
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect // импорт collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatListViewModel @Inject constructor(
    private val repository: FirebaseRepository
) : ViewModel() {

    private val auth = FirebaseAuth.getInstance()

    private val _chats = MutableStateFlow<List<Chat>>(emptyList())
    val chats: StateFlow<List<Chat>> = _chats

    private val _isLoading = MutableStateFlow<Boolean>(false)
    val isLoading: StateFlow<Boolean> get() = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    init {
        loadUserChats()
    }

    fun loadUserChats() {
        viewModelScope.launch {
            repository.getCurrentUser().onSuccess { user ->
                repository.getUserChatsFlow(user.userId).collect { result ->
                    result.onSuccess { chats ->
                        _chats.value = chats
                    }
                    result.onFailure { error ->
                        _errorMessage.value = error.message
                    }
                }
            }.onFailure { error ->
                _errorMessage.value = error.message
            }
        }
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }
}