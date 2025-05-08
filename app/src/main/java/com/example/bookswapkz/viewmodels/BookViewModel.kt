package com.example.bookswapkz.viewmodels

import android.app.Application // <<< --- ИМПОРТ ДОБАВЛЕН
import android.net.Uri
import android.util.Log
import android.widget.Toast // <<< --- ИМПОРТ ДОБАВЛЕН
import androidx.lifecycle.AndroidViewModel // <<< --- ИЗМЕНЕНО НА AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.bookswapkz.data.FirebaseRepository
import com.example.bookswapkz.models.Book
import com.example.bookswapkz.models.Chat
import com.example.bookswapkz.models.Exchange
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
    private val application: Application // <<< --- ДОБАВЛЕНО Application в конструктор
) : AndroidViewModel(application) { // <<< --- НАСЛЕДУЕМСЯ ОТ AndroidViewModel

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val currentUserId: String? get() = auth.currentUser?.uid

    // ... остальные LiveData ...
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
        Log.d("BookViewModel", "ViewModel Initialized. Current auth state: user=${currentUserId}, isLoggedIn=${currentUserId != null}")
        observeAuthState()
        currentUserId?.let {
            if (_user.value == null) {
                Log.d("BookViewModel", "User logged in but data not loaded, forcing initial load for $it")
                loadCurrentUserAndData(it)
            }
        }
    }

    private fun observeAuthState() {
        // ... (твой код)
        Log.d("BookViewModel", "Setting up auth state listener")
        auth.addAuthStateListener { firebaseAuth ->
            val firebaseUser = firebaseAuth.currentUser
            Log.d("BookViewModel", "Auth state changed - User: ${firebaseUser?.uid}")

            if (firebaseUser != null) {
                if (_user.value?.userId != firebaseUser.uid) {
                    Log.d("BookViewModel", "User changed or not loaded (${_user.value?.userId} vs ${firebaseUser.uid}), loading data...")
                    loadCurrentUserAndData(firebaseUser.uid)
                } else {
                    Log.d("BookViewModel", "User already loaded (${firebaseUser.uid}), skipping reload on auth state change.")
                }
            } else {
                Log.d("BookViewModel", "No user logged in, clearing data")
                clearUserData()
            }
        }
    }

    private fun clearUserData() {
        // ... (твой код)
        _user.postValue(null)
        _allBooks.postValue(emptyList())
        _myBooks.postValue(emptyList())
        _givenExchanges.postValue(emptyList())
        _receivedExchanges.postValue(emptyList())
        _chatList.postValue(emptyList())
        // _errorMessage.postValue(null) // Оставим ошибки, если они были
        _isLoading.postValue(false)
        Log.d("BookViewModel", "User data cleared.")
    }

    private fun loadCurrentUserAndData(userId: String) {
        // ... (твой код)
        Log.d("BookViewModel", "Starting to load data for user: $userId")
        if (userId.isBlank()) {
            Log.e("BookViewModel", "Cannot load data for blank userId.")
            clearUserData()
            return
        }
        viewModelScope.launch {
            _isLoading.postValue(true)
            Log.d("BookViewModel", "Getting user data from repository for $userId")
            val userResult = repository.getUserById(userId)

            userResult.onSuccess { currentUser ->
                Log.d("BookViewModel", "Successfully loaded user: ${currentUser.nickname} for ID: ${currentUser.userId}")
                _user.postValue(currentUser)
                launch { fetchUserBooksInternal(currentUser.userId) }
                launch { loadUserExchangeHistory(currentUser.userId) }
                launch { loadUserChats(currentUser.userId) }
                launch { fetchAllBooksInternal() }
            }.onFailure { error ->
                Log.e("BookViewModel", "Failed to load user data for $userId", error)
                _errorMessage.postValue("Ошибка загрузки профиля: ${error.localizedMessage}")
                clearUserData()
            }
            // _isLoading.postValue(false) // Управляется в каждой корутине
        }
    }

    private suspend fun fetchAllBooksInternal() {
        // ... (твой код)
        val booksResult = repository.getAllBooks()
        booksResult.onSuccess { books ->
            _allBooks.postValue(books ?: emptyList())
            Log.d("BookViewModel", "Fetched ${books?.size ?: 0} all books.")
        }.onFailure { error ->
            _errorMessage.postValue("Ошибка загрузки книг: ${error.localizedMessage}")
        }
    }

    private suspend fun fetchUserBooksInternal(userId: String) {
        // ... (твой код)
        Log.d("BookViewModel", "fetchUserBooksInternal called with userId: $userId")
        if (userId.isBlank()) {
            Log.w("BookViewModel", "UserId is blank in fetchUserBooksInternal, setting empty books list")
            _myBooks.postValue(emptyList())
            return
        }
        val booksResult = repository.getUserBooks(userId)
        booksResult.onSuccess { books ->
            Log.d("BookViewModel", "Successfully fetched ${books?.size ?: 0} books for user $userId")
            _myBooks.postValue(books ?: emptyList())
        }.onFailure { error ->
            Log.e("BookViewModel", "Failed to fetch user books for $userId", error)
            _errorMessage.postValue("Ошибка загрузки ваших книг: ${error.localizedMessage}")
            _myBooks.postValue(emptyList())
        }
    }

    fun addBook(book: Book, imageUri: Uri?): LiveData<Result<String>> {
        // ... (твой код)
        val result = MutableLiveData<Result<String>>()
        viewModelScope.launch {
            _isLoading.postValue(true)
            val addResult = repository.addBook(book, imageUri)
            result.postValue(addResult)
            addResult.onSuccess {
                Log.i("BookViewModel", "Book added successfully, reloading lists.")
                fetchAllBooksInternal()
                currentUserId?.let { fetchUserBooksInternal(it) }
            }.onFailure {
                _errorMessage.postValue("Ошибка добавления книги: ${it.localizedMessage}")
            }
            _isLoading.postValue(false)
        }
        return result
    }

    fun registerUser(
        email: String, password: String, name: String, nickname: String,
        city: String, street: String, houseNumber: String,
        age: Int, phone: String
    ): LiveData<Result<FirebaseUser>> {
        // ... (твой код)
        val result = MutableLiveData<Result<FirebaseUser>>()
        viewModelScope.launch {
            _isLoading.postValue(true)
            val registerResult = repository.registerUser(nickname, name, city, street, houseNumber, age, phone, email, password)
            result.postValue(registerResult)
            registerResult.onFailure {
                _errorMessage.postValue("Ошибка регистрации: ${it.localizedMessage}")
            }
            _isLoading.postValue(false)
        }
        return result
    }

    fun loginUser(email: String, password: String): LiveData<Result<FirebaseUser>> {
        // ... (твой код)
        val result = MutableLiveData<Result<FirebaseUser>>()
        viewModelScope.launch {
            _isLoading.postValue(true)
            val loginResult = repository.loginUser(email, password)
            result.postValue(loginResult)
            loginResult.onFailure {
                _errorMessage.postValue("Ошибка входа: ${it.localizedMessage}")
            }
            _isLoading.postValue(false)
        }
        return result
    }

    fun triggerExchange(bookToExchange: Book) { /* ... */ }
    fun clearExchangeResult() { _exchangeResult.value = null }
    fun loadUserExchangeHistory(userId: String) { /* ... */ }

    fun loadUserChats(userId: String) {
        // ... (твой код)
        if (userId.isBlank()) {
            Log.w("BookViewModel", "Cannot load user chats, userId is blank.")
            _chatList.postValue(emptyList())
            return
        }
        viewModelScope.launch {
            repository.getUserChatsFlow(userId)
                .catch { e ->
                    Log.e("BookViewModel", "Error in user chats flow for $userId", e)
                    _errorMessage.postValue("Ошибка загрузки списка чатов: ${e.localizedMessage}")
                    _chatList.postValue(emptyList())
                }
                .collect { result ->
                    result.onSuccess { chats ->
                        Log.d("BookViewModel", "Loaded ${chats.size} chats for user $userId")
                        _chatList.postValue(chats)
                    }
                    result.onFailure { e ->
                        Log.e("BookViewModel", "Failure collecting user chats for $userId", e)
                        _errorMessage.postValue("Ошибка при обновлении списка чатов: ${e.localizedMessage}")
                        _chatList.postValue(emptyList())
                    }
                }
        }
    }

    fun saveUser(userToSave: User) {
        viewModelScope.launch {
            _isLoading.postValue(true)
            val result = repository.updateUser(userToSave)
            result.onSuccess {
                _user.postValue(userToSave)
                Log.i("BookViewModel", "User profile saved successfully.")
                // Теперь getApplication() доступен
                Toast.makeText(getApplication(), "Профиль сохранен", Toast.LENGTH_SHORT).show()
            }.onFailure { error ->
                _errorMessage.postValue("Не удалось сохранить профиль: ${error.localizedMessage}")
            }
            _isLoading.postValue(false)
        }
    }

    fun clearErrorMessage() { _errorMessage.value = null }

    fun getOrCreateChatForNavigation(otherUserId: String): LiveData<Result<String>> {
        // ... (твой код)
        val resultLiveData = MutableLiveData<Result<String>>()
        val localCurrentUserId = currentUserId

        if (localCurrentUserId == null) {
            resultLiveData.postValue(Result.failure(Exception("Сначала войдите в систему.")))
            return resultLiveData
        }
        if (otherUserId == localCurrentUserId) {
            resultLiveData.postValue(Result.failure(Exception("Нельзя начать чат с самим собой.")))
            return resultLiveData
        }
        if (otherUserId.isBlank()) {
            resultLiveData.postValue(Result.failure(Exception("ID собеседника не указан.")))
            return resultLiveData
        }

        _isLoading.postValue(true)
        viewModelScope.launch {
            Log.d("BookViewModel", "getOrCreateChatForNavigation: currentUserId=$localCurrentUserId, otherUserId=$otherUserId")
            val chatResult = repository.getOrCreateChat(localCurrentUserId, otherUserId)
            resultLiveData.postValue(chatResult)
            chatResult.onSuccess { chatId ->
                Log.i("BookViewModel", "Chat get/create success. ChatId: $chatId")
            }.onFailure { error ->
                Log.e("BookViewModel", "Chat get/create failed.", error)
                _errorMessage.postValue("Не удалось начать чат: ${error.localizedMessage}")
            }
            _isLoading.postValue(false)
        }
        return resultLiveData
    }

    fun loadMyBooks() {
        // ... (твой код)
        currentUserId?.let { userId ->
            Log.d("BookViewModel", "Explicitly loading books for current user: $userId")
            viewModelScope.launch {
                _isLoading.postValue(true)
                fetchUserBooksInternal(userId)
                _isLoading.postValue(false)
            }
        } ?: run {
            Log.w("BookViewModel", "Cannot load my books - user not logged in.")
            _errorMessage.postValue("Войдите, чтобы увидеть свои книги.")
            _myBooks.postValue(emptyList())
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d("BookViewModel", "ViewModel Cleared")
    }
}