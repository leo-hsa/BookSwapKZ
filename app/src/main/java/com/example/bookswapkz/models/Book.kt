package com.example.bookswapkz.models

import android.os.Parcelable
import com.google.firebase.firestore.DocumentId
import kotlinx.parcelize.Parcelize

@Parcelize
data class Book(
    @DocumentId val id: String = "",
    val title: String = "",
    val author: String = "",
    val description: String = "",
    val imageUrl: String? = null,
    val userId: String = "", // ID текущего владельца
    val ownerNickname: String? = null, // Никнейм владельца
    val city: String = "",
    val phone: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isForRent: Boolean = false,
    val rentPrice: Double? = null,
    val condition: String = "",
    val rentPeriod: String? = null,
    val isRented: Boolean = false,
    val isExchanged: Boolean = false, // Добавляем новое свойство для отслеживания обмененных книг
    val ownerCount: Int = 1,

    val searchableTitle: String = title.lowercase(),
    val searchableAuthor: String = author.lowercase(),
    val searchableCity: String = city.lowercase(),
    val searchableNickname: String = ownerNickname?.lowercase() ?: ""

) : Parcelable {
    constructor() : this(
        id = "", title = "", author = "", description = "", imageUrl = null, userId = "",
        ownerNickname = null, city = "", phone = null, timestamp = 0L, isForRent = false,
        rentPrice = null, condition = "", rentPeriod = null, isRented = false, isExchanged = false,
        ownerCount = 1, searchableTitle = "", searchableAuthor = "", searchableCity = "", 
        searchableNickname = ""
    )
}

fun Book.prepareForSave(): Book {
    return this.copy(
        searchableTitle = this.title.lowercase(),
        searchableAuthor = this.author.lowercase(),
        searchableCity = this.city.lowercase(),
        searchableNickname = this.ownerNickname?.lowercase() ?: ""
    )
}