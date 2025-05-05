package com.example.bookswapkz.models

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import com.google.firebase.Timestamp // <-- ИМПОРТ Timestamp для Firestore

@Parcelize
data class Chat(
    @DocumentId val chatId: String = "",
    val participantIds: List<String> = emptyList(),
    val participantInfo: Map<String, Map<String, String?>> = emptyMap(), // userId -> {"nickname": "...", "photoUrl": "..."}
    val lastMessageText: String? = null,
    // Используем Date? + @ServerTimestamp для единообразия с Exchange
    @ServerTimestamp val lastMessageTimestamp: Date? = null,
    val lastMessageSenderId: String? = null,
    val unreadCount: Map<String, Long> = emptyMap() // userId -> count
) : Parcelable {
    constructor() : this("", emptyList(), emptyMap(), null, null, null, emptyMap())
}