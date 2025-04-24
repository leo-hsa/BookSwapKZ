package com.example.bookswapkz.data

import android.net.Uri
import com.example.bookswapkz.models.Book
import com.example.bookswapkz.models.Exchange
import com.example.bookswapkz.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val storage: FirebaseStorage
) {
    // User collection reference
    private val usersCollection = firestore.collection("users")
    
    // Books collection reference
    private val booksCollection = firestore.collection("books")
    
    // Exchanges collection reference
    private val exchangesCollection = firestore.collection("exchanges")
    
    // Storage reference for book images
    private val bookImagesRef: StorageReference = storage.reference.child("book_images")
    
    // Get current user ID
    val currentUserId: String?
        get() = auth.currentUser?.uid
    
    // Check if user is authenticated
    val isUserAuthenticated: Boolean
        get() = auth.currentUser != null

    // User Related Operations
    
    /**
     * Creates a new user in Firestore
     */
    suspend fun createUser(user: User): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            usersCollection.document(user.id).set(user).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get user by ID
     */
    suspend fun getUserById(userId: String): Result<User> = withContext(Dispatchers.IO) {
        try {
            val document = usersCollection.document(userId).get().await()
            if (document.exists()) {
                val user = document.toObject(User::class.java)
                user?.let {
                    Result.success(it)
                } ?: Result.failure(Exception("Failed to convert document to User"))
            } else {
                Result.failure(Exception("User not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get current user
     */
    suspend fun getCurrentUser(): Result<User> = withContext(Dispatchers.IO) {
        currentUserId?.let { userId ->
            getUserById(userId)
        } ?: Result.failure(Exception("User not authenticated"))
    }
    
    /**
     * Update user profile
     */
    suspend fun updateUserProfile(user: User): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            usersCollection.document(user.id).set(user).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Check if nickname is already taken
     */
    suspend fun isNicknameTaken(nickname: String, userId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val querySnapshot = usersCollection.whereEqualTo("nickname", nickname).get().await()
            val isTaken = querySnapshot.documents.any { it.id != userId }
            Result.success(isTaken)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Book Related Operations
    
    /**
     * Upload book image to Firebase Storage
     */
    suspend fun uploadBookImage(imageUri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            val filename = UUID.randomUUID().toString()
            val imageRef = bookImagesRef.child(filename)
            
            val uploadTask = imageRef.putFile(imageUri).await()
            val downloadUrl = imageRef.downloadUrl.await()
            
            Result.success(downloadUrl.toString())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Add a new book to Firestore
     */
    suspend fun addBook(book: Book): Result<String> = withContext(Dispatchers.IO) {
        try {
            val document = booksCollection.add(book).await()
            Result.success(document.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get all books
     */
    suspend fun getAllBooks(): Result<List<Book>> = withContext(Dispatchers.IO) {
        try {
            val querySnapshot = booksCollection.get().await()
            val books = querySnapshot.documents.mapNotNull { doc -> 
                doc.toObject(Book::class.java)?.also { book -> book.id = doc.id }
            }
            Result.success(books)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get book by ID
     */
    suspend fun getBookById(bookId: String): Result<Book> = withContext(Dispatchers.IO) {
        try {
            val document = booksCollection.document(bookId).get().await()
            if (document.exists()) {
                val book = document.toObject(Book::class.java)?.also { it.id = document.id }
                book?.let {
                    Result.success(it)
                } ?: Result.failure(Exception("Failed to convert document to Book"))
            } else {
                Result.failure(Exception("Book not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get books by owner ID
     */
    suspend fun getBooksByOwnerId(ownerId: String): Result<List<Book>> = withContext(Dispatchers.IO) {
        try {
            val querySnapshot = booksCollection.whereEqualTo("userId", ownerId).get().await()
            val books = querySnapshot.documents.mapNotNull { doc -> 
                doc.toObject(Book::class.java)?.also { book -> book.id = doc.id }
            }
            Result.success(books)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Update book
     */
    suspend fun updateBook(book: Book): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            book.id?.let { bookId ->
                booksCollection.document(bookId).set(book).await()
                Result.success(Unit)
            } ?: Result.failure(Exception("Book ID is null"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Delete book
     */
    suspend fun deleteBook(bookId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            booksCollection.document(bookId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Authentication Related Operations

    /**
     * Login user with email and password
     */
    suspend fun loginUser(email: String, password: String): Result<com.google.firebase.auth.FirebaseUser> = withContext(Dispatchers.IO) {
        try {
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user
            if (firebaseUser != null) {
                Result.success(firebaseUser)
            } else {
                Result.failure(Exception("Authentication failed, user is null"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Register a new user with email and password
     */
    suspend fun registerUser(user: User, password: String): Result<com.google.firebase.auth.FirebaseUser> = withContext(Dispatchers.IO) {
        try {
            val authResult = auth.createUserWithEmailAndPassword(user.email, password).await()
            val firebaseUser = authResult.user
            
            if (firebaseUser != null) {
                // Create the user document in Firestore with the UID from Firebase Auth
                val userWithId = user.copy(id = firebaseUser.uid)
                createUser(userWithId).onFailure { error ->
                    return@withContext Result.failure(error)
                }
                
                Result.success(firebaseUser)
            } else {
                Result.failure(Exception("Registration failed, user is null"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Exchange Related Operations
    
    /**
     * Create a new exchange request
     */
    suspend fun createExchange(exchange: Exchange): Result<String> = withContext(Dispatchers.IO) {
        try {
            val document = exchangesCollection.add(exchange).await()
            Result.success(document.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get exchange by ID
     */
    suspend fun getExchangeById(exchangeId: String): Result<Exchange> = withContext(Dispatchers.IO) {
        try {
            val document = exchangesCollection.document(exchangeId).get().await()
            if (document.exists()) {
                val exchange = document.toObject(Exchange::class.java)?.also { it.id = document.id }
                exchange?.let {
                    Result.success(it)
                } ?: Result.failure(Exception("Failed to convert document to Exchange"))
            } else {
                Result.failure(Exception("Exchange not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get exchanges by user ID (as requester or owner)
     */
    suspend fun getExchangesByUserId(userId: String): Result<List<Exchange>> = withContext(Dispatchers.IO) {
        try {
            val asRequesterSnapshot = exchangesCollection.whereEqualTo("requesterId", userId).get().await()
            val asOwnerSnapshot = exchangesCollection.whereEqualTo("ownerId", userId).get().await()
            
            val exchanges = mutableListOf<Exchange>()
            
            asRequesterSnapshot.documents.mapNotNullTo(exchanges) { doc -> 
                doc.toObject(Exchange::class.java)?.also { it.id = doc.id }
            }
            
            asOwnerSnapshot.documents.mapNotNullTo(exchanges) { doc -> 
                doc.toObject(Exchange::class.java)?.also { it.id = doc.id }
            }
            
            Result.success(exchanges.distinctBy { it.id })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Update exchange status
     */
    suspend fun updateExchangeStatus(exchangeId: String, status: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            exchangesCollection.document(exchangeId).update("status", status).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
} 