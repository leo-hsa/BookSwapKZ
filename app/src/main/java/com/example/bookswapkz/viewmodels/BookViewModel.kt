package com.example.bookswapkz.viewmodels // Убедитесь, что пакет указан правильно

import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bookswapkz.FirebaseRepository // Убедитесь, что импорт репозитория правильный
import com.example.bookswapkz.models.Book
import com.example.bookswapkz.models.User
import kotlinx.coroutines.launch

class BookViewModel : ViewModel() {
    // Экземпляр репозитория для взаимодействия с Firebase
    private val repository = FirebaseRepository()

    // LiveData для текущего пользователя
    private val _user = MutableLiveData<User?>()
    val user: LiveData<User?> get() = _user

    // LiveData для списка всех книг
    private val _allBooks = MutableLiveData<List<Book>>()
    val allBooks: LiveData<List<Book>> get() = _allBooks

    // LiveData для списка книг текущего пользователя
    private val _myBooks = MutableLiveData<List<Book>>()
    val myBooks: LiveData<List<Book>> get() = _myBooks

    // LiveData для отображения сообщений об ошибках (можно расширить)
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> get() = _errorMessage


    // Блок инициализации: Загружаем начальные данные при создании ViewModel
    init {
        Log.d("BookViewModel", "ViewModel Initialized")
        getUser() // Получаем текущего пользователя (если он уже вошел)
        fetchAllBooks() // Загружаем список всех книг
    }

    /**
     * Получает данные текущего пользователя из репозитория и обновляет LiveData _user.
     * Если пользователь получен, загружает его книги.
     */
    fun getUser() {
        Log.d("BookViewModel", "Fetching user data...")
        viewModelScope.launch {
            // Используем observeForever здесь, так как ViewModel сама управляет своей областью видимости.
            // В реальном приложении может быть лучше использовать Flow или другие подходы.
            repository.getUser().observeForever { userResult ->
                if (userResult != null) {
                    _user.value = userResult
                    Log.d("BookViewModel", "User data updated: ${userResult.nickname}")
                    fetchUserBooks(userResult.userId) // Загружаем книги этого пользователя
                } else {
                    _user.value = null // Пользователь не найден или не вошел
                    Log.d("BookViewModel", "User is null or not logged in.")
                }
                // Сбрасываем ошибку при успешном получении данных
                _errorMessage.value = null
            }
            // TODO: Добавить обработку ошибок от repository.getUser(), если она возвращает статус/ошибку
        }
    }

    /**
     * Загружает список всех книг из репозитория.
     */
    private fun fetchAllBooks() {
        Log.d("BookViewModel", "Fetching all books...")
        viewModelScope.launch {
            repository.getAllBooks().observeForever { books ->
                _allBooks.value = books
                Log.d("BookViewModel", "All books updated: ${books?.size ?: 0} books")
            }
            // TODO: Обработка ошибок
        }
    }

    /**
     * Загружает список книг для конкретного пользователя.
     * @param userId ID пользователя, чьи книги нужно загрузить.
     */
    private fun fetchUserBooks(userId: String) {
        if (userId.isBlank()) {
            Log.w("BookViewModel", "fetchUserBooks called with blank userId.")
            _myBooks.value = emptyList() // Сбрасываем список, если ID пустой
            return
        }
        Log.d("BookViewModel", "Fetching books for user ID: $userId")
        viewModelScope.launch {
            repository.getUserBooks(userId).observeForever { books ->
                _myBooks.value = books
                Log.d("BookViewModel", "User books updated: ${books?.size ?: 0} books for user $userId")
            }
            // TODO: Обработка ошибок
        }
    }

