package com.example.bookswapkz.models

import android.os.Parcelable
import com.google.firebase.firestore.DocumentId
import kotlinx.parcelize.Parcelize

@Parcelize
data class User(
    @DocumentId val userId: String = "",
    val nickname: String = "", // Сделаем nullable, если может быть пустым при регистрации
    val name: String = "",     // Сделаем nullable
    val email: String = "",
    val age: Int = 0,          // Или Int?
    val city: String = "",     // Или String?
    val street: String = "",   // Или String?
    val houseNumber: String = "", // Или String?
    val phone: String = "",       // Или String?
    // avatarUrl убран
    val registrationDate: Long = System.currentTimeMillis() // Если это поле есть
) : Parcelable {
    // Пустой конструктор для Firestore
    constructor() : this(
        userId = "",
        nickname = "",
        name = "",
        email = "",
        age = 0,
        city = "",
        street = "",
        houseNumber = "",
        phone = ""
        // registrationDate не нужно здесь, т.к. есть значение по умолчанию
    )
}