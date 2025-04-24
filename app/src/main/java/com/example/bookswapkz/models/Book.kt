package com.example.bookswapkz.models

import android.os.Parcelable
import com.google.firebase.firestore.DocumentId
import kotlinx.parcelize.Parcelize

@Parcelize
data class Book(
    @DocumentId var id: String = "", // Changed from val to var
    val title: String = "",
    val author: String = "",
    val description: String = "", // Описание
    val imageUrl: String? = null, // URL картинки (nullable)
    val userId: String = "", // ID текущего владельца
    val ownerNickname: String? = null, // Никнейм владельца (nullable)
    val city: String = "",
    val phone: String? = null, // Телефон владельца (nullable)
    val timestamp: Long = System.currentTimeMillis(), // Время добавления/обновления
    val isForRent: Boolean = false, // Для фильтра аренды
    val rentPrice: Double? = null, // Стоимость за час аренды
    val rentDurationHours: Int? = null, // Длительность аренды в часах
    val rentTotalPrice: Double? = null, // Общая стоимость аренды
    val condition: String = "", // Состояние книги
    val rentPeriod: String? = null, // Срок аренды (опционально)
    val isRented: Boolean = false, // Сдана ли в аренду сейчас (опционально)
    val ownerCount: Int = 1, // Счетчик владельцев (Int, по умолчанию 1)
    val ownerName: String? = null, // Owner name for display

    // Поля для поиска (обновляются перед сохранением)
    val searchableTitle: String = title.lowercase(),
    val searchableAuthor: String = author.lowercase(),
    val searchableCity: String = city.lowercase(),
    val searchableNickname: String = ownerNickname?.lowercase() ?: ""

) : Parcelable {
    // Пустой конструктор обязателен для Firestore
    constructor() : this(
        id = "", title = "", author = "", description = "", imageUrl = null, userId = "",
        ownerNickname = null, city = "", phone = null, timestamp = 0L, isForRent = false,
        rentPrice = null, rentDurationHours = null, rentTotalPrice = null, condition = "", 
        rentPeriod = null, isRented = false, ownerCount = 1,
        ownerName = null, searchableTitle = "", searchableAuthor = "", searchableCity = "", 
        searchableNickname = ""
    )
}

// Функция для обновления поисковых полей ПЕРЕД сохранением в Firestore
fun Book.prepareForSave(): Book {
    return this.copy(
        searchableTitle = this.title.lowercase(),
        searchableAuthor = this.author.lowercase(),
        searchableCity = this.city.lowercase(),
        searchableNickname = this.ownerNickname?.lowercase() ?: ""
    )
}

