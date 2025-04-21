package com.example.bookswapkz

import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.bookswapkz.models.Book
import com.example.bookswapkz.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage

class FirebaseRepository {
    private val auth = FirebaseAuth.getInstance()
    private val database = Firebase.database
    private val storage = Firebase.storage
    private val booksRef = database.getReference("Books")
    private val usersRef = database.getReference("Users")

    fun registerUser(nickname: String, name: String, city: String, age: Int, phone: String, email: String, password: String): LiveData<Boolean> {
        val result = MutableLiveData<Boolean>()
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val userId = auth.currentUser?.uid ?: return@addOnCompleteListener
                val user = User(userId, nickname, name, city, age, phone)
                usersRef.child(userId).setValue(user).addOnCompleteListener { saveTask ->
                    result.value = saveTask.isSuccessful
                    if (!saveTask.isSuccessful) {
                        Log.e("FirebaseRepository", "Failed to save user: ${saveTask.exception?.message}")
                    }
                }
            } else {
                Log.e("FirebaseRepository", "Registration failed: ${task.exception?.message}")
                result.value = false
            }
        }
        return result
    }

    fun getUser(): LiveData<User?> {
        val result = MutableLiveData<User?>()
        val userId = auth.currentUser?.uid ?: run {
            result.value = null
            return result
        }
        usersRef.child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                result.value = snapshot.getValue(User::class.java)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseRepository", "Get user failed: ${error.message}")
                result.value = null
            }
        })
        return result
    }

    fun loginUser(email: String, password: String): LiveData<Boolean> {
        val result = MutableLiveData<Boolean>()
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            result.value = task.isSuccessful
            if (!task.isSuccessful) {
                Log.e("FirebaseRepository", "Login failed: ${task.exception?.message}")
            }
        }
        return result
    }

    fun addBook(book: Book, imageUri: Uri?): LiveData<Boolean> {
        val result = MutableLiveData<Boolean>()
        val userId = auth.currentUser?.uid ?: run {
            result.value = false
            return result
        }
        val bookId = booksRef.push().key ?: run {
            result.value = false
            return result
        }
        val updatedBook = book.copy(id = bookId, userId = userId)

        if (imageUri == null) {
            saveBook(updatedBook, null, result)
        } else {
            val imageRef = storage.reference.child("images/$bookId.jpg")
            imageRef.putFile(imageUri).continueWithTask { task ->
                if (!task.isSuccessful) throw task.exception ?: Exception("Upload failed")
                imageRef.downloadUrl
            }.addOnCompleteListener { urlTask ->
                if (urlTask.isSuccessful) {
                    saveBook(updatedBook, urlTask.result.toString(), result)
                } else {
                    result.value = false
                }
            }
        }
        return result
    }

    private fun saveBook(book: Book, imageUrl: String?, result: MutableLiveData<Boolean>) {
        val bookToSave = book.copy(imageUrl = imageUrl)
        booksRef.child(book.id).setValue(bookToSave).addOnCompleteListener { task ->
            result.value = task.isSuccessful
        }
    }

    fun getAllBooks(): LiveData<List<Book>> {
        val result = MutableLiveData<List<Book>>()
        booksRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val books = mutableListOf<Book>()
                for (child in snapshot.children) {
                    val book = child.getValue(Book::class.java)
                    if (book != null) books.add(book)
                }
                result.value = books
            }

            override fun onCancelled(error: DatabaseError) {
                result.value = emptyList()
            }
        })
        return result
    }

    fun getUserBooks(userId: String): LiveData<List<Book>> {
        val result = MutableLiveData<List<Book>>()
        booksRef.orderByChild("userId").equalTo(userId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val books = mutableListOf<Book>()
                for (child in snapshot.children) {
                    val book = child.getValue(Book::class.java)
                    if (book != null) books.add(book)
                }
                result.value = books
            }

            override fun onCancelled(error: DatabaseError) {
                result.value = emptyList()
            }
        })
        return result
    }
}