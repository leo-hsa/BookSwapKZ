package com.example.bookswapkz.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.bookswapkz.models.Chat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _chats = MutableLiveData<List<Chat>>()
    val chats: LiveData<List<Chat>> = _chats

    private val _messages = MutableLiveData<List<Chat>>()
    val messages: LiveData<List<Chat>> = _messages

    fun loadChats() {
        val currentUserId = auth.currentUser?.uid ?: return
        
        firestore.collection("chats")
            .whereArrayContains("participants", currentUserId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    return@addSnapshotListener
                }

                val chatsList = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Chat::class.java)
                } ?: emptyList()

                _chats.value = chatsList
            }
    }

    fun loadMessages(otherUserId: String) {
        val currentUserId = auth.currentUser?.uid ?: return
        
        firestore.collection("messages")
            .whereEqualTo("senderId", currentUserId)
            .whereEqualTo("receiverId", otherUserId)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    return@addSnapshotListener
                }

                val messagesList = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Chat::class.java)
                } ?: emptyList()

                _messages.value = messagesList
            }
    }

    fun sendMessage(receiverId: String, message: String) {
        val currentUserId = auth.currentUser?.uid ?: return
        
        val chat = Chat(
            senderId = currentUserId,
            receiverId = receiverId,
            message = message
        )

        firestore.collection("messages")
            .add(chat)
            .addOnSuccessListener { documentRef ->
                // Update the chat with its ID
                documentRef.update("id", documentRef.id)
            }
    }

    fun markAsRead(chatId: String) {
        firestore.collection("messages")
            .document(chatId)
            .update("isRead", true)
    }
} 