package com.example.bookswapkz // Убедитесь, что пакет правильный

import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.bookswapkz.models.Book
import com.example.bookswapkz.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query // Импорт для запросов
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await // Для использования await с Firebase задачами (опционально)

class FirebaseRepository {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
    private val usersCollection = firestore.collection("users")
    private val booksCollection = firestore.collection("books")
    private val storageRef = storage.reference.child("book_images") // Папка для изображений книг

    private val TAG = "FirebaseRepository"

    // Поле для хранения последней ошибки
    var lastError: String? = null
        private set // Сеттер доступен только внутри этого класса

    /**
     * Получает данные текущего авторизованного пользователя из Firestore.
     * Возвращает LiveData<User?>.
     */
    fun getUser(): LiveData<User?> {
        val userData = MutableLiveData<User?>()
        val firebaseUser: FirebaseUser? = auth.currentUser
        lastError = null // Сброс ошибки

        if (firebaseUser == null) {
            Log.d(TAG, "No current user found.")
            userData.postValue(null) // Отправляем null, если пользователя нет
        } else {
            Log.d(TAG, "Fetching user data for UID: ${firebaseUser.uid}")
            usersCollection.document(firebaseUser.uid).get()
                .addOnSuccessListener { documentSnapshot ->
                    if (documentSnapshot.exists()) {
                        val user = documentSnapshot.toObject(User::class.java)?.copy(userId = documentSnapshot.id) // Копируем ID документа в поле userId
                        userData.postValue(user)
                        Log.d(TAG, "User data fetched successfully: ${user?.nickname}")
                    } else {
                        Log.w(TAG, "User document does not exist for UID: ${firebaseUser.uid}")
                        userData.postValue(null) // Документ не найден
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error getting user document", e)
                    lastError = "Ошибка загрузки профиля: ${e.localizedMessage}"
                    userData.postValue(null) // Ошибка при загрузке
                }
        }
        return userData
    }

    /**
     * Получает список всех книг из Firestore.
     * Возвращает LiveData<List<Book>>.
     */
    fun getAllBooks(): LiveData<List<Book>> {
        val booksData = MutableLiveData<List<Book>>()
        lastError = null // Сброс ошибки
        Log.d(TAG, "Fetching all books...")

        booksCollection
            // .orderBy("timestamp", Query.Direction.DESCENDING) // Пример сортировки по времени добавления
            .addSnapshotListener { snapshots, e -> // Используем слушатель для обновлений в реальном времени
                if (e != null) {
                    Log.w(TAG, "Error listening for all books updates.", e)
                    lastError = "Ошибка загрузки книг: ${e.localizedMessage}"
                    booksData.postValue(emptyList()) // Возвращаем пустой список при ошибке
                    return@addSnapshotListener
                }

                val booksList = mutableListOf<Book>()
                if (snapshots != null) {
                    for (doc in snapshots) {
                        val book = doc.toObject(Book::class.java).copy(id = doc.id) // Копируем ID документа в поле id
                        booksList.add(book)
                    }
                }
                Log.d(TAG, "All books listener updated, ${booksList.size} books found.")
                booksData.postValue(booksList)
            }
        return booksData
    }

    /**
     * Получает список книг для конкретного пользователя из Firestore.
     * Возвращает LiveData<List<Book>>.
     */
    fun getUserBooks(userId: String): LiveData<List<Book>> {
        val booksData = MutableLiveData<List<Book>>()
        lastError = null // Сброс ошибки

        if (userId.isBlank()) {
            Log.w(TAG, "getUserBooks called with blank userId.")
            booksData.postValue(emptyList())
            return booksData
        }

        Log.d(TAG, "Fetching books for user ID: $userId")
        booksCollection.whereEqualTo("userId", userId)
            // .orderBy("timestamp", Query.Direction.DESCENDING) // Пример сортировки
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w(TAG, "Error listening for user books updates.", e)
                    lastError = "Ошибка загрузки ваших книг: ${e.localizedMessage}"
                    booksData.postValue(emptyList())
                    return@addSnapshotListener
                }

                val booksList = mutableListOf<Book>()
                if (snapshots != null) {
                    for (doc in snapshots) {
                        val book = doc.toObject(Book::class.java).copy(id = doc.id)
                        booksList.add(book)
                    }
                }
                Log.d(TAG, "User books listener updated, ${booksList.size} books found for user $userId.")
                booksData.postValue(booksList)
            }
        return booksData
    }


    /**
     * Добавляет книгу в Firestore и загружает изображение в Storage (если есть).
     * Возвращает LiveData<Boolean> с результатом операции.
     */
    fun addBook(book: Book, imageUri: Uri?): LiveData<Boolean> {
        val result = MutableLiveData<Boolean>()
        lastError = null // Сброс ошибки
        val currentUser = auth.currentUser

        if (currentUser == null) {
            lastError = "Пользователь не авторизован"
            result.postValue(false)
            return result
        }

        // Добавляем userId к объекту книги перед сохранением
        val bookWithUserId = book.copy(userId = currentUser.uid)

        // 1. Генерируем ID для новой книги
        val newBookRef = booksCollection.document()
        val bookId = newBookRef.id
        val bookToSave = bookWithUserId.copy(id = bookId) // Сохраняем с ID

        // 2. Загружаем изображение, если оно есть
        if (imageUri != null) {
            val imageRef = storageRef.child("${bookId}_${System.currentTimeMillis()}.jpg")
            Log.d(TAG, "Uploading image to: ${imageRef.path}")
            imageRef.putFile(imageUri)
                .addOnSuccessListener { taskSnapshot ->
                    Log.d(TAG, "Image upload successful.")
                    // 3. Получаем URL загруженного изображения
                    imageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                        Log.d(TAG, "Image URL obtained: $downloadUrl")
                        // 4. Сохраняем книгу с URL изображения
                        val bookWithImage = bookToSave.copy(imageUrl = downloadUrl.toString())
                        saveBookData(newBookRef, bookWithImage, result)
                    }.addOnFailureListener { e ->
                        Log.e(TAG, "Failed to get download URL", e)
                        lastError = "Ошибка получения URL изображения: ${e.localizedMessage}"
                        // Попытка сохранить книгу без изображения или вернуть ошибку?
                        // Пока сохраняем без картинки
                        saveBookData(newBookRef, bookToSave, result)
                        // result.postValue(false) // Если картинка обязательна
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Image upload failed", e)
                    lastError = "Ошибка загрузки изображения: ${e.localizedMessage}"
                    // Попытка сохранить книгу без изображения или вернуть ошибку?
                    // Пока сохраняем без картинки
                    saveBookData(newBookRef, bookToSave, result)
                    // result.postValue(false) // Если картинка обязательна
                }
        } else {
            // 3. Изображения нет, сразу сохраняем данные книги
            Log.d(TAG, "No image provided, saving book data directly.")
            saveBookData(newBookRef, bookToSave, result)
        }

        return result
    }

    // Вспомогательная функция для сохранения данных книги в Firestore
    private fun saveBookData(docRef: com.google.firebase.firestore.DocumentReference, book: Book, resultLiveData: MutableLiveData<Boolean>) {
        docRef.set(book)
            .addOnSuccessListener {
                Log.i(TAG, "Book data saved successfully for book ID: ${book.id}")
                resultLiveData.postValue(true)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error saving book data", e)
                lastError = "Ошибка сохранения данных книги: ${e.localizedMessage}"
                resultLiveData.postValue(false)
            }
    }

    /**
     * Регистрирует пользователя в Firebase Auth и сохраняет его данные в Firestore.
     * Возвращает LiveData<Boolean> с результатом операции.
     */
    fun registerUser(
        nickname: String, name: String, city: String, street: String, houseNumber: String,
        age: Int, phone: String, email: String, password: String
    ): LiveData<Boolean> {
        val result = MutableLiveData<Boolean>()
        lastError = null // Сброс ошибки

        Log.d(TAG, "Attempting registration for email: $email")
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                val firebaseUser = authResult.user
                if (firebaseUser != null) {
                    Log.d(TAG, "User created in Auth successfully: ${firebaseUser.uid}")
                    // Пользователь создан в Auth, теперь сохраняем доп. инфо в Firestore
                    val userMap = hashMapOf(
                        // Не сохраняем userId в документе, используем ID документа как userId
                        "nickname" to nickname,
                        "name" to name,
                        "email" to email.lowercase(), // Сохраняем email в нижнем регистре
                        "age" to age,
                        "city" to city,
                        "street" to street,
                        "houseNumber" to houseNumber,
                        "phone" to phone
                    )
                    usersCollection.document(firebaseUser.uid).set(userMap)
                        .addOnSuccessListener {
                            Log.d(TAG, "User data saved to Firestore for UID: ${firebaseUser.uid}")
                            result.postValue(true) // Вся регистрация успешна
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Error saving user data to Firestore", e)
                            // Пользователь создан в Auth, но данные не сохранились.
                            // Это плохая ситуация. Можно попытаться удалить пользователя из Auth или сообщить об особой ошибке.
                            lastError = "Ошибка сохранения профиля: ${e.localizedMessage}"
                            result.postValue(false)
                        }
                } else {
                    Log.e(TAG, "FirebaseUser is null after successful Auth creation!")
                    lastError = "Не удалось получить данные пользователя после создания."
                    result.postValue(false)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error creating user in Auth", e)
                lastError = "Ошибка регистрации: ${e.localizedMessage}" // Часто "email already in use"
                result.postValue(false)
            }
        return result
    }

    /**
     * Выполняет вход пользователя через Firebase Auth.
     * Возвращает LiveData<Boolean> с результатом операции.
     */
    fun loginUser(email: String, password: String): LiveData<Boolean> {
        val result = MutableLiveData<Boolean>()
        lastError = null // Сброс ошибки
        Log.d(TAG, "Attempting login for email: $email")

        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                Log.i(TAG, "signInWithEmail:success for user ${it.user?.uid}")
                result.postValue(true) // Вход успешен
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "signInWithEmail:failure", e)
                lastError = "Ошибка входа: ${e.localizedMessage}" // Часто "invalid credentials"
                result.postValue(false) // Вход не удался
            }
        return result
    }

    // TODO: Добавить методы для удаления книги, обновления данных пользователя и т.д.
    // TODO: Добавить метод для отписки от слушателей Firestore (removeListeners) для вызова в onCleared ViewModel

}