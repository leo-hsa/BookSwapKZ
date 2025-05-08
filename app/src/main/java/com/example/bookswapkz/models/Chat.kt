package com.example.bookswapkz.models

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
// import com.google.firebase.Timestamp // Этот импорт не нужен, если используем java.util.Date

@Parcelize
data class Chat(
    @DocumentId val chatId: String = "", // Будет заполнен Firestore при чтении
    val participantIds: List<String> = emptyList(),
    val participantInfo: Map<String, Map<String, String?>> = emptyMap(), // userId -> {"nickname": "...", "avatarUrl": "..."}
    val lastMessageText: String? = null,
    @ServerTimestamp val lastMessageTimestamp: Date? = null, // Используем Date? + @ServerTimestamp
    val lastMessageSenderId: String? = null,
    val unreadCount: Map<String, Long> = emptyMap() // userId -> count
) : Parcelable {
    // Пустой конструктор для Firestore
    constructor() : this("", emptyList(), emptyMap(), null, null, null, emptyMap())
}