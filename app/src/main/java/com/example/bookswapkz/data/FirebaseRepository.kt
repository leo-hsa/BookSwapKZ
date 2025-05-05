package com.example.bookswapkz.data

import android.net.Uri
import android.util.Log
import com.example.bookswapkz.models.Book
import com.example.bookswapkz.models.Chat
import com.example.bookswapkz.models.Exchange
import com.example.bookswapkz.models.Message
import com.example.bookswapkz.models.User
import com.example.bookswapkz.models.prepareForSave
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.Result

@Singleton
class FirebaseRepository @Inject constructor() {

    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
    private val storageRef = storage.reference.child("book_images")
    private val usersCollection = firestore.collection("users")
    private val booksCollection = firestore.collection("books")
    private val exchangesCollection = firestore.collection("exchanges")
    private val chatsCollection = firestore.collection("chats")

    private val TAG = "FirebaseRepository"

    suspend fun getUserById(userId: String): Result<User> = withContext(Dispatchers.IO) {
        if (userId.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("User ID cannot be blank"))
        }
        try {
            Log.d(TAG, "getUserById: Fetching user $userId")
            val document = usersCollection.document(userId).get().await()
            if (document.exists()) {
                val user = document.toObject(User::class.java)?.copy(userId = document.id)
                if (user != null) {
                    Log.d(TAG, "getUserById: User $userId fetched successfully")
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
                val userMap = hashMapOf(
                    "nickname" to nickname, "name" to name, "email" to email.lowercase(),
                    "age" to age, "city" to city, "street" to street,
                    "houseNumber" to houseNumber, "phone" to phone
                )
                usersCollection.document(firebaseUser.uid).set(userMap).await()
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
            Log.d(TAG, "updateUser: Updating user ${user.userId}")
            val userMap = mapOf(
                "nickname" to user.nickname, "name" to user.name, "email" to user.email.lowercase(),
                "age" to user.age, "city" to user.city, "street" to user.street,
                "houseNumber" to user.houseNumber, "phone" to user.phone, "photoUrl" to user.photoUrl
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
        val currentUser = auth.currentUser ?: return@withContext Result.failure(Exception("Пользователь не авторизован"))
        try {
            val newBookRef = booksCollection.document()
            val bookId = newBookRef.id
            val userResult = getCurrentUser().getOrNull()

            var finalBook = book.copy(
                id = bookId,
                userId = currentUser.uid,
                ownerNickname = userResult?.nickname,
                ownerCount = 1,
                timestamp = System.currentTimeMillis()
            )

            if (imageUri != null) {
                val imageUrl = uploadBookImage(bookId, imageUri).getOrThrow()
                finalBook = finalBook.copy(imageUrl = imageUrl)
            }

            newBookRef.set(finalBook.prepareForSave()).await()
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
            val books = querySnapshot.documents.mapNotNull { it.toObject(Book::class.java)?.copy(id = it.id) }
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

            Log.d(TAG, "getUserBooks: Awaiting query results")
            val querySnapshot = query.get().await()

            Log.d(TAG, "getUserBooks: Got ${querySnapshot.size()} documents")
            val books = querySnapshot.documents.mapNotNull { doc ->
                try {
                    doc.toObject(Book::class.java)?.copy(id = doc.id).also { book ->
                        Log.d(TAG, "getUserBooks: Mapped document ${doc.id} to book ${book?.title}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "getUserBooks: Error mapping document ${doc.id}", e)
                    null
                }
            }
            Log.d(TAG, "getUserBooks: Successfully mapped ${books.size} books")
            Result.success(books)
        } catch (e: Exception) {
            Log.e(TAG, "getUserBooks: Failed to fetch books", e)
            Result.failure(Exception("Ошибка загрузки книг пользователя: ${e.localizedMessage}", e))
        }
    }

    suspend fun updateBook(book: Book): Result<Unit> = withContext(Dispatchers.IO) {
        if (book.id.isBlank()) return@withContext Result.failure(IllegalArgumentException("Book ID blank"))
        try {
            booksCollection.document(book.id).set(book.prepareForSave(), SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(Exception("Ошибка обновления книги: ${e.localizedMessage}", e)) }
    }

    suspend fun getBookById(bookId: String): Result<Book> = withContext(Dispatchers.IO) {
        if (bookId.isBlank()) return@withContext Result.failure(IllegalArgumentException("Book ID blank"))
        try {
            val document = booksCollection.document(bookId).get().await()
            if (document.exists()) {
                document.toObject(Book::class.java)?.copy(id = document.id)?.let { Result.success(it) }
                    ?: Result.failure(Exception("Failed to parse book: $bookId"))
            } else { Result.failure(Exception("Book not found: $bookId")) }
        } catch (e: Exception) { Result.failure(Exception("Error fetching book $bookId: ${e.localizedMessage}", e)) }
    }

    suspend fun recordExchangeAndUpdateBook(book: Book, newOwner: User): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserGivenHistory(userId: String): Result<List<Exchange>> = withContext(Dispatchers.IO) {
        try {
            Result.success(emptyList())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserReceivedHistory(userId: String): Result<List<Exchange>> = withContext(Dispatchers.IO) {
        try {
            Result.success(emptyList())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserExchangeHistory(userId: String): Result<Pair<List<Exchange>, List<Exchange>>> = withContext(Dispatchers.IO) {
        try {
            Result.success(Pair(emptyList(), emptyList()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getOrCreateChat(user1Id: String, user2Id: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            Result.success("")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getUserChatsFlow(userId: String): Flow<Result<List<Chat>>> = callbackFlow { /* ... как ранее ... */ }
    fun getChatMessagesFlow(chatId: String): Flow<Result<List<Message>>> = callbackFlow { /* ... как ранее ... */ }
    suspend fun sendMessage(chatId: String, message: Message): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val messageMap = mapOf(
                "text" to message.text,
                "senderId" to message.senderId,
                "timestamp" to message.timestamp
            )
            chatsCollection.document(chatId)
                .collection("messages")
                .add(messageMap)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}