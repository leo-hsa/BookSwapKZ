package com.example.bookswapkz

import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.bookswapkz.models.Book
import com.example.bookswapkz.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException // Импорт для проверки ошибки
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import java.util.Date

class FirebaseRepository {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val storage: FirebaseStorage = FirebaseStorage.getInstance() // Сначала Storage
    private val storageRef = storage.reference.child("book_images") // Затем используем storage
    private val usersCollection = firestore.collection("users") // Затем используем firestore
    private val booksCollection = firestore.collection("books") // Затем используем firestore

    private val TAG = "FirebaseRepository"

    var lastError: String? = null
        private set

    fun getUser(): LiveData<User?> {
        val userData = MutableLiveData<User?>()
        val firebaseUser: FirebaseUser? = auth.currentUser
        lastError = null

        if (firebaseUser == null) {
            Log.d(TAG, "getUser: No current user found.")
            userData.postValue(null)
        } else {
            Log.d(TAG, "getUser: Fetching user data for UID: ${firebaseUser.uid}")
            usersCollection.document(firebaseUser.uid).get()
                .addOnSuccessListener { documentSnapshot ->
                    if (documentSnapshot.exists()) {
                        try {
                            val user = documentSnapshot.toObject(User::class.java)?.copy(userId = documentSnapshot.id)
                            userData.postValue(user)
                            Log.d(TAG, "getUser: User data fetched successfully: ${user?.nickname}")
                        } catch (e: Exception) {
                            Log.e(TAG, "getUser: Error converting document to User object for UID ${firebaseUser.uid}", e)
                            lastError = "Ошибка чтения данных профиля: ${e.localizedMessage}"
                            userData.postValue(null)
                        }
                    } else {
                        Log.w(TAG, "getUser: User document does not exist for UID: ${firebaseUser.uid}")
                        userData.postValue(null)
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "getUser: Error getting user document for UID ${firebaseUser.uid}", e)
                    lastError = "Ошибка загрузки профиля: ${e.localizedMessage}"
                    userData.postValue(null)
                }
        }
        return userData
    }

    fun getAllBooks(): LiveData<List<Book>> {
        val booksData = MutableLiveData<List<Book>>()
        lastError = null
        Log.d(TAG, "getAllBooks: Setting up listener for all books...")

        booksCollection
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w(TAG, "getAllBooks: Error listening for all books updates.", e)
                    lastError = "Ошибка загрузки книг: ${e.localizedMessage}"
                    booksData.postValue(emptyList())
                    return@addSnapshotListener
                }