    /**
     * Добавляет новую книгу и ее изображение (если есть) через репозиторий.
     * Возвращает LiveData<Boolean>, указывающий на успех операции.
     */
    fun addBook(book: Book, imageUri: Uri?): LiveData<Boolean> {
        val result = MutableLiveData<Boolean>()
        viewModelScope.launch {
            Log.d("BookViewModel", "Attempting to add book: ${book.title}")
            repository.addBook(book, imageUri).observeForever { success ->
                if (success) {
                    Log.i("BookViewModel", "Book '${book.title}' added successfully.")
                    // Можно обновить список всех книг или книг пользователя, но это должно происходить
                    // автоматически, если репозиторий правильно настроил слушатели Firebase.
                } else {
                    Log.e("BookViewModel", "Failed to add book: ${book.title}")
                    _errorMessage.value = "Не удалось добавить книгу" // Пример сообщения об ошибке
                }
                result.value = success
            }
        }
        return result
    }

    /**
     * Регистрирует нового пользователя через репозиторий.
     * Возвращает LiveData<Boolean>, указывающий на успех операции.
     * При успехе обновляет данные текущего пользователя.
     */
    fun registerUser(nickname: String, name: String, city: String, age: Int, phone: String, email: String, password: String): LiveData<Boolean> {
        val result = MutableLiveData<Boolean>()
        viewModelScope.launch {
            Log.d("BookViewModel", "Attempting to register user: $nickname ($email)")
            repository.registerUser(nickname, name, city, age, phone, email, password).observeForever { success ->
                if (success) {
                    Log.i("BookViewModel", "User registration successful for $nickname.")
                    getUser() // Получаем данные нового пользователя после регистрации
                } else {
                    Log.e("BookViewModel", "User registration failed for $nickname.")
                    _errorMessage.value = "Ошибка регистрации" // Пример сообщения об ошибке
                }
                result.value = success
            }
        }
        return result
    }

    /**
     * Выполняет вход пользователя через репозиторий.
     * Возвращает LiveData<Boolean>, указывающий на успех операции.
     * При успехе обновляет данные текущего пользователя.
     */
    fun loginUser(email: String, password: String): LiveData<Boolean> {
        val result = MutableLiveData<Boolean>()
        viewModelScope.launch {
            Log.d("BookViewModel", "Attempting to login user: $email")
            // Убедитесь, что метод loginUser СУЩЕСТВУЕТ в вашем FirebaseRepository!
            repository.loginUser(email, password).observeForever { success ->
                if (success) {
                    Log.i("BookViewModel", "Login successful for $email.")
                    getUser() // Получаем данные пользователя после входа
                } else {
                    Log.w("BookViewModel", "Login failed for $email.")
                    _errorMessage.value = "Неверный email или пароль" // Пример сообщения об ошибке
                }
                result.value = success
            }
            // TODO: Добавить более детальную обработку ошибок от репозитория (неверный пароль, пользователь не найден и т.д.)
        }
        return result
    }

    /**
     * Сохраняет (обновляет) данные пользователя.
     * TODO: Реализовать вызов метода репозитория для сохранения данных.
     */
    fun saveUser(name: String, nickname: String, city: String, age: Int, phone: String) {
        Log.w("BookViewModel", "saveUser function is not implemented yet.")
        // TODO: Реализовать логику сохранения данных пользователя
        // val currentUser = _user.value ?: return // Нужен текущий пользователь
        // val updatedUser = currentUser.copy(name = name, nickname = nickname, ...)
        // viewModelScope.launch { repository.updateUser(updatedUser) ... }
    }

    /**
     * Метод для сброса сообщения об ошибке после его отображения.
     */
    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        Log.d("BookViewModel", "ViewModel Cleared")
        // Важно: Если вы использовали observeForever без привязки к LifecycleOwner,
        // нужно отписаться здесь, чтобы избежать утечек.
        // Однако, viewModelScope автоматически отменяет корутины при очистке ViewModel.
        // Но если LiveData из репозитория - это внешние слушатели (напр. Firebase), их надо отключать.
        // repository.removeListeners() // Примерный метод для отключения слушателей в репозитории
    }
}