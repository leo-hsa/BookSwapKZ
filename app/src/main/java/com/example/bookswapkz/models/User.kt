package com.example.bookswapkz.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class User(
    val userId: String = "",
    val nickname: String = "",
    val name: String = "",
    val email: String = "", // Добавил email, т.к. он нужен для регистрации/входа
    val age: Int = 0,
    val city: String = "",
    val street: String = "", // <-- ДОБАВЛЕНО
    val houseNumber: String = "", // <-- ДОБАВЛЕНО
    val phone: String = ""
    // Добавьте другие поля, если они нужны, например, ссылка на фото профиля
) : Parcelable