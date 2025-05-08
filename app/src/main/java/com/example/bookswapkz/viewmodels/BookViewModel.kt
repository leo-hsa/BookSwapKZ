package com.example.bookswapkz.viewmodels

import android.app.Application
import android.net.Uri
import android.util.Log
import android.widget.Toast // Импорт для Toast
import androidx.lifecycle.AndroidViewModel // Наследуемся от AndroidViewModel для доступа к Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.bookswapkz.data.FirebaseRepository
import com.example.bookswapkz.models.Book
import com.example.bookswapkz.models.Chat
import com.example.bookswapkz.models.Exchange
import com.example.bookswapkz.models.ExchangeStatus // Импорт Enum статуса
import com.example.bookswapkz.models.User
import com.example.bookswapkz.utils.Event // Импорт Event wrapper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect // Импорт collect
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.Result

@HiltViewModel
class BookViewModel @Inject constructor(
    private val repository: FirebaseRepository,
    application: Application // Hilt предоставит Application
) : AndroidViewModel(application) { // Наследуемся от AndroidViewModel

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val currentUserId: String? get() = auth.currentUser?.uid

    // LiveData для данных пользователя
    private val _user = MutableLiveData<User?>()
    val user: LiveData<User?> get() = _user

    // LiveData для книг
    private val _allBooks = MutableLiveData<List<Book>>()
    val allBooks: LiveData<List<Book>> get() = _allBooks
    private val _myBooks = MutableLiveData<List<Book>>()
    val myBooks: LiveData<List<Book>> get() = _myBooks

    // LiveData для состояний и ошибок
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> get() = _errorMessage
    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> get() = _isLoading

    // LiveData для чатов (если нужно)
    private val _chatList = MutableLiveData<List<Chat>>()
    val chatList: LiveData<List<Chat>> get() = _chatList

    // --- LiveData для обмена (Вариант 2) ---
    private val _pendingReceivedRequests = MutableLiveData<List<Exchange>>()
    val pendingReceivedRequests: LiveData<List<Exchange>> get() = _pendingReceivedRequests

    private val _exchangeActionResult = MutableLiveData<Event<Result<Unit>>>()
    val exchangeActionResult: LiveData<Event<Result<Unit>>> get() = _exchangeActionResult

    private val _requestSentResult = MutableLiveData<Event<Result<Unit>>>()
    val requestSentResult: LiveData<Event<Result<Unit>>> get() = _requestSentResult

    private val _isLoadingRequests = MutableLiveData<Boolean>(false)
    val isLoadingRequests: LiveData<Boolean> get() = _isLoadingRequests


    init {
        Log.d("BookViewModel", "ViewModel Initialized.")
        observeAuthState()
        currentUserId?.let { if (_user.value == null) loadCurrentUserAndData(it) }
    }

    private fun observeAuthState() {
        auth.addAuthStateListener { firebaseAuth ->
            val firebaseUser = firebaseAuth.currentUser
            Log.d("BookViewModel", "Auth state changed - User: ${firebaseUser?.uid}")
            if (firebaseUser != null) {
                if (_user.value?.userId != firebaseUser.uid) loadCurrentUserAndData(firebaseUser.uid)
            } else {
                clearUserData()
            }
        }
    }

    private fun clearUserData() {
        _user.postValue(null)
        _allBooks.postValue(emptyList())
        _myBooks.postValue(emptyList())
        _pendingReceivedRequests.postValue(emptyList())
        _chatList.postValue(emptyList())
        _errorMessage.postValue(null)
        _isLoading.postValue(false)
        _isLoadingRequests.postValue(false)
        Log.d("BookViewModel", "User data cleared.")
    }

    private fun loadCurrentUserAndData(userId: String) {
        Log.d("BookViewModel", "Starting to load data for user: $userId")
        if (userId.isBlank()) return
        viewModelScope.launch {
            _isLoading.postValue(true)
            val userResult = repository.getUserById(userId)
            userResult.onSuccess { currentUser ->
                _user.postValue(currentUser)
                launch { fetchUserBooksInternal(currentUser.userId) }
                launch { loadUserChats(currentUser.userId) }
                launch { loadPendingReceivedRequests() }
                launch { fetchAllBooksInternal() }
            }.onFailure { error ->
                _errorMessage.postValue("Ошибка загрузки профиля: ${error.localizedMessage}")
                clearUserData()
            }
            _isLoading.postValue(false) // Скрываем основной isLoading после загрузки пользователя
        }
    }

    private suspend fun fetchAllBooksInternal() {
        val booksResult = repository.getAllBooks()
        booksResult.onSuccess { _allBooks.postValue(it ?: emptyList()) }
            .onFailure { _errorMessage.postValue("Книги: ${it.localizedMessage}") }
    }

    private suspend fun fetchUserBooksInternal(userId: String) {
        if (userId.isBlank()) { _myBooks.postValue(emptyList()); return }
        val booksResult = repository.getUserBooks(userId)
        booksResult.onSuccess { _myBooks.postValue(it ?: emptyList()) }
            .onFailure { _errorMessage.postValue("Мои книги: ${it.localizedMessage}") }
    }

    fun addBook(book: Book, imageUri: Uri?): LiveData<Result<String>> {
        val result = MutableLiveData<Result<String>>()
        viewModelScope.launch {
            _isLoading.postValue(true)
            val addResult = repository.addBook(book, imageUri)
            result.postValue(addResult)
            addResult.onSuccess {
                fetchAllBooksInternal()
                currentUserId?.let { fetchUserBooksInternal(it) }
            }.onFailure { _errorMessage.postValue("Добавление: ${it.localizedMessage}") }
            _isLoading.postValue(false)
        }
        return result
    }

    fun registerUser(/*...*/): LiveData<Result<FirebaseUser>> {
        val result = MutableLiveData<Result<FirebaseUser>>()
        viewModelScope.launch { /* ... */ }
        return result
    } // Реализация как раньше

    fun loginUser(email: String, password: String): LiveData<Result<FirebaseUser>> {
        val result = MutableLiveData<Result<FirebaseUser>>()
        viewModelScope.launch { /* ... */ }
        return result
    }// Реализация как раньше

    fun saveUser(userToSave: User) {
        viewModelScope.launch { /* ... */ }
    }// Реализация как раньше

    fun loadUserChats(userId: String) {
        if (userId.isBlank()) return
        viewModelScope.launch { /* ... */ }
    }// Реализация как раньше

    fun getOrCreateChatForNavigation(otherUserId: String): LiveData<Result<String>> {
        val resultLiveData = MutableLiveData<Result<String>>()
        viewModelScope.launch { /* ... */ }
        return resultLiveData
    }// Реализация как раньше

    fun loadMyBooks() {
        currentUserId?.let { userId ->
            viewModelScope.launch {
                _isLoading.postValue(true)
                fetchUserBooksInternal(userId)
                _isLoading.postValue(false)
            }
        } ?: _errorMessage.postValue("Войдите, чтобы увидеть свои книги")
    }

    // --- Методы для обмена (Вариант 2) ---

    fun requestExchange(bookToRequest: Book) {
        val currentUser = _user.value
        if (currentUser == null || currentUser.userId.isBlank() || bookToRequest.userId.isBlank() || bookToRequest.id.isBlank() || bookToRequest.userId == currentUser.userId) {
            val errorMsg = when {
                currentUser == null -> "Войдите в систему"
                bookToRequest.userId == currentUser.userId -> "Нельзя запросить свою книгу"
                else -> "Ошибка данных книги/пользователя"
            }
            _errorMessage.postValue(errorMsg)
            _requestSentResult.postValue(Event(Result.failure(Exception(errorMsg))))
            return
        }

        _isLoading.postValue(true) // Используем основной isLoading для этой операции
        viewModelScope.launch {
            val exchangeRequest = Exchange(
                requesterId = currentUser.userId,
                requesterNickname = currentUser.nickname,
                requestedOwnerId = bookToRequest.userId,
                requestedBookId = bookToRequest.id,
                requestedBookTitle = bookToRequest.title,
                requestedBookImageUrl = bookToRequest.imageUrl,
                status = ExchangeStatus.PENDING.name
            )
            val result = repository.createExchangeRequest(exchangeRequest)
            _requestSentResult.postValue(Event(result.map { Unit }))

            result.onSuccess { requestId ->
                Log.i("BookViewModel", "Exchange request created: $requestId")
                Toast.makeText(getApplication(), "Запрос на обмен отправлен", Toast.LENGTH_SHORT).show()
            }.onFailure { error ->
                Log.e("BookViewModel", "Failed to create exchange request", error)
                _errorMessage.postValue("Не удалось отправить запрос: ${error.localizedMessage}")
            }
            _isLoading.postValue(false)
        }
    }

    // <<<--- ИСПРАВЛЕН ЭТОТ МЕТОД ---<<<
    fun loadPendingReceivedRequests() {
        val userId = currentUserId ?: return
        if (userId.isBlank()) return

        _isLoadingRequests.postValue(true)
        _errorMessage.postValue(null)
        Log.d("BookViewModel", "Loading pending received requests for user: $userId")

        viewModelScope.launch {
            // Используем Flow из репозитория и collect
            repository.getPendingReceivedRequests(userId)
                .catch { e -> // Обработка ошибок в самом Flow
                    Log.e("BookViewModel", "Error in pending received requests flow", e)
                    _errorMessage.postValue("Ошибка загрузки запросов: ${e.localizedMessage}")
                    _pendingReceivedRequests.postValue(emptyList())
                    _isLoadingRequests.postValue(false) // Важно сбросить флаг при ошибке Flow
                }
                .collect { result -> // Собираем Result из Flow
                    // isLoadingRequests сбрасывается только после получения первого результата (или ошибки выше)
                    _isLoadingRequests.postValue(false)
                    result.onSuccess { requests ->
                        Log.d("BookViewModel", "Loaded ${requests.size} pending requests.")
                        _pendingReceivedRequests.postValue(requests)
                    }.onFailure { error -> // Ошибка внутри Result
                        Log.e("BookViewModel", "Failure collecting received requests", error)
                        _errorMessage.postValue("Ошибка обновления запросов: ${error.localizedMessage}")
                        _pendingReceivedRequests.postValue(emptyList())
                    }
                }
        }
    }
    // --- >>>

    // <<<--- ИСПРАВЛЕН ЭТОТ МЕТОД ---<<<
    fun acceptExchange(exchange: Exchange) {
        if (exchange.id.isBlank() || exchange.requestedBookId.isBlank() || exchange.requesterId.isBlank()) {
            _errorMessage.postValue("Ошибка данных для подтверждения обмена.")
            _exchangeActionResult.postValue(Event(Result.failure(IllegalArgumentException("Invalid exchange data for acceptance"))))
            return
        }
        _isLoadingRequests.postValue(true) // Используем isLoadingRequests
        _errorMessage.postValue(null)

        viewModelScope.launch {
            Log.d("BookViewModel", "Accepting exchange request: ${exchange.id}")
            // Передаем все нужные параметры
            val result = repository.acceptExchange(
                exchangeId = exchange.id,
                bookId = exchange.requestedBookId,
                requesterId = exchange.requesterId,
                requesterNickname = exchange.requesterNickname ?: "Пользователь"
            )
            _exchangeActionResult.postValue(Event(result)) // Отправляем результат в Event

            result.onSuccess {
                Log.i("BookViewModel", "Exchange ${exchange.id} accepted successfully.")
                Toast.makeText(getApplication(), "Обмен подтвержден!", Toast.LENGTH_SHORT).show()
                loadPendingReceivedRequests()
                loadMyBooks()
                fetchAllBooksInternal()
            }.onFailure { error ->
                Log.e("BookViewModel", "Failed to accept exchange ${exchange.id}", error)
                _errorMessage.postValue("Не удалось подтвердить обмен: ${error.localizedMessage}")
            }
            _isLoadingRequests.postValue(false) // Сбрасываем флаг
        }
    }
    // --- >>>

    // <<<--- ИСПРАВЛЕН ЭТОТ МЕТОД ---<<<
    fun rejectExchange(exchange: Exchange) {
        if (exchange.id.isBlank()) {
            _errorMessage.postValue("Ошибка данных для отклонения запроса.")
            _exchangeActionResult.postValue(Event(Result.failure(IllegalArgumentException("Invalid exchange data for rejection"))))
            return
        }
        _isLoadingRequests.postValue(true) // Используем isLoadingRequests
        _errorMessage.postValue(null)

        viewModelScope.launch {
            Log.d("BookViewModel", "Rejecting exchange request: ${exchange.id}")
            val result = repository.rejectExchange(exchange.id)
            _exchangeActionResult.postValue(Event(result)) // Отправляем результат в Event

            result.onSuccess {
                Log.i("BookViewModel", "Exchange ${exchange.id} rejected successfully.")
                Toast.makeText(getApplication(), "Запрос отклонен", Toast.LENGTH_SHORT).show()
                loadPendingReceivedRequests()
            }.onFailure { error ->
                Log.e("BookViewModel", "Failed to reject exchange ${exchange.id}", error)
                _errorMessage.postValue("Не удалось отклонить запрос: ${error.localizedMessage}")
            }
            _isLoadingRequests.postValue(false) // Сбрасываем флаг
        }
    }
    // --- >>>

    fun clearErrorMessage() { _errorMessage.value = null }

    override fun onCleared() {
        super.onCleared()
        Log.d("BookViewModel", "ViewModel Cleared")
    }
}