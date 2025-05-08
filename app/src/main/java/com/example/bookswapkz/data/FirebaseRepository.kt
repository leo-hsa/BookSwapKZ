package com.example.bookswapkz.data

import android.net.Uri
import android.util.Log
import com.example.bookswapkz.models.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton
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
                    userId = firebaseUser.uid,
                    nickname = nickname,
                    name = name,
                    email = email.lowercase(),
                    age = age,
                    city = city,
                    street = street,
                    houseNumber = houseNumber,
                    phone = phone
                    // avatarUrl убран
                )
                usersCollection.document(firebaseUser.uid).set(newUser).await()
                Log.d(TAG, "registerUser: User data saved to Firestore for UID: ${firebaseUser.uid}")
                Result.success(firebaseUser)
            } else {
                Log.e(TAG, "registerUser: FirebaseUser is null after successful Auth creation!")
                Result.failure(Exception("Failed to retrieve user data after creation."))
            }
        } catch (e: Exception) {
            // ... обработка ошибок ...
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
        // ... (без изменений) ...
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
                "nickname" to user.nickname,
                "name" to user.name,
                "email" to user.email.lowercase(),
                "age" to user.age,
                "city" to user.city,
                "street" to user.street,
                "houseNumber" to user.houseNumber,
                "phone" to user.phone
                // "avatarUrl" убран
            )
            usersCollection.document(user.userId).set(userMap, SetOptions.merge()).await()
            Log.i(TAG, "updateUser: User ${user.userId} updated successfully.")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "updateUser: Error updating user ${user.userId}", e)
            Result.failure(Exception("Ошибка обновления профиля: ${e.localizedMessage}", e))
        }
    }

    suspend fun addBook(book: Book, imageUri: Uri?): Result<String> = withContext(Dispatchers.IO) {
        // ... (без изменений, если Book модель не зависит от avatarUrl пользователя) ...
        val currentUser = auth.currentUser ?: return@withContext Result.failure(Exception("Пользователь не авторизован"))
        try {
            val newBookRef = booksCollection.document()
            val bookId = newBookRef.id
            val userProfile = getUserById(currentUser.uid).getOrNull()

            var finalBook = book.copy(
                id = bookId,
                userId = currentUser.uid,
                ownerNickname = userProfile?.nickname ?: "Неизвестно",
                timestamp = Date().time
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
        // ... (без изменений) ...
        try {
            val imageFileName = "${bookId}_${System.currentTimeMillis()}.jpg"
            val imageRef = storageRef.child(imageFileName)
            imageRef.putFile(imageUri).await()
            val downloadUrl = imageRef.downloadUrl.await()
            Result.success(downloadUrl.toString())
        } catch (e: Exception) { Result.failure(Exception("Ошибка загрузки/получения URL изображения: ${e.localizedMessage}", e)) }
    }

    suspend fun getAllBooks(): Result<List<Book>> = withContext(Dispatchers.IO) {
        // ... (без изменений) ...
        try {
            val querySnapshot = booksCollection.orderBy("timestamp", Query.Direction.DESCENDING).get().await()
            val books = querySnapshot.documents.mapNotNull { it.toObject<Book>() }
            Result.success(books)
        } catch (e: Exception) { Result.failure(Exception("Ошибка загрузки книг: ${e.localizedMessage}", e)) }
    }

    suspend fun getUserBooks(userId: String): Result<List<Book>> = withContext(Dispatchers.IO) {
        // ... (без изменений) ...
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
                currentUserProfile?.let {
                    // Убираем avatarUrl
                    participantInfoMap[it.userId] = mapOf("nickname" to it.nickname)
                }
                otherUserProfile?.let {
                    // Убираем avatarUrl
                    participantInfoMap[it.userId] = mapOf("nickname" to it.nickname)
                }

                val newChat = Chat(
                    chatId = generatedChatId,
                    participantIds = participants,
                    participantInfo = participantInfoMap,
                    lastMessageTimestamp = Date(),
                    unreadCount = mapOf(currentUserId to 0L, otherUserId to 0L)
                )
                chatRef.set(newChat).await()
                Log.d(TAG, "Chat $generatedChatId created successfully.")
                Result.success(generatedChatId)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting or creating chat for users $currentUserId, $otherUserId", e)
            Result.failure(e)
        }
    }

    fun getUserChatsFlow(userId: String): Flow<Result<List<Chat>>> = callbackFlow {
        // ... (без изменений в логике, кроме того что participantInfo не будет содержать avatarUrl) ...
        Log.d(TAG, "getUserChatsFlow: Setting up listener for user $userId")
        if (userId.isBlank()) {
            trySend(Result.failure(IllegalArgumentException("User ID is blank for chat flow")))
            close()
            return@callbackFlow
        }

        val query = chatsCollection
            .whereArrayContains("participantIds", userId)
            .orderBy("lastMessageTimestamp", Query.Direction.DESCENDING)

        val listenerRegistration = query.addSnapshotListener { snapshots, e ->
            if (e != null) {
                Log.e(TAG, "getUserChatsFlow: Listener error for user $userId", e)
                trySend(Result.failure(e))
                close(e)
                return@addSnapshotListener
            }
            if (snapshots != null) {
                val chats = snapshots.documents.mapNotNull { it.toObject<Chat>() }
                Log.d(TAG, "getUserChatsFlow: Received ${chats.size} chats for user $userId")
                trySend(Result.success(chats))
            } else {
                Log.d(TAG, "getUserChatsFlow: Snapshots are null for user $userId")
                trySend(Result.success(emptyList()))
            }
        }
        awaitClose {
            Log.d(TAG, "getUserChatsFlow: Listener removed for user $userId")
            listenerRegistration.remove()
        }
    }.flowOn(Dispatchers.IO)

    fun getChatMessagesFlow(chatId: String): Flow<Result<List<Message>>> = callbackFlow {
        // ... (без изменений) ...
        Log.d(TAG, "getChatMessagesFlow: Setting up listener for chat $chatId")
        if (chatId.isBlank()) {
            trySend(Result.failure(IllegalArgumentException("Chat ID is blank for messages flow")))
            close()
            return@callbackFlow
        }
        val currentUserId = auth.currentUser?.uid

        val query = chatsCollection.document(chatId)
            .collection(messagesSubCollection)
            .orderBy("timestamp", Query.Direction.ASCENDING)

        val listenerRegistration = query.addSnapshotListener { snapshots, e ->
            if (e != null) {
                Log.e(TAG, "getChatMessagesFlow: Listener error for chat $chatId", e)
                trySend(Result.failure(e))
                close(e)
                return@addSnapshotListener
            }
            if (snapshots != null) {
                val messages = snapshots.documents.mapNotNull { it.toObject<Message>() }
                Log.d(TAG, "getChatMessagesFlow: Received ${messages.size} messages for chat $chatId")
                trySend(Result.success(messages))
                if (currentUserId != null && messages.any { it.senderId != currentUserId }) {
                    chatsCollection.document(chatId)
                        .update("unreadCount.$currentUserId", 0)
                        .addOnSuccessListener { Log.d(TAG, "Unread count reset for $currentUserId in chat $chatId") }
                        .addOnFailureListener { Log.w(TAG, "Failed to reset unread count for $currentUserId in chat $chatId", it)}
                }

            } else {
                Log.d(TAG, "getChatMessagesFlow: Snapshots are null for chat $chatId")
                trySend(Result.success(emptyList()))
            }
        }
        awaitClose {
            Log.d(TAG, "getChatMessagesFlow: Listener removed for chat $chatId")
            listenerRegistration.remove()
        }
    }.flowOn(Dispatchers.IO)

    suspend fun sendMessage(chatId: String, message: Message): Result<Unit> = withContext(Dispatchers.IO) {
        // ... (без изменений) ...
        if (chatId.isBlank() || message.senderId.isBlank() || message.text.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("ChatId, senderId or message text is blank"))
        }
        try {
            val chatRef = chatsCollection.document(chatId)
            val messageRef = chatRef.collection(messagesSubCollection).document()
            val chatDoc = chatRef.get().await().toObject<Chat>()
            val receiverId = chatDoc?.participantIds?.firstOrNull { it != message.senderId }

            val messageToSend = message.copy(
                id = messageRef.id,
                chatId = chatId,
                timestamp = Date()
            )

            firestore.runBatch { batch ->
                batch.set(messageRef, messageToSend)
                val chatUpdates = hashMapOf<String, Any?>(
                    "lastMessageText" to messageToSend.text,
                    "lastMessageTimestamp" to FieldValue.serverTimestamp(),
                    "lastMessageSenderId" to messageToSend.senderId
                )
                if (receiverId != null) {
                    chatUpdates["unreadCount.$receiverId"] = FieldValue.increment(1)
                }
                batch.update(chatRef, chatUpdates)

            }.await()
            Log.d(TAG, "Message sent to chat $chatId by ${message.senderId}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending message to chat $chatId", e)
            Result.failure(e)
        }
    }

    // --- Остальные методы (Exchange, updateBook, getBookById) ---
    suspend fun recordExchangeAndUpdateBook(book: Book, newOwner: User): Result<Unit> = withContext(Dispatchers.IO) {
        Result.success(Unit) // Заглушка
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
    suspend fun updateBook(book: Book): Result<Unit> = withContext(Dispatchers.IO) {
        Result.success(Unit) // Заглушка
    }
    suspend fun getBookById(bookId: String): Result<Book> = withContext(Dispatchers.IO) {
        Result.failure(NotImplementedError()) // Заглушка
    }
}