package com.example.bookswapkz.models

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date // <-- Используем Date
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import com.google.firebase.firestore.DocumentId
import com.google.firebase.Timestamp // Импорт для совместимости, но используем Date

@Parcelize
data class Message(
    @DocumentId val id: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val text: String = "",
    @ServerTimestamp val timestamp: Date? = null // <-- Используем Date? и аннотацию
) : Parcelable {
    constructor() : this("", "", "", "", null) // Пустой конструктор с null для timestamp
}