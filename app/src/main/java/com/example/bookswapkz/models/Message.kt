package com.example.bookswapkz.models

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date // <-- Используем Date
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import com.google.firebase.firestore.DocumentId
// import com.google.firebase.Timestamp // Этот импорт не нужен, если используем java.util.Date

@Parcelize
data class Message(
    @DocumentId val id: String = "", // Будет заполнен Firestore при чтении
    val chatId: String = "",
    val senderId: String = "",
    val text: String = "",
    @ServerTimestamp val timestamp: Date? = null // Используем Date? и аннотацию
) : Parcelable {
    // Пустой конструктор для Firestore (если поля nullable или имеют значения по умолчанию)
    constructor() : this("", "", "", "", null)
}