package com.example.bookswapkz.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bookswapkz.data.FirebaseRepository
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlin.Result
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: FirebaseRepository
) : ViewModel() {

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    private val _registrationResult = MutableLiveData<Result<FirebaseUser>>()
    val registrationResult: LiveData<Result<FirebaseUser>> = _registrationResult

    fun loginUser(email: String, password: String): LiveData<Result<FirebaseUser>> {
        val result = MutableLiveData<Result<FirebaseUser>>()
        viewModelScope.launch {
            result.value = repository.loginUser(email, password)
        }
        return result
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
    ): LiveData<Result<FirebaseUser>> {
        val result = MutableLiveData<Result<FirebaseUser>>()
        viewModelScope.launch {
            result.value = repository.registerUser(
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
        }
        return result
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    fun clearRegistrationResult() {
        _registrationResult.value = null
    }
} 