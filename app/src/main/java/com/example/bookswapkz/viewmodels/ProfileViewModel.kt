package com.example.bookswapkz.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bookswapkz.models.Exchange
import com.example.bookswapkz.models.User
import com.example.bookswapkz.repositories.FirebaseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: FirebaseRepository
) : ViewModel() {

    private val _userData = MutableLiveData<User?>()
    val userData: LiveData<User?> = _userData

    private val _exchangeHistory = MutableLiveData<List<Exchange>>()
    val exchangeHistory: LiveData<List<Exchange>> = _exchangeHistory

    fun getUserData(userId: String) {
        viewModelScope.launch {
            repository.getUserById(userId).onSuccess { user ->
                _userData.value = user
            }
        }
    }

    fun loadExchangeHistory(userId: String) {
        viewModelScope.launch {
            repository.getUserExchangeHistory(userId).onSuccess { exchanges ->
                _exchangeHistory.value = exchanges
            }
        }
    }
} 