package com.example.bookswapkz.viewmodels

import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bookswapkz.FirebaseRepository
import com.example.bookswapkz.models.Book
import com.example.bookswapkz.models.User
import kotlinx.coroutines.launch

class BookViewModel : ViewModel() {

    private val repository = FirebaseRepository()

    private val _user = MutableLiveData<User?>()
    val user: LiveData<User?> get() = _user

    private val _allBooks = MutableLiveData<List<Book>>()
    val allBooks: LiveData<List<Book>> get() = _allBooks

    private val _myBooks = MutableLiveData<List<Book>>()
    val myBooks: LiveData<List<Book>> get() = _myBooks

    private val _rentableBooks = MutableLiveData<List<Book>>()
    val rentableBooks: LiveData<List<Book>> get() = _rentableBooks

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> get() = _errorMessage

    private val _exchangeStatus = MutableLiveData<Boolean?>()
    val exchangeStatus: LiveData<Boolean?> get() = _exchangeStatus


    init {
        Log.d("BookViewModel", "ViewModel Initialized")
        getUser()
        fetchAllBooks()
        loadRentableBooks()
    }

    fun getUser() {
        Log.d("BookViewModel", "Fetching user data...")
        viewModelScope.launch {
            repository.getUser().observeForever { userResult ->
                if (userResult != null) {
                    _user.value = userResult
                    Log.d("BookViewModel", "User data updated: ${userResult.nickname}")
                    fetchUserBooks(userResult.userId)
                } else {
                    _user.value = null
                    _myBooks.value = emptyList()
                    Log.d("BookViewModel", "User is null or not logged in.")
                }
                _errorMessage.value = null
            }
        }
    }

    private fun fetchAllBooks() {
        Log.d("BookViewModel", "Fetching all books...")
        viewModelScope.launch {
            repository.getAllBooks().observeForever { books ->
                _allBooks.value = books ?: emptyList()
                Log.d("BookViewModel", "All books updated: ${books?.size ?: 0} books")
            }
        }
    }

    private fun fetchUserBooks(userId: String) {
        if (userId.isBlank()) {
            Log.w("BookViewModel", "fetchUserBooks called with blank userId.")
            _myBooks.value = emptyList()
            return
        }
        Log.d("BookViewModel", "Fetching books for user ID: $userId")
        viewModelScope.launch {
            repository.getUserBooks(userId).observeForever { books ->
                _myBooks.value = books ?: emptyList()
                Log.d("BookViewModel", "User books updated: ${books?.size ?: 0} books for user $userId")
            }
        }
    }

    fun addBook(book: Book, imageUri: Uri?): LiveData<Boolean> {
        val result = MutableLiveData<Boolean>()
        viewModelScope.launch {
            Log.d("BookViewModel", "Attempting to add book: ${book.title}")
            repository.addBook(book, imageUri).observeForever { success ->
                if (success) {
                    Log.i("BookViewModel", "Book '${book.title}' added successfully.")
                } else {
                    Log.e("BookViewModel", "Failed to add book: ${book.title}")
                    _errorMessage.value = repository.lastError ?: "Не удалось добавить книгу"
                }
                result.value = success
            }
        }
        return result
    }

    fun registerUser(
        nickname: String, name: String,
        city: String, street: String, houseNumber: String,
        age: Int, phone: String, email: String, password: String
    ): LiveData<Boolean> {
        val result = MutableLiveData<Boolean>()
        viewModelScope.launch {
            Log.d("BookViewModel", "Attempting to register user: $nickname ($email)")
            repository.registerUser(nickname, name, city, street, houseNumber, age, phone, email, password)
                .observeForever { success ->
                    if (success) {
                        Log.i("BookViewModel", "User registration successful for $nickname.")
                        getUser()
                    } else {
                        Log.e("BookViewModel", "User registration failed for $nickname.")
                        _errorMessage.value = repository.lastError ?: "Ошибка регистрации"
                    }
                    result.value = success
                }
        }
        return result
    }

    fun loginUser(email: String, password: String): LiveData<Boolean> {
        val result = MutableLiveData<Boolean>()
        viewModelScope.launch {
            Log.d("BookViewModel", "Attempting to login user: $email")
            repository.loginUser(email, password).observeForever { success ->
                if (success) {
                    Log.i("BookViewModel", "Login successful for $email.")
                    getUser()
                } else {
                    Log.w("BookViewModel", "Login failed for $email.")
                    _errorMessage.value = repository.lastError ?: "Неверный email или пароль"
                }
                result.value = success
            }
        }
        return result
    }

    fun performExchange(bookToExchange: Book): LiveData<Boolean?> {
        val currentUser = _user.value
        _exchangeStatus.value = null


        if (currentUser == null) {
            Log.e("BookViewModel", "Cannot perform exchange, user is not logged in.")
            _errorMessage.value = "Необходимо войти для обмена"
            return MutableLiveData<Boolean?>(false)
        }
        if (bookToExchange.userId == currentUser.userId) {
            Log.w("BookViewModel", "User cannot exchange their own book.")
            _errorMessage.value = "Нельзя обменять свою же книгу"
            return MutableLiveData<Boolean?>(false)
        }

        Log.d("BookViewModel", "Performing exchange for book ${bookToExchange.id} to user ${currentUser.userId}")

        repository.recordExchange(bookToExchange.id, currentUser.userId, currentUser.phone)
            .observeForever { success ->
                _exchangeStatus.value = success
                if (!success) {
                    _errorMessage.value = repository.lastError ?: "Ошибка обмена"
                } else {
                    Log.i("BookViewModel", "Exchange successful for book ${bookToExchange.id}")
                }
            }
        return _exchangeStatus
    }

    fun saveUser(name: String, nickname: String, city: String, age: Int, phone: String, street: String, houseNumber: String) {
        Log.w("BookViewModel", "saveUser function is not fully implemented.")
        // TODO: Реализовать обновление данных пользователя в репозитории
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    fun clearExchangeStatus() {
        _exchangeStatus.value = null
    }

    fun loadRentableBooks(paidOnly: Boolean? = null) {
        _isLoading.value = true
        viewModelScope.launch {
            repository.getRentableBooks(paidOnly).observeForever { books ->
                _rentableBooks.value = books ?: emptyList()
                _isLoading.value = false
                Log.d("BookViewModel", "Rentable books updated: ${books?.size ?: 0} books")
            }
        }
    }

    fun rentBook(bookId: String, rentPeriod: String): LiveData<Boolean> {
        val result = MutableLiveData<Boolean>()
        viewModelScope.launch {
            _isLoading.value = true
            repository.rentBook(bookId, rentPeriod).observeForever { success ->
                if (success) {
                    Log.i("BookViewModel", "Book rented successfully")
                    loadRentableBooks() // Refresh the list
                } else {
                    Log.e("BookViewModel", "Failed to rent book")
                    _errorMessage.value = repository.lastError ?: "Не удалось арендовать книгу"
                }
                _isLoading.value = false
                result.value = success
            }
        }
        return result
    }

    fun returnBook(bookId: String): LiveData<Boolean> {
        val result = MutableLiveData<Boolean>()
        viewModelScope.launch {
            _isLoading.value = true
            repository.returnBook(bookId).observeForever { success ->
                if (success) {
                    Log.i("BookViewModel", "Book returned successfully")
                    loadRentableBooks() // Refresh the list
                } else {
                    Log.e("BookViewModel", "Failed to return book")
                    _errorMessage.value = repository.lastError ?: "Не удалось вернуть книгу"
                }
                _isLoading.value = false
                result.value = success
            }
        }
        return result
    }

    override fun onCleared() {
        super.onCleared()
        Log.d("BookViewModel", "ViewModel Cleared")

    }
}