package com.example.bookswapkz.viewmodels

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.bookswapkz.data.FirebaseRepository
import com.example.bookswapkz.models.Book
import com.example.bookswapkz.models.Chat
import com.example.bookswapkz.models.Exchange
import com.example.bookswapkz.models.Message
import com.example.bookswapkz.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.Result

@HiltViewModel
class BookViewModel @Inject constructor(
    private val repository: FirebaseRepository,
    application: Application
) : AndroidViewModel(application) {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

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
    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> get() = _isLoading
    private val _givenExchanges = MutableLiveData<List<Exchange>>()
    val givenExchanges: LiveData<List<Exchange>> get() = _givenExchanges
    private val _receivedExchanges = MutableLiveData<List<Exchange>>()
    val receivedExchanges: LiveData<List<Exchange>> get() = _receivedExchanges
    private val _chatList = MutableLiveData<List<Chat>>()
    val chatList: LiveData<List<Chat>> get() = _chatList

    init {
        val currentUser = auth.currentUser
        Log.d("BookViewModel", "ViewModel Initialized. Current auth state: user=${currentUser?.uid}, isLoggedIn=${currentUser != null}")
        observeAuthState()

        if (currentUser != null && _user.value == null) {
            Log.d("BookViewModel", "User logged in but data not loaded, forcing initial load")
            loadCurrentUserAndData(currentUser.uid)
        }
    }

    private fun observeAuthState() {
        Log.d("BookViewModel", "Setting up auth state listener")
        auth.addAuthStateListener { firebaseAuth ->
            val firebaseUser = firebaseAuth.currentUser
            Log.d("BookViewModel", "Auth state changed - User: ${firebaseUser?.uid}")

            if (firebaseUser != null) {
                if (_user.value?.userId != firebaseUser.uid) {
                    Log.d("BookViewModel", "User changed or not loaded, loading data...")
                    loadCurrentUserAndData(firebaseUser.uid)
                } else {
                    Log.d("BookViewModel", "User already loaded, skipping reload")
                }
            } else {
                Log.d("BookViewModel", "No user logged in, clearing data")
                clearUserData()
            }
        }

        val currentUser = auth.currentUser
        Log.d("BookViewModel", "Initial auth check - Current user: ${currentUser?.uid}")

        if (currentUser != null) {
            Log.d("BookViewModel", "User already logged in, loading initial data")
            loadCurrentUserAndData(currentUser.uid)
        } else {
            Log.d("BookViewModel", "No user logged in initially, clearing data")
            clearUserData()
            _isLoading.value = false
        }
    }

    private fun clearUserData() {
        _user.postValue(null)
        _allBooks.postValue(emptyList())
        _myBooks.postValue(emptyList())
        _givenExchanges.postValue(emptyList())
        _receivedExchanges.postValue(emptyList())
        _chatList.postValue(emptyList())
        _errorMessage.postValue(null)
        Log.d("BookViewModel", "User data cleared.")
    }

    private fun loadCurrentUserAndData(userId: String) {
        Log.d("BookViewModel", "Starting to load data for user: $userId")
        viewModelScope.launch {
            try {
                _isLoading.postValue(true)
                Log.d("BookViewModel", "Getting user data from repository")
                val userResult = repository.getUserById(userId)

                userResult.onSuccess { currentUser ->
                    Log.d("BookViewModel", "Successfully loaded user: ${currentUser.nickname}")
                    _user.postValue(currentUser)
                    launch { fetchUserBooksInternal(currentUser.userId) }
                    launch { loadUserExchangeHistory(currentUser.userId) }
                    launch { loadUserChats() }
                    launch { fetchAllBooksInternal() }
                }.onFailure { error ->
                    Log.e("BookViewModel", "Failed to load user data", error)
                    _errorMessage.postValue("Ошибка загрузки профиля: ${error.localizedMessage}")
                    clearUserData()
                }
            } catch (e: Exception) {
                Log.e("BookViewModel", "Unexpected error in loadCurrentUserAndData", e)
                _errorMessage.postValue("Unexpected error: ${e.localizedMessage}")
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    private suspend fun fetchAllBooksInternal() {
        val booksResult = repository.getAllBooks()
        booksResult.onSuccess { _allBooks.postValue(it ?: emptyList()) }
            .onFailure { _errorMessage.postValue("Книги: ${it.localizedMessage}") }
    }

    private suspend fun fetchUserBooksInternal(userId: String) {
        Log.d("BookViewModel", "fetchUserBooksInternal called with userId: $userId")
        if (userId.isBlank()) {
            Log.d("BookViewModel", "UserId is blank, setting empty books list")
            _myBooks.postValue(emptyList())
            return
        }

        try {
            Log.d("BookViewModel", "Fetching books from repository")
            val booksResult = repository.getUserBooks(userId)
            booksResult.onSuccess { books ->
                val bookCount = books?.size ?: 0
                Log.d("BookViewModel", "Successfully fetched $bookCount books")
                _myBooks.postValue(books ?: emptyList())
            }.onFailure { error ->
                Log.e("BookViewModel", "Failed to fetch user books", error)
                _errorMessage.postValue("Мои книги: ${error.localizedMessage}")
                _myBooks.postValue(emptyList())
            }
        } catch (e: Exception) {
            Log.e("BookViewModel", "Unexpected error in fetchUserBooksInternal", e)
            _errorMessage.postValue("Unexpected error fetching books: ${e.localizedMessage}")
            _myBooks.postValue(emptyList())
        }
    }

    fun addBook(book: Book, imageUri: Uri?): LiveData<Result<String>> {
        val result = MutableLiveData<Result<String>>()
        viewModelScope.launch {
            _isLoading.postValue(true)
            val addResult = repository.addBook(book, imageUri)
            result.postValue(addResult)
            addResult.onFailure { _errorMessage.postValue("Добавление: ${it.localizedMessage}") }
            if (addResult.isSuccess) {
                fetchAllBooksInternal()
                auth.currentUser?.uid?.let { userId ->
                    Log.d("BookViewModel", "Reloading user books after adding new book")
                    fetchUserBooksInternal(userId)
                }
            }
            _isLoading.postValue(false)
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
            _isLoading.postValue(true)
            val registerResult = repository.registerUser(nickname, name, city, street, houseNumber, age, phone, email, password)
            result.postValue(registerResult)
            registerResult.onFailure { _errorMessage.postValue("Регистрация: ${it.localizedMessage}") }
            _isLoading.postValue(false)
        }
        return result
    }

    fun loginUser(email: String, password: String): LiveData<Result<FirebaseUser>> {
        val result = MutableLiveData<Result<FirebaseUser>>()
        viewModelScope.launch {
            _isLoading.postValue(true)
            val loginResult = repository.loginUser(email, password)
            result.postValue(loginResult)
            loginResult.onFailure { _errorMessage.postValue("Вход: ${it.localizedMessage}") }
            _isLoading.postValue(false)
        }
        return result
    }

    fun triggerExchange(bookToExchange: Book) {
        val currentUser = _user.value
        if (currentUser == null) {
            _errorMessage.postValue("Войдите для обмена")
            _exchangeResult.postValue(Result.failure(IllegalStateException("User not logged in")))
            return
        }
        if (bookToExchange.userId == currentUser.userId) {
            _errorMessage.postValue("Нельзя обменять свою же книгу")
            _exchangeResult.postValue(Result.failure(IllegalArgumentException("Cannot exchange own book")))
            return
        }

        _exchangeResult.postValue(null)
        _errorMessage.value = null

        viewModelScope.launch {
            Log.d("BookViewModel", "Triggering exchange for book ${bookToExchange.id} to user ${currentUser.userId}")
            _isLoading.postValue(true)
            val exchangeRepoResult = repository.recordExchangeAndUpdateBook(bookToExchange, currentUser)
            _exchangeResult.postValue(exchangeRepoResult)

            if (exchangeRepoResult.isSuccess) {
                Log.i("BookViewModel", "Exchange recorded successfully in repo for book ${bookToExchange.id}")
                launch { fetchAllBooksInternal() }
                launch { fetchUserBooksInternal(currentUser.userId) }
                launch { loadUserExchangeHistory(currentUser.userId) }
            } else {
                _errorMessage.postValue(exchangeRepoResult.exceptionOrNull()?.localizedMessage ?: "Ошибка обмена")
            }
            _isLoading.postValue(false)
        }
    }

    fun clearExchangeResult() { _exchangeResult.value = null }

    fun loadUserExchangeHistory(userId: String) {
        if (userId.isBlank()) return
        viewModelScope.launch {
            _isLoading.postValue(true)
            val historyResult = repository.getUserExchangeHistory(userId)
            historyResult.onSuccess { (given, received) ->
                _givenExchanges.postValue(given)
                _receivedExchanges.postValue(received)
                Log.d("BookViewModel", "Exchange history loaded: Given=${given.size}, Received=${received.size}")
            }.onFailure { error ->
                _givenExchanges.postValue(emptyList())
                _receivedExchanges.postValue(emptyList())
                _errorMessage.postValue("Ошибка загрузки истории: ${error.localizedMessage}")
            }
            _isLoading.postValue(false)
        }
    }

    fun loadUserChats() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _isLoading.postValue(true)
            repository.getUserChatsFlow(userId)
                .catch { e ->
                    _errorMessage.postValue("Чаты: ${e.localizedMessage}")
                    _isLoading.postValue(false)
                }
                .collect { result ->
                    result.onSuccess { chats -> _chatList.postValue(chats) }
                        .onFailure { _errorMessage.postValue("Чаты: ${it.localizedMessage}") }
                    _isLoading.postValue(false)
                }
        }
    }

    fun saveUser(user: User) {
        viewModelScope.launch {
            _isLoading.postValue(true)
            val result = repository.updateUser(user)
            result.onSuccess {
                _user.postValue(user)
            }.onFailure { error ->
                _errorMessage.postValue("Не удалось сохранить профиль: ${error.localizedMessage}")
            }
            _isLoading.postValue(false)
        }
    }

    fun clearErrorMessage() { _errorMessage.value = null }

    fun getOrCreateChatForNavigation(otherUserId: String): LiveData<Result<String>> {
        val result = MutableLiveData<Result<String>>()
        val currentUserId = auth.currentUser?.uid

        if (currentUserId == null || otherUserId == currentUserId) { // Fixed syntax
            result.postValue(Result.failure(Exception(if (currentUserId == null) "Сначала войдите" else "Нельзя чатиться с собой")))
            return result
        }
        viewModelScope.launch {
            _isLoading.postValue(true)
            val chatResult = repository.getOrCreateChat(currentUserId, otherUserId)
            result.postValue(chatResult)
            chatResult.onFailure { _errorMessage.postValue("Чат: ${it.localizedMessage}") }
            _isLoading.postValue(false)
        }
        return result
    }

    fun loadMyBooks() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            Log.d("BookViewModel", "Explicitly loading books for current user: ${currentUser.uid}")
            viewModelScope.launch {
                _isLoading.postValue(true)
                fetchUserBooksInternal(currentUser.uid)
                _isLoading.postValue(false)
            }
        } else {
            Log.d("BookViewModel", "Cannot load books - user not logged in")
            _errorMessage.postValue("Войдите, чтобы увидеть свои книги")
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d("BookViewModel", "ViewModel Cleared")
    }
}