package com.example.bookswapkz.models

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date
import android.os.Parcelable // Добавляем Parcelable
import kotlinx.parcelize.Parcelize // Добавляем Parcelize
import com.google.firebase.firestore.DocumentId

@Parcelize // Делаем Parcelable для возможной передачи
data class Exchange(
    @DocumentId var id: String = "",
    val bookId: String = "",
    val oldOwnerId: String = "",
    val newOwnerId: String = "",
    val status: String = "pending", // pending, accepted, rejected, completed
    @ServerTimestamp
    val exchangeDate: Date? = null,
    @ServerTimestamp
    val createdAt: Date? = null,
    @ServerTimestamp
    val updatedAt: Date? = null,
    val bookTitle: String = "",
    val oldOwnerName: String = "",
    val newOwnerName: String = ""
) : Parcelable { // Наследуем Parcelable
    // Пустой конструктор для Firestore
    constructor() : this("", "", "", "", "pending", null, null, null, "", "", "")
}