package com.example.bookswapkz.data

import android.net.Uri
import android.util.Log
import com.example.bookswapkz.models.* // Импортируй Exchange и ExchangeStatus
import com.google.firebase.auth.FirebaseAuth
// Импорты для обработки ошибок Auth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
// Импорты Firestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException // <<< --- ДОБАВЛЕН ИМПОРТ
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.WriteBatch
import com.google.firebase.firestore.ktx.toObject
// Импорты Storage
import com.google.firebase.storage.FirebaseStorage
// Импорты Kotlinx Coroutines
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
// Импорты Java Util
import java.util.Date
// Импорты Dagger/Hilt
import javax.inject.Inject
import javax.inject.Singleton
// Импорт Kotlin Result
import kotlin.Result

@Singleton
class FirebaseRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val storage: FirebaseStorage
) {

    private val storageRef = storage.reference.child("book_images")
    private val usersCollection = firestore.collection("users")
    private val booksCollection = firestore.collection("books")
    private val exchangesCollection = firestore.collection("exchanges")
    private val chatsCollection = firestore.collection("chats")
    private val messagesSubCollection = "messages"

    private val TAG = "FirebaseRepository"

    // --- Методы User ---
    suspend fun getUserById(userId: String): Result<User> = withContext(Dispatchers.IO) {
        if (userId.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("User ID cannot be blank"))
        }
        try {
            Log.d(TAG, "getUserById: Fetching user $userId")
            val document = usersCollection.document(userId).get().await()
            if (document.exists()) {
                val user = document.toObject<User>()
                if (user != null) {
                    Log.d(TAG, "getUserById: User $userId fetched successfully: $user")
                    Result.success(user)
                } else {
                    Log.e(TAG, "getUserById: Failed to convert document to User for ID $userId")
                    Result.failure(Exception("Failed to parse user data for ID: $userId"))
                }
            } else {
                Log.w(TAG, "getUserById: User document $userId not found")
                Result.failure(Exception("User not found with ID: $userId"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "getUserById: Error fetching user $userId", e)
            Result.failure(Exception("Error fetching user: ${e.localizedMessage}", e))
        }
    }

    suspend fun getCurrentUser(): Result<User> = withContext(Dispatchers.IO) {
        val firebaseUser = auth.currentUser
        if (firebaseUser == null) {
            Log.d(TAG, "getCurrentUser: No authenticated user.")
            Result.failure(Exception("User not authenticated"))
        } else {
            getUserById(firebaseUser.uid)
        }
    }

    suspend fun registerUser(
        nickname: String, name: String, city: String, street: String, houseNumber: String,
        age: Int, phone: String, email: String, password: String
    ): Result<FirebaseUser> = withContext(Dispatchers.IO) {
        Log.d(TAG, "registerUser: Attempting registration for email: $email")
        try {
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user
            if (firebaseUser != null) {
                Log.d(TAG, "registerUser: User created in Auth successfully: ${firebaseUser.uid}")
                val newUser = User(
                    userId = firebaseUser.uid, nickname = nickname, name = name,
                    email = email.lowercase(), age = age, city = city, street = street,
                    houseNumber = houseNumber, phone = phone
                )
                usersCollection.document(firebaseUser.uid).set(newUser).await()
                Log.d(TAG, "registerUser: User data saved to Firestore for UID: ${firebaseUser.uid}")
                Result.success(firebaseUser)
            } else {
                Log.e(TAG, "registerUser: FirebaseUser is null after successful Auth creation!")
                Result.failure(Exception("Failed to retrieve user data after creation."))
            }
        } catch (e: Exception) {
            Log.e(TAG, "registerUser: Error during registration for email $email", e)
            val errorMessage = when (e) {
                is FirebaseAuthUserCollisionException -> "Этот Email уже зарегистрирован."
                is FirebaseAuthWeakPasswordException -> "Пароль слишком слабый. Используйте минимум 6 символов."
                is FirebaseAuthInvalidCredentialsException -> "Некорректный формат email."
                else -> "Ошибка регистрации: ${e.localizedMessage}"
            }
            Result.failure(Exception(errorMessage, e))
        }
    }

    suspend fun loginUser(email: String, password: String): Result<FirebaseUser> = withContext(Dispatchers.IO) {
        Log.d(TAG, "loginUser: Attempting login for email: $email")
        try {
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val user = authResult.user
            if (user != null) {
                Log.i(TAG, "loginUser: signInWithEmail:success for user ${user.uid}")
                Result.success(user)
            } else {
                Result.failure(Exception("Login successful but user data is null"))
            }
        } catch (e: Exception) {
            Log.w(TAG, "loginUser: signInWithEmail:failure", e)
            val errorMessage = when (e) {
                is FirebaseAuthInvalidUserException -> "Пользователь не найден."
                is FirebaseAuthInvalidCredentialsException -> "Неверный email или пароль."
                else -> "Ошибка входа: ${e.localizedMessage}"
            }
            Result.failure(Exception(errorMessage, e))
        }
    }

    suspend fun updateUser(user: User): Result<Unit> = withContext(Dispatchers.IO) {
        if (user.userId.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("User ID cannot be blank for update"))
        }
        try {
            Log.d(TAG, "updateUser: Updating user ${user.userId} with data: $user")
            val userMap = mapOf(
                "nickname" to user.nickname, "name" to user.name, "email" to user.email.lowercase(),
                "age" to user.age, "city" to user.city, "street" to user.street,
                "houseNumber" to user.houseNumber, "phone" to user.phone
            )
            usersCollection.document(user.userId).set(userMap, SetOptions.merge()).await()
            Log.i(TAG, "updateUser: User ${user.userId} updated successfully.")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "updateUser: Error updating user ${user.userId}", e)
            Result.failure(Exception("Ошибка обновления профиля: ${e.localizedMessage}", e))
        }
    }

    // --- Методы Book ---
    suspend fun addBook(book: Book, imageUri: Uri?): Result<String> = withContext(Dispatchers.IO) {
        val currentUser = auth.currentUser ?: return@withContext Result.failure(Exception("Пользователь не авторизован"))
        try {
            val newBookRef = booksCollection.document()
            val bookId = newBookRef.id
            val userProfile = getUserById(currentUser.uid).getOrNull()
            var finalBook = book.copy(
                id = bookId, userId = currentUser.uid,
                ownerNickname = userProfile?.nickname ?: "Неизвестно",
                timestamp = Date().time // Используем Long для timestamp
                // Убедись, что ownerCount инициализируется правильно (например, 1)
                // ownerCount = 1
            )
            if (imageUri != null) {
                val imageUrl = uploadBookImage(bookId, imageUri).getOrThrow()
                finalBook = finalBook.copy(imageUrl = imageUrl)
            }
            newBookRef.set(finalBook).await()
            Log.i(TAG, "addBook: Book saved successfully with ID: $bookId")
            Result.success(bookId)
        } catch (e: Exception) {
            Log.e(TAG, "addBook: Error adding book", e)
            Result.failure(Exception("Не удалось добавить книгу: ${e.localizedMessage}", e))
        }
    }

    private suspend fun uploadBookImage(bookId: String, imageUri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            val imageFileName = "${bookId}_${System.currentTimeMillis()}.jpg"
            val imageRef = storageRef.child(imageFileName)
            imageRef.putFile(imageUri).await()
            val downloadUrl = imageRef.downloadUrl.await()
            Result.success(downloadUrl.toString())
        } catch (e: Exception) { Result.failure(Exception("Ошибка загрузки/получения URL изображения: ${e.localizedMessage}", e)) }
    }

    suspend fun getAllBooks(): Result<List<Book>> = withContext(Dispatchers.IO) {
        try {
            val querySnapshot = booksCollection.orderBy("timestamp", Query.Direction.DESCENDING).get().await()
            val books = querySnapshot.documents.mapNotNull { it.toObject<Book>() }
            Result.success(books)
        } catch (e: Exception) { Result.failure(Exception("Ошибка загрузки книг: ${e.localizedMessage}", e)) }
    }

    suspend fun getUserBooks(userId: String): Result<List<Book>> = withContext(Dispatchers.IO) {
        Log.d(TAG, "getUserBooks: Starting to fetch books for userId: $userId")
        if (userId.isBlank()) {
            Log.w(TAG, "getUserBooks: Blank userId provided")
            return@withContext Result.failure(IllegalArgumentException("User ID blank"))
        }
        try {
            Log.d(TAG, "getUserBooks: Executing Firestore query for user: $userId")
            val query = booksCollection.whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
            val querySnapshot = query.get().await()
            val books = querySnapshot.documents.mapNotNull { it.toObject<Book>() }
            Log.d(TAG, "getUserBooks: Successfully mapped ${books.size} books")
            Result.success(books)
        } catch (e: Exception) {
            Log.e(TAG, "getUserBooks: Failed to fetch books", e)
            Result.failure(Exception("Ошибка загрузки книг пользователя: ${e.localizedMessage}", e))
        }
    }

    suspend fun getBookById(bookId: String): Result<Book> = withContext(Dispatchers.IO) {
        if (bookId.isBlank()) return@withContext Result.failure(IllegalArgumentException("Book ID blank"))
        try {
            val document = booksCollection.document(bookId).get().await()
            if (document.exists()) {
                document.toObject<Book>()?.let { Result.success(it) }
                    ?: Result.failure(Exception("Failed to parse book: $bookId"))
            } else { Result.failure(Exception("Book not found: $bookId")) }
        } catch (e: Exception) { Result.failure(Exception("Error fetching book $bookId: ${e.localizedMessage}", e)) }
    }

    suspend fun updateBook(book: Book): Result<Unit> = withContext(Dispatchers.IO) {
        if (book.id.isBlank()) return@withContext Result.failure(IllegalArgumentException("Book ID blank"))
        try {
            booksCollection.document(book.id).set(book, SetOptions.merge()).await() // Используем merge для частичного обновления
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(Exception("Ошибка обновления книги: ${e.localizedMessage}", e)) }
    }

    // --- Методы Chat ---
    suspend fun getOrCreateChat(currentUserId: String, otherUserId: String): Result<String> = withContext(Dispatchers.IO) {
        if (currentUserId == otherUserId) {
            return@withContext Result.failure(IllegalArgumentException("Cannot create chat with oneself."))
        }
        val participants = listOf(currentUserId, otherUserId).sorted()
        val generatedChatId = "${participants[0]}_${participants[1]}"
        try {
            val chatRef = chatsCollection.document(generatedChatId)
            val chatSnapshot = chatRef.get().await()
            if (chatSnapshot.exists()) {
                Log.d(TAG, "Chat $generatedChatId already exists.")
                Result.success(generatedChatId)
            } else {
                Log.d(TAG, "Creating new chat: $generatedChatId")
                val currentUserProfile = getUserById(currentUserId).getOrNull()
                val otherUserProfile = getUserById(otherUserId).getOrNull()
                val participantInfoMap = mutableMapOf<String, Map<String, String?>>()
                currentUserProfile?.let { participantInfoMap[it.userId] = mapOf("nickname" to it.nickname) }
                otherUserProfile?.let { participantInfoMap[it.userId] = mapOf("nickname" to it.nickname) }
                val newChat = Chat(
                    chatId = generatedChatId, participantIds = participants, participantInfo = participantInfoMap,
                    lastMessageTimestamp = Date(), unreadCount = mapOf(currentUserId to 0L, otherUserId to 0L)
                )
                chatRef.set(newChat).await()
                Log.d(TAG, "Chat $generatedChatId created successfully.")
                Result.success(generatedChatId)
            }
        } catch (e: Exception) { Log.e(TAG, "Error getting or creating chat", e); Result.failure(e) }
    }

    fun getUserChatsFlow(userId: String): Flow<Result<List<Chat>>> = callbackFlow {
        Log.d(TAG, "getUserChatsFlow: Setting up listener for user $userId")
        if (userId.isBlank()) { trySend(Result.failure(IllegalArgumentException("User ID is blank"))); close(); return@callbackFlow }
        val query = chatsCollection.whereArrayContains("participantIds", userId)
            .orderBy("lastMessageTimestamp", Query.Direction.DESCENDING)
        val listener = query.addSnapshotListener { snapshots, e ->
            if (e != null) { trySend(Result.failure(e)); close(e); return@addSnapshotListener }
            if (snapshots != null) {
                trySend(Result.success(snapshots.documents.mapNotNull { it.toObject<Chat>() }))
            } else { trySend(Result.success(emptyList())) }
        }
        awaitClose { listener.remove() }
    }.flowOn(Dispatchers.IO)

    fun getChatMessagesFlow(chatId: String): Flow<Result<List<Message>>> = callbackFlow {
        Log.d(TAG, "getChatMessagesFlow: Setting up listener for chat $chatId")
        if (chatId.isBlank()) { trySend(Result.failure(IllegalArgumentException("Chat ID is blank"))); close(); return@callbackFlow }
        val currentUserId = auth.currentUser?.uid
        val query = chatsCollection.document(chatId).collection(messagesSubCollection)
            .orderBy("timestamp", Query.Direction.ASCENDING)
        val listener = query.addSnapshotListener { snapshots, e ->
            if (e != null) { trySend(Result.failure(e)); close(e); return@addSnapshotListener }
            if (snapshots != null) {
                val messages = snapshots.documents.mapNotNull { it.toObject<Message>() }
                trySend(Result.success(messages))
                if (currentUserId != null && messages.any { it.senderId != currentUserId }) {
                    chatsCollection.document(chatId).update("unreadCount.$currentUserId", 0)
                        .addOnFailureListener { Log.w(TAG, "Failed to reset unread count", it) }
                }
            } else { trySend(Result.success(emptyList())) }
        }
        awaitClose { listener.remove() }
    }.flowOn(Dispatchers.IO)

    suspend fun sendMessage(chatId: String, message: Message): Result<Unit> = withContext(Dispatchers.IO) {
        if (chatId.isBlank() || message.senderId.isBlank() || message.text.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Invalid message data"))
        }
        try {
            val chatRef = chatsCollection.document(chatId)
            val messageRef = chatRef.collection(messagesSubCollection).document()
            val chatDoc = chatRef.get().await().toObject<Chat>()
            val receiverId = chatDoc?.participantIds?.firstOrNull { it != message.senderId }
            val messageToSend = message.copy(id = messageRef.id, chatId = chatId, timestamp = Date())
            firestore.runBatch { batch ->
                batch.set(messageRef, messageToSend)
                val chatUpdates = hashMapOf<String, Any?>(
                    "lastMessageText" to messageToSend.text,
                    "lastMessageTimestamp" to FieldValue.serverTimestamp(),
                    "lastMessageSenderId" to messageToSend.senderId
                )
                if (receiverId != null) { chatUpdates["unreadCount.$receiverId"] = FieldValue.increment(1) }
                batch.update(chatRef, chatUpdates)
            }.await()
            Result.success(Unit)
        } catch (e: Exception) { Log.e(TAG, "Error sending message", e); Result.failure(e) }
    }

    // --- Методы для обмена (Вариант 2) ---

    suspend fun createExchangeRequest(exchange: Exchange): Result<String> = withContext(Dispatchers.IO) {
        try {
            val existingQuery = exchangesCollection
                .whereEqualTo("requesterId", exchange.requesterId)
                .whereEqualTo("requestedBookId", exchange.requestedBookId)
                .whereEqualTo("status", ExchangeStatus.PENDING.name)
                .limit(1).get().await()
            if (!existingQuery.isEmpty) {
                return@withContext Result.failure(IllegalStateException("Запрос на эту книгу уже существует."))
            }
            val newExchangeRef = exchangesCollection.document()
            val requestToSave = exchange.copy(id = newExchangeRef.id, createdAt = Date())
            newExchangeRef.set(requestToSave).await()
            Result.success(newExchangeRef.id)
        } catch (e: Exception) { Log.e(TAG, "Error creating exchange request", e); Result.failure(e) }
    }

    fun getPendingReceivedRequests(userId: String): Flow<Result<List<Exchange>>> = callbackFlow {
        Log.d(TAG, "Setting up listener for pending received requests for user $userId")
        if (userId.isBlank()) { trySend(Result.failure(IllegalArgumentException("User ID is blank"))); close(); return@callbackFlow }
        val query = exchangesCollection
            .whereEqualTo("requestedOwnerId", userId)
            .whereEqualTo("status", ExchangeStatus.PENDING.name)
            .orderBy("createdAt", Query.Direction.DESCENDING)
        val listener = query.addSnapshotListener { snapshots, e ->
            if (e != null) { trySend(Result.failure(e)); close(e); return@addSnapshotListener }
            if (snapshots != null) {
                trySend(Result.success(snapshots.documents.mapNotNull { it.toObject<Exchange>() }))
            } else { trySend(Result.success(emptyList())) }
        }
        awaitClose { listener.remove() }
    }.flowOn(Dispatchers.IO)

    suspend fun acceptExchange(
        exchangeId: String, bookId: String, requesterId: String, requesterNickname: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (exchangeId.isBlank() || bookId.isBlank() || requesterId.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("IDs cannot be blank"))
        }
        val exchangeRef = exchangesCollection.document(exchangeId)
        val bookRef = booksCollection.document(bookId)
        try {
            firestore.runTransaction { transaction ->
                val exchangeSnapshot = transaction.get(exchangeRef)
                val currentStatus = exchangeSnapshot.getString("status")
                if (currentStatus != ExchangeStatus.PENDING.name) {
                    // Используем стандартное исключение Kotlin вместо специфичного для Firestore
                    throw IllegalStateException("Exchange request already processed: $currentStatus")
                }
                // Обновляем книгу
                transaction.update(bookRef, "userId", requesterId)
                transaction.update(bookRef, "ownerNickname", requesterNickname)
                transaction.update(bookRef, "ownerCount", FieldValue.increment(1)) // Убедись, что ownerCount есть
                // Обновляем запрос
                transaction.update(exchangeRef, mapOf(
                    "status" to ExchangeStatus.ACCEPTED.name,
                    "processedAt" to FieldValue.serverTimestamp()
                ))
            }.await()
            Log.i(TAG, "Exchange request $exchangeId accepted.")
            Result.success(Unit)
        } catch (e: Exception) { Log.e(TAG, "Error accepting exchange $exchangeId", e); Result.failure(e) }
    }

    suspend fun rejectExchange(exchangeId: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (exchangeId.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Exchange ID cannot be blank"))
        }
        try {
            exchangesCollection.document(exchangeId).update(mapOf(
                "status" to ExchangeStatus.REJECTED.name,
                "processedAt" to FieldValue.serverTimestamp()
            )).await()
            Log.i(TAG, "Exchange request $exchangeId rejected.")
            Result.success(Unit)
        } catch (e: Exception) { Log.e(TAG, "Error rejecting exchange $exchangeId", e); Result.failure(e) }
    }

    // --- Заглушки для старых/неиспользуемых методов ---
    suspend fun recordExchangeAndUpdateBook(book: Book, newOwner: User): Result<Unit> = withContext(Dispatchers.IO) {
        Result.failure(NotImplementedError("Use createExchangeRequest"))
    }
    suspend fun getUserGivenHistory(userId: String): Result<List<Exchange>> = withContext(Dispatchers.IO) {
        Result.success(emptyList()) // Заглушка
    }
    suspend fun getUserReceivedHistory(userId: String): Result<List<Exchange>> = withContext(Dispatchers.IO) {
        Result.success(emptyList()) // Заглушка
    }
    suspend fun getUserExchangeHistory(userId: String): Result<Pair<List<Exchange>, List<Exchange>>> = withContext(Dispatchers.IO) {
        Result.success(Pair(emptyList(), emptyList())) // Заглушка
    }
}