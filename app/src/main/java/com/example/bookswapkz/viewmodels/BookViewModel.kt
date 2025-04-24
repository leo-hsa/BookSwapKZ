package com.example.bookswapkz.viewmodels

import android.app.Application // <-- Импорт для Application Context
import android.net.Uri
import android.util.Log
// import android.widget.Toast // <-- Импорт Toast УДАЛЕН
import androidx.lifecycle.AndroidViewModel // <-- Наследуем от AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
// import androidx.lifecycle.ViewModel // Заменяем на AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bookswapkz.data.FirebaseRepository
import com.example.bookswapkz.models.Book
import com.example.bookswapkz.models.Exchange
import com.example.bookswapkz.models.User
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BookViewModel @Inject constructor(
    private val repository: FirebaseRepository,
    application: Application // Внедряем Application для AndroidViewModel
) : AndroidViewModel(application) { // Наследуем AndroidViewModel

    // ... (остальные LiveData без изменений) ...
    private val _user = MutableLiveData<User?>()
    val user: LiveData<User?> get() = _user
    private val _allBooks = MutableLiveData<List<Book>>()
    val allBooks: LiveData<List<Book>> get() = _allBooks
    private val _myBooks = MutableLiveData<List<Book>>()
    val myBooks: LiveData<List<Book>> get() = _myBooks
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> get() = _errorMessage
    private val _exchangeResult = MutableLiveData<Result<Unit>?>()
    val exchangeResult: LiveData<Result<Unit>?> get() = _exchangeResult
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading
    private val _givenExchanges = MutableLiveData<List<Exchange>>()
    val givenExchanges: LiveData<List<Exchange>> get() = _givenExchanges
    private val _receivedExchanges = MutableLiveData<List<Exchange>>()
    val receivedExchanges: LiveData<List<Exchange>> get() = _receivedExchanges

    init {
        Log.d("BookViewModel", "ViewModel Initialized")
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _isLoading.postValue(true)
            val userResult = repository.getCurrentUser()
            userResult.onSuccess { currentUser ->
                _user.postValue(currentUser)
                Log.d("BookViewModel", "Initial user loaded: ${currentUser.nickname}")
                fetchUserBooksInternal(currentUser.id)
                loadUserExchangeHistory(currentUser.id)
            }.onFailure {
                _user.postValue(null)
                _myBooks.postValue(emptyList())
                _givenExchanges.postValue(emptyList())
                _receivedExchanges.postValue(emptyList())
                Log.w("BookViewModel", "Initial user load failed or user not logged in.")
            }
            fetchAllBooksInternal()
            _isLoading.postValue(false)
        }
    }

    private fun fetchAllBooksInternal() {
        viewModelScope.launch {
            _isLoading.postValue(true)
            repository.getAllBooks().onSuccess { books ->
                _allBooks.postValue(books)
                Log.d("BookViewModel", "All books updated: ${books.size} books")
            }.onFailure { error ->
                Log.e("BookViewModel", "Error fetching all books", error)
                _errorMessage.postValue("Failed to load books: ${error.message}")
            }
            _isLoading.postValue(false)
        }
    }
    
    private fun fetchUserBooksInternal(userId: String) {
        viewModelScope.launch {
            _isLoading.postValue(true)
            repository.getBooksByOwnerId(userId).onSuccess { books ->
                _myBooks.postValue(books)
                Log.d("BookViewModel", "User books updated: ${books.size} books for user $userId")
            }.onFailure { error ->
                Log.e("BookViewModel", "Error fetching user books", error)
                _errorMessage.postValue("Failed to load your books: ${error.message}")
            }
            _isLoading.postValue(false)
        }
    }
    
    fun addBook(book: Book, imageUri: Uri?): LiveData<Result<String>> {
        val result = MutableLiveData<Result<String>>()
        viewModelScope.launch {
            _isLoading.postValue(true)
            
            try {
                // First upload the image if provided
                val imageUrlResult = if (imageUri != null) {
                    repository.uploadBookImage(imageUri)
                } else {
                    Result.success("")
                }
                
                imageUrlResult.onSuccess { imageUrl ->
                    // Create a copy of the book with the image URL
                    val bookWithImage = if (imageUrl.isNotEmpty()) {
                        book.copy(imageUrl = imageUrl)
                    } else {
                        book
                    }
                    
                    // Add the book to the database
                    repository.addBook(bookWithImage).onSuccess { bookId ->
                        result.postValue(Result.success(bookId))
                        // Refresh the book lists
                        fetchAllBooksInternal()
                        _user.value?.id?.let { fetchUserBooksInternal(it) }
                    }.onFailure { error ->
                        result.postValue(Result.failure(error))
                    }
                }.onFailure { error ->
                    result.postValue(Result.failure(error))
                }
            } catch (e: Exception) {
                result.postValue(Result.failure(e))
            } finally {
                _isLoading.postValue(false)
            }
        }
        return result
    }
    
    fun loadUserExchangeHistory(userId: String) {
        viewModelScope.launch {
            _isLoading.postValue(true)
            repository.getExchangesByUserId(userId).onSuccess { exchanges ->
                val given = exchanges.filter { it.oldOwnerId == userId }
                val received = exchanges.filter { it.newOwnerId == userId }
                _givenExchanges.postValue(given)
                _receivedExchanges.postValue(received)
                Log.d("BookViewModel", "Exchanges loaded: ${given.size} given, ${received.size} received")
            }.onFailure { error ->
                Log.e("BookViewModel", "Failed to load exchange history", error)
                _errorMessage.postValue("Failed to load exchange history: ${error.message}")
            }
            _isLoading.postValue(false)
        }
    }
    
    fun clearErrorMessage() {
        _errorMessage.postValue(null)
    }
    
    fun clearExchangeResult() {
        _exchangeResult.postValue(null)
    }
    
    fun triggerExchange(book: Book) {
        Log.d("BookViewModel", "Attempting to exchange book with ID: ${book.id}")
        viewModelScope.launch {
            _isLoading.postValue(true)
            val currentUserId = _user.value?.id ?: ""
            if (currentUserId.isEmpty()) {
                _errorMessage.postValue("Пользователь не авторизован")
                _isLoading.postValue(false)
                return@launch
            }
            
            val exchange = Exchange(
                bookId = book.id ?: "",
                oldOwnerId = book.userId ?: "",
                newOwnerId = currentUserId,
                status = "pending",
                bookTitle = book.title ?: "Неизвестная книга",
                oldOwnerName = book.ownerName ?: "Неизвестный владелец",
                newOwnerName = _user.value?.nickname ?: ""
            )
            
            repository.createExchange(exchange).onSuccess {
                _exchangeResult.postValue(Result.success(Unit))
            }.onFailure { error ->
                Log.e("BookViewModel", "Exchange failed", error)
                _errorMessage.postValue("Не удалось создать обмен: ${error.message}")
                _exchangeResult.postValue(Result.failure(error))
            }
            _isLoading.postValue(false)
        }
    }
    
    fun loginUser(email: String, password: String): LiveData<Result<FirebaseUser>> {
        val result = MutableLiveData<Result<FirebaseUser>>()
        viewModelScope.launch {
            _isLoading.postValue(true)
            repository.loginUser(email, password).onSuccess { user ->
                result.postValue(Result.success(user))
                // After successful login, try to load user data
                repository.getCurrentUser().onSuccess { currentUser ->
                    _user.postValue(currentUser)
                    Log.d("BookViewModel", "User data loaded after login: ${currentUser.nickname}")
                    fetchUserBooksInternal(currentUser.id)
                    loadUserExchangeHistory(currentUser.id)
                }.onFailure { error ->
                    Log.e("BookViewModel", "Failed to load user data after login", error)
                    _errorMessage.postValue("Вход выполнен, но не удалось загрузить данные пользователя")
                }
            }.onFailure { error ->
                Log.e("BookViewModel", "Login failed", error)
                _errorMessage.postValue("Ошибка входа: ${error.message}")
                result.postValue(Result.failure(error))
            }
            _isLoading.postValue(false)
        }
        return result
    }
    
    fun registerUser(
        nickname: String,
        name: String,
        city: String,
        street: String,
        houseNumber: String,
        age: Int,
        phone: String,
        email: String,
        password: String
    ): LiveData<Result<FirebaseUser>> {
        val result = MutableLiveData<Result<FirebaseUser>>()
        viewModelScope.launch {
            _isLoading.postValue(true)
            
            // Create a user object with the registration data
            val newUser = User(
                id = "", // Will be set by the repository
                nickname = nickname,
                name = name,
                email = email,
                city = city,
                street = street,
                houseNumber = houseNumber,
                age = age,
                phone = phone
            )
            
            // Call the repository to register the user
            repository.registerUser(newUser, password).onSuccess { firebaseUser ->
                result.postValue(Result.success(firebaseUser))
                
                // Load the user data after registration
                repository.getCurrentUser().onSuccess { currentUser ->
                    _user.postValue(currentUser)
                    Log.d("BookViewModel", "User data loaded after registration: ${currentUser.nickname}")
                }.onFailure { error ->
                    Log.e("BookViewModel", "Failed to load user data after registration", error)
                    _errorMessage.postValue("Регистрация выполнена, но не удалось загрузить данные пользователя")
                }
            }.onFailure { error ->
                Log.e("BookViewModel", "Registration failed", error)
                _errorMessage.postValue("Ошибка регистрации: ${error.message}")
                result.postValue(Result.failure(error))
            }
            
            _isLoading.postValue(false)
        }
        return result
    }
    
    fun saveUser(user: User) {
        Log.d("BookViewModel", "Attempting to save user ${user.id}")
        viewModelScope.launch {
            _isLoading.postValue(true)
            repository.updateUserProfile(user).onSuccess {
                Log.i("BookViewModel", "User ${user.id} updated successfully.")
                _user.postValue(user)
            }.onFailure { error ->
                Log.e("BookViewModel", "Failed to update user ${user.id}", error)
                _errorMessage.postValue("Failed to save profile: ${error.message}")
            }
            _isLoading.postValue(false)
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d("BookViewModel", "ViewModel Cleared")
    }
}