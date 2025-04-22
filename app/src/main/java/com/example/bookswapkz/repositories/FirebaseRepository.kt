package com.example.bookswapkz.repositories // Убедитесь, что пакет правильный

import android.util.Log
import com.example.bookswapkz.models.Book
import com.example.bookswapkz.models.Exchange
import com.example.bookswapkz.models.User
import com.google.firebase.auth.FirebaseAuth // Добавлен импорт Auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions // Для merge при обновлении книги
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton // Для Hilt
class FirebaseRepository @Inject constructor() { // Для Hilt

    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance() // Добавлен Auth
    private val usersCollection = firestore.collection("users")
    private val booksCollection = firestore.collection("books")
    private val exchangesCollection = firestore.collection("exchanges")

    private val TAG = "FirebaseRepository"

    /**
     * Получает данные пользователя по его ID из Firestore.
     * Использует корутины и возвращает Result<User>.
     */
    suspend fun getUserById(userId: String): Result<User> = withContext(Dispatchers.IO) {
        if (userId.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("User ID cannot be blank"))
        }
        try {
            Log.d(TAG, "getUserById: Fetching user $userId")
            val document = usersCollection.document(userId).get().await()

            if (document.exists()) {
                // Пытаемся преобразовать и добавляем ID документа
                val user = document.toObject(User::class.java)?.copy(userId = document.id)
                if (user != null) {
                    Log.d(TAG, "getUserById: User $userId fetched successfully")
                    Result.success(user)
                } else {
                    Log.e(TAG, "getUserById: Failed to convert document to User for ID $userId")
                    Result.failure(Exception("Failed to parse user data"))
                }
            } else {
                Log.w(TAG, "getUserById: User document $userId not found")
                Result.failure(Exception("User not found"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "getUserById: Error fetching user $userId", e)
            Result.failure(e)
        }
    }

    /**
     * Получает данные ТЕКУЩЕГО авторизованного пользователя.
     * Возвращает Result<User>.
     */
    suspend fun getCurrentUser(): Result<User> = withContext(Dispatchers.IO) {
        val firebaseUser = auth.currentUser
        if (firebaseUser == null) {
            Log.d(TAG, "getCurrentUser: No authenticated user.")
            Result.failure(Exception("User not authenticated"))
        } else {
            getUserById(firebaseUser.uid) // Используем предыдущий метод
        }
    }

    /**
     * Записывает информацию об обмене в коллекцию 'exchanges'
     * и обновляет владельца и счетчик книги в коллекции 'books' с помощью транзакции.
     * Возвращает Result<Unit> для индикации успеха/ошибки.
     */
    suspend fun recordExchangeAndUpdateBook(book: Book, newOwner: User): Result<Unit> = withContext(Dispatchers.IO) {
        val oldOwnerId = book.userId // Текущий владелец из объекта книги
        val newOwnerId = newOwner.userId
        val bookId = book.id

        if (bookId.isBlank() || oldOwnerId.isBlank() || newOwnerId.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Invalid IDs for exchange recording"))
        }
        if (oldOwnerId == newOwnerId) {
            return@withContext Result.failure(IllegalArgumentException("Cannot exchange book with oneself"))
        }

        Log.d(TAG, "recordExchangeAndUpdateBook: Starting transaction for book $bookId from $oldOwnerId to $newOwnerId")

        try {
            firestore.runTransaction { transaction ->
                val bookRef = booksCollection.document(bookId)
                val bookSnapshot = transaction.get(bookRef)

                if (!bookSnapshot.exists()) {
                    Log.e(TAG, "recordExchangeAndUpdateBook: Book $bookId not found during transaction.")
                    throw FirebaseFirestoreException("Книга не найдена!", FirebaseFirestoreException.Code.ABORTED) // Прерываем транзакцию
                }

                // Проверяем текущего владельца на всякий случай
                val currentDbOwnerId = bookSnapshot.getString("userId")
                if (currentDbOwnerId != oldOwnerId) {
                    Log.e(TAG, "recordExchangeAndUpdateBook: Concurrent modification detected for book $bookId. Expected owner $oldOwnerId, found $currentDbOwnerId.")
                    throw FirebaseFirestoreException("Книгу уже забрали!", FirebaseFirestoreException.Code.ABORTED)
                }


                // 1. Обновляем книгу
                val currentOwnerCount = bookSnapshot.getLong("ownerCount") ?: 0
                val newOwnerCountValue = currentOwnerCount + 1
                val bookUpdates = mapOf(
                    "userId" to newOwnerId,
                    "phone" to newOwner.phone, // Берем телефон из объекта нового владельца
                    "ownerCount" to newOwnerCountValue
                )
                Log.d(TAG,"recordExchangeAndUpdateBook: Updating book $bookId with: $bookUpdates")
                transaction.update(bookRef, bookUpdates)

                // 2. Создаем запись об обмене
                val exchange = Exchange(
                    bookId = bookId,
                    oldOwnerId = oldOwnerId,
                    newOwnerId = newOwnerId,
                    exchangeDate = Date(), // Use current date instead of null
                    bookTitle = book.title,
                    oldOwnerName = "", // TODO: Нужно передать или загрузить имя старого владельца
                    newOwnerName = newOwner.nickname
                )
                val newExchangeRef = exchangesCollection.document()
                Log.d(TAG,"recordExchangeAndUpdateBook: Creating exchange record ${newExchangeRef.id}")
                transaction.set(newExchangeRef, exchange)

            }.await() // Ожидаем завершения транзакции

            Log.i(TAG, "recordExchangeAndUpdateBook: Transaction successful for book $bookId")
            Result.success(Unit) // Транзакция прошла успешно

        } catch (e: Exception) {
            Log.e(TAG, "recordExchangeAndUpdateBook: Transaction failed for book $bookId", e)
            Result.failure(e) // Транзакция не удалась
        }
    }


    /**
     * Получает историю обменов, где указанный пользователь был СТАРЫМ владельцем.
     * Возвращает Result<List<Exchange>>.
     */
    suspend fun getUserGivenHistory(userId: String): Result<List<Exchange>> = withContext(Dispatchers.IO) {
        if (userId.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("User ID cannot be blank"))
        }
        try {
            Log.d(TAG, "getUserGivenHistory: Fetching exchanges where oldOwnerId = $userId")
            val querySnapshot = exchangesCollection
                .whereEqualTo("oldOwnerId", userId)
                .orderBy("exchangeDate", Query.Direction.DESCENDING) // Сортируем по дате
                .get()
                .await()

            val exchanges = querySnapshot.documents.mapNotNull { doc ->
                try {
                    doc.toObject(Exchange::class.java)?.copy(id = doc.id) // Добавляем ID документа
                } catch (e: Exception) {
                    Log.e(TAG, "getUserGivenHistory: Failed to convert exchange document ${doc.id}", e)
                    null // Пропускаем документ с ошибкой парсинга
                }
            }
            Log.d(TAG, "getUserGivenHistory: Found ${exchanges.size} exchanges given by user $userId")
            Result.success(exchanges)
        } catch (e: Exception) {
            Log.e(TAG, "getUserGivenHistory: Error fetching history for user $userId", e)
            Result.failure(e)
        }
    }

    /**
     * Получает историю обменов, где указанный пользователь стал НОВЫМ владельцем.
     * Возвращает Result<List<Exchange>>.
     */
    suspend fun getUserReceivedHistory(userId: String): Result<List<Exchange>> = withContext(Dispatchers.IO) {
        if (userId.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("User ID cannot be blank"))
        }
        try {
            Log.d(TAG, "getUserReceivedHistory: Fetching exchanges where newOwnerId = $userId")
            val querySnapshot = exchangesCollection
                .whereEqualTo("newOwnerId", userId)
                .orderBy("exchangeDate", Query.Direction.DESCENDING)
                .get()
                .await()

            val exchanges = querySnapshot.documents.mapNotNull { doc ->
                try {
                    doc.toObject(Exchange::class.java)?.copy(id = doc.id)
                } catch (e: Exception) {
                    Log.e(TAG, "getUserReceivedHistory: Failed to convert exchange document ${doc.id}", e)
                    null
                }
            }
            Log.d(TAG, "getUserReceivedHistory: Found ${exchanges.size} exchanges received by user $userId")
            Result.success(exchanges)
        } catch (e: Exception) {
            Log.e(TAG, "getUserReceivedHistory: Error fetching history for user $userId", e)
            Result.failure(e)
        }
    }

    /**
     * Получает историю обменов пользователя по его ID.
     * Возвращает Result<List<Exchange>>.
     */
    suspend fun getUserExchangeHistory(userId: String): Result<List<Exchange>> = withContext(Dispatchers.IO) {
        if (userId.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("User ID cannot be blank"))
        }
        try {
            Log.d(TAG, "getUserExchangeHistory: Fetching exchange history for user $userId")
            val exchanges = exchangesCollection
                .whereEqualTo("oldOwnerId", userId)
                .get()
                .await()
                .documents
                .mapNotNull { document ->
                    document.toObject(Exchange::class.java)?.copy(id = document.id)
                }
            
            Log.d(TAG, "getUserExchangeHistory: Successfully fetched ${exchanges.size} exchanges for user $userId")
            Result.success(exchanges)
        } catch (e: Exception) {
            Log.e(TAG, "getUserExchangeHistory: Error fetching exchange history for user $userId", e)
            Result.failure(e)
        }
    }

    // TODO: Добавить сюда переписанные на suspend функции/Result:
    // suspend fun addBook(book: Book, imageUri: Uri?): Result<Unit>
    // suspend fun registerUser(...): Result<FirebaseUser> // Возвращать User из Auth
    // suspend fun loginUser(email: String, password: String): Result<FirebaseUser>
    // suspend fun getAllBooks(): Result<List<Book>> // Без LiveData
    // suspend fun getUserBooks(userId: String): Result<List<Book>> // Без LiveData

}