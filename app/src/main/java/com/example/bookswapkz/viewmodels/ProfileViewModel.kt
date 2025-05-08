package com.example.bookswapkz.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bookswapkz.models.Exchange
import com.example.bookswapkz.models.User
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _userData = MutableLiveData<User?>()
    val userData: LiveData<User?> = _userData

    private val _exchangeHistory = MutableLiveData<List<Exchange>>()
    val exchangeHistory: LiveData<List<Exchange>> = _exchangeHistory

    private val _nicknameExists = MutableLiveData<Boolean>()
    val nicknameExists: LiveData<Boolean> = _nicknameExists

    private val _saveSuccess = MutableLiveData<Boolean>()
    val saveSuccess: LiveData<Boolean> = _saveSuccess

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    fun getUserData(userId: String) {
        viewModelScope.launch {
            try {
                val document = firestore.collection("users").document(userId).get().await()
                if (document.exists()) {
                    val user = document.toObject(User::class.java)
                    _userData.value = user
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }

    fun loadExchangeHistory(userId: String) {
        viewModelScope.launch {
            try {
                val exchanges = firestore.collection("exchanges")
                    .whereEqualTo("userId", userId)
                    .get()
                    .await()
                    .toObjects(Exchange::class.java)
                _exchangeHistory.value = exchanges
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }

    fun updateUserProfile(
        userId: String,
        nickname: String,
        phone: String,
        city: String,
        street: String,
        houseNumber: String
    ) {
        viewModelScope.launch {
            try {
                // Check if nickname is already taken by another user
                val nicknameQuery = firestore.collection("users")
                    .whereEqualTo("nickname", nickname)
                    .whereNotEqualTo("userId", userId)
                    .get()
                    .await()

                if (!nicknameQuery.isEmpty) {
                    _nicknameExists.value = true
                    return@launch
                }

                // Update user profile
                val userData = mapOf(
                    "nickname" to nickname,
                    "phone" to phone,
                    "city" to city,
                    "street" to street,
                    "houseNumber" to houseNumber
                )

                firestore.collection("users").document(userId)
                    .update(userData)
                    .await()

                _saveSuccess.value = true
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }
} 