                val booksList = mutableListOf<Book>()
                if (snapshots != null) {
                    for (doc in snapshots) {
                        try {
                            val book = doc.toObject(Book::class.java).copy(id = doc.id)
                            booksList.add(book)
                        } catch (ex: Exception) {
                            Log.e(TAG, "getAllBooks: Error converting document ${doc.id} to Book", ex)
                        }
                    }
                }
                Log.d(TAG, "getAllBooks: Listener updated, ${booksList.size} books found.")
                booksData.postValue(booksList)
            }
        return booksData
    }

    fun getUserBooks(userId: String): LiveData<List<Book>> {
        val booksData = MutableLiveData<List<Book>>()
        lastError = null

        if (userId.isBlank()) {
            Log.w(TAG, "getUserBooks: Called with blank userId.")
            booksData.postValue(emptyList())
            return booksData
        }

        Log.d(TAG, "getUserBooks: Setting up listener for books of user ID: $userId")
        booksCollection.whereEqualTo("userId", userId)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w(TAG, "getUserBooks: Error listening for user books updates (User: $userId).", e)
                    lastError = "Ошибка загрузки ваших книг: ${e.localizedMessage}"
                    booksData.postValue(emptyList())
                    return@addSnapshotListener
                }

                val booksList = mutableListOf<Book>()
                if (snapshots != null) {
                    for (doc in snapshots) {
                        try {
                            val book = doc.toObject(Book::class.java).copy(id = doc.id)
                            booksList.add(book)
                        } catch (ex: Exception) {
                            Log.e(TAG, "getUserBooks: Error converting document ${doc.id} to Book for user $userId", ex)
                        }
                    }
                }
                Log.d(TAG, "getUserBooks: Listener updated, ${booksList.size} books found for user $userId.")
                booksData.postValue(booksList)
            }
        return booksData
    }


    fun addBook(book: Book, imageUri: Uri?): LiveData<Boolean> { // imageUri больше не используется, но оставим для совместимости с ViewModel
        val result = MutableLiveData<Boolean>()
        lastError = null
        val currentUser = auth.currentUser

        if (currentUser == null) {
            Log.e(TAG, "addBook failed: User not authenticated")
            lastError = "Пользователь не авторизован"
            result.postValue(false)
            return result
        }

        // Убеждаемся, что imageUrl нет в сохраняемом объекте
        val bookWithUserId = book.copy(userId = currentUser.uid /*, imageUrl = null */) // Убеждаемся, что imageUrl сброшен, если он был в модели
        val newBookRef = booksCollection.document()
        val bookId = newBookRef.id
        val bookToSave = bookWithUserId.copy(id = bookId, ownerCount = 1)
        Log.d(TAG, "addBook: Generated book ID: $bookId for user ${currentUser.uid}")

        // Сразу сохраняем книгу без изображения
        Log.d(TAG, "addBook: Saving book data directly (no image)...")
        saveBookData(newBookRef, bookToSave, result)

        return result
    }

    private fun saveBookData(docRef: com.google.firebase.firestore.DocumentReference, book: Book, resultLiveData: MutableLiveData<Boolean>) {
        Log.d(TAG, "saveBookData: Attempting to save book ID ${book.id} to Firestore path ${docRef.path}")
        docRef.set(book)
            .addOnSuccessListener {
                Log.i(TAG, "saveBookData: Book data saved successfully for book ID: ${book.id}")
                lastError = null
                resultLiveData.postValue(true)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "saveBookData: Error saving book data for book ID ${book.id}", e)
                lastError = "Ошибка сохранения данных книги: ${e.localizedMessage}"
                resultLiveData.postValue(false)
            }
    }

    fun registerUser(
        nickname: String, name: String, city: String, street: String, houseNumber: String,
        age: Int, phone: String, email: String, password: String
    ): LiveData<Boolean> {
        val result = MutableLiveData<Boolean>()
        lastError = null

        Log.d(TAG, "registerUser: Attempting registration for email: $email")
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                val firebaseUser = authResult.user
                if (firebaseUser != null) {
                    Log.d(TAG, "registerUser: User created in Auth successfully: ${firebaseUser.uid}")
                    val userMap = hashMapOf(
                        "nickname" to nickname,
                        "name" to name,
                        "email" to email.lowercase(),
                        "age" to age,
                        "city" to city,
                        "street" to street,
                        "houseNumber" to houseNumber,
                        "phone" to phone
                    )
                    usersCollection.document(firebaseUser.uid).set(userMap)
                        .addOnSuccessListener {
                            Log.d(TAG, "registerUser: User data saved to Firestore for UID: ${firebaseUser.uid}")
                            result.postValue(true)
                        }.addOnFailureListener { e ->
                            Log.e(TAG, "registerUser: Error saving user data to Firestore for UID ${firebaseUser.uid}", e)
                            lastError = "Ошибка сохранения профиля: ${e.localizedMessage}"
                            result.postValue(false)
                        }
                } else {
                    Log.e(TAG, "registerUser: FirebaseUser is null after successful Auth creation!")
                    lastError = "Не удалось получить данные пользователя после создания."
                    result.postValue(false)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "registerUser: Error creating user in Auth", e)
                if (e is FirebaseAuthUserCollisionException) {
                    lastError = "Этот Email уже зарегистрирован."
                } else {
                    lastError = "Ошибка регистрации: ${e.localizedMessage}"
                }
                result.postValue(false)
            }
        return result
    }

    fun loginUser(email: String, password: String): LiveData<Boolean> {
        val result = MutableLiveData<Boolean>()
        lastError = null
        Log.d(TAG, "loginUser: Attempting login for email: $email")

        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                Log.i(TAG, "loginUser: signInWithEmail:success for user ${it.user?.uid}")
                result.postValue(true)
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "loginUser: signInWithEmail:failure", e)
                lastError = "Ошибка входа: Неверный email или пароль. (${e.javaClass.simpleName})"
                result.postValue(false)
            }
        return result
    }


    fun recordExchange(bookId: String, newOwnerUserId: String, newOwnerPhone: String): LiveData<Boolean> {
        val result = MutableLiveData<Boolean>()
        lastError = null
        Log.d(TAG, "recordExchange: Attempting for book $bookId to user $newOwnerUserId")

        if (bookId.isBlank() || newOwnerUserId.isBlank()) {
            Log.e(TAG, "recordExchange: Invalid input (blank bookId or userId)")
            lastError = "Внутренняя ошибка (пустой ID)"
            result.postValue(false)
            return result
        }

        val bookRef = booksCollection.document(bookId)

        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(bookRef)

            if (!snapshot.exists()) {
                Log.e(TAG, "recordExchange: Book document $bookId not found during transaction.")
                throw FirebaseFirestoreException("Книга не найдена!", FirebaseFirestoreException.Code.NOT_FOUND)
            }

            val currentOwnerCount = snapshot.getLong("ownerCount") ?: 0
            val newOwnerCountValue = currentOwnerCount + 1

            val updates = mapOf(
                "userId" to newOwnerUserId,
                "phone" to newOwnerPhone,
                "ownerCount" to newOwnerCountValue
            )
            Log.d(TAG,"recordExchange: Updating book $bookId with: $updates")
            transaction.update(bookRef, updates)
            null
        }.addOnSuccessListener {
            Log.i(TAG, "recordExchange: Transaction success for book $bookId")
            result.postValue(true)
        }.addOnFailureListener { e ->
            Log.e(TAG, "recordExchange: Transaction failure for book $bookId", e)
            if (e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.NOT_FOUND) {
                lastError = "Книга не найдена (ошибка транзакции)."
            } else {
                lastError = "Ошибка записи обмена: ${e.localizedMessage}"
            }
            result.postValue(false)
        }

        return result
    }

    fun getRentableBooks(paidOnly: Boolean? = null): LiveData<List<Book>> {
        val booksData = MutableLiveData<List<Book>>()
        lastError = null
        Log.d(TAG, "getRentableBooks: Setting up listener for rentable books...")

        var query = booksCollection.whereEqualTo("isForRent", true)
            .whereEqualTo("isRented", false)

        if (paidOnly != null) {
            if (paidOnly) {
                query = query.whereGreaterThan("rentPrice", 0.0)
            } else {
                query = query.whereEqualTo("rentPrice", null)
            }
        }

        query.addSnapshotListener { snapshots, e ->
            if (e != null) {
                Log.w(TAG, "getRentableBooks: Error listening for rentable books updates.", e)
                lastError = "Ошибка загрузки книг для аренды: ${e.localizedMessage}"
                booksData.postValue(emptyList())
                return@addSnapshotListener
            }

            val booksList = mutableListOf<Book>()
            if (snapshots != null) {
                for (doc in snapshots) {
                    try {
                        val book = doc.toObject(Book::class.java).copy(id = doc.id)
                        booksList.add(book)
                    } catch (ex: Exception) {
                        Log.e(TAG, "getRentableBooks: Error converting document ${doc.id} to Book", ex)
                    }
                }
            }
            Log.d(TAG, "getRentableBooks: Listener updated, ${booksList.size} rentable books found.")
            booksData.postValue(booksList)
        }
        return booksData
    }

    fun rentBook(bookId: String, rentPeriod: String): LiveData<Boolean> {
        val result = MutableLiveData<Boolean>()
        lastError = null

        val currentUser = auth.currentUser
        if (currentUser == null) {
            lastError = "Пользователь не авторизован"
            result.postValue(false)
            return result
        }

        val bookRef = booksCollection.document(bookId)
        val updates = hashMapOf<String, Any>(
            "isRented" to true,
            "rentedToUserId" to currentUser.uid,
            "rentStartDate" to Date().toString(),
            "rentEndDate" to calculateEndDate(rentPeriod)
        )

        bookRef.update(updates)
            .addOnSuccessListener {
                Log.d(TAG, "rentBook: Book $bookId rented successfully")
                result.postValue(true)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "rentBook: Error renting book $bookId", e)
                lastError = "Ошибка при аренде книги: ${e.localizedMessage}"
                result.postValue(false)
            }

        return result
    }

    fun returnBook(bookId: String): LiveData<Boolean> {
        val result = MutableLiveData<Boolean>()
        lastError = null

        val bookRef = booksCollection.document(bookId)
        val updates = hashMapOf<String, Any>(
            "isRented" to false,
            "rentedToUserId" to FieldValue.delete(),
            "rentStartDate" to FieldValue.delete(),
            "rentEndDate" to FieldValue.delete()
        )

        bookRef.update(updates)
            .addOnSuccessListener {
                Log.d(TAG, "returnBook: Book $bookId returned successfully")
                result.postValue(true)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "returnBook: Error returning book $bookId", e)
                lastError = "Ошибка при возврате книги: ${e.localizedMessage}"
                result.postValue(false)
            }

        return result
    }

    private fun calculateEndDate(rentPeriod: String): String {
        val currentDate = Date()
        val calendar = java.util.Calendar.getInstance()
        calendar.time = currentDate

        when (rentPeriod) {
            "1 неделя" -> calendar.add(java.util.Calendar.WEEK_OF_YEAR, 1)
            "2 недели" -> calendar.add(java.util.Calendar.WEEK_OF_YEAR, 2)
            "1 месяц" -> calendar.add(java.util.Calendar.MONTH, 1)
            else -> calendar.add(java.util.Calendar.WEEK_OF_YEAR, 1) // Default to 1 week
        }

        return calendar.time.toString()
    }
}