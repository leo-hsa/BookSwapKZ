package com.example.bookswapkz.models

import android.os.Parcelable
import com.google.firebase.firestore.DocumentId
import kotlinx.parcelize.Parcelize

@Parcelize
data class User(
    @DocumentId var id: String = "", // Changed from userId to id and val to var
    val nickname: String = "",
    val name: String = "",
    val email: String = "",
    val age: Int = 0,
    val city: String = "",
    val street: String = "",
    val houseNumber: String = "",
    val phone: String = "",
    val photoUrl: String = ""
    // Добавьте другие поля, если нужно (например, profileImageUrl: String? = null)
) : Parcelable {
    // Пустой конструктор для Firestore
    constructor() : this("", "", "", "", 0, "", "", "", "", "")
}