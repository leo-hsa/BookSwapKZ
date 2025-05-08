package com.example.bookswapkz.models

import android.os.Parcelable
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import kotlinx.parcelize.Parcelize
import java.util.Date

@Parcelize
data class Exchange(
    @DocumentId val id: String = "",
    val requesterId: String = "",           // Кто запросил (Пользователь А)
    val requesterNickname: String? = null,  // Имя того, кто запросил (для отображения)
    val requestedOwnerId: String = "",      // Владелец книги (Пользователь Б)
    val requestedBookId: String = "",       // ID книги, которую запросили (Book_B)
    val requestedBookTitle: String? = null, // Название книги (для отображения)
    val requestedBookImageUrl: String? = null,// Обложка книги (для отображения)
    var status: String = ExchangeStatus.PENDING.name, // "PENDING", "ACCEPTED", "REJECTED", "COMPLETED"
    @ServerTimestamp val createdAt: Date? = null,
    var processedAt: Date? = null // Время принятия/отклонения
) : Parcelable {
    // Пустой конструктор для Firestore
    constructor() : this(
        "", "", null, "", "", null, null,
        ExchangeStatus.PENDING.name, null, null
    )
}

// Enum для статусов (можно оставить как есть или упростить до PENDING, ACCEPTED, REJECTED)
enum class ExchangeStatus {
    PENDING,  // Ожидает
    ACCEPTED, // Принят (означает, что книга передана)
    REJECTED  // Отклонен
}