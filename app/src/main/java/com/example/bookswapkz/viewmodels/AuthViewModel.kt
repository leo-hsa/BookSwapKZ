package com.example.bookswapkz.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bookswapkz.data.FirebaseRepository
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: FirebaseRepository
) : ViewModel() {

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    private val _registrationResult = MutableLiveData<Result<FirebaseUser>>()
    val registrationResult: LiveData<Result<FirebaseUser>> = _registrationResult

    private val _loginResult = MutableLiveData<Result<FirebaseUser>>()
    val loginResult: LiveData<Result<FirebaseUser>> = _loginResult

    fun loginUser(email: String, password: String) {
        viewModelScope.launch {
            val result = repository.loginUser(email, password)
            _loginResult.value = result
            if (result.isFailure) {
                _errorMessage.value = result.exceptionOrNull()?.message ?: "Login failed"
            }
        }
    }

    fun registerUser(
        email: String,
        password: String,
        name: String,
        nickname: String,
        city: String,
        street: String,
        houseNumber: String,
        age: Int,
        phone: String
    ) {
        viewModelScope.launch {
            val result = repository.registerUser(
                nickname = nickname,
                name = name,
                city = city,
                street = street,
                houseNumber = houseNumber,
                age = age,
                phone = phone,
                email = email,
                password = password
            )
            _registrationResult.value = result
            if (result.isFailure) {
                _errorMessage.value = result.exceptionOrNull()?.message ?: "Registration failed"
            }
        }
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    fun clearRegistrationResult() {
        _registrationResult.value = null
    }

    fun clearLoginResult() {
        _loginResult.value = null
    }
}