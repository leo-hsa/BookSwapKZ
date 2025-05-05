package com.example.bookswapkz.models

import android.os.Parcelable
import com.google.firebase.firestore.DocumentId
import kotlinx.parcelize.Parcelize

@Parcelize
data class User(
    @DocumentId val userId: String = "",
    val nickname: String = "",
    val name: String = "",
    val email: String = "",
    val age: Int = 0,
    val city: String = "",
    val street: String = "",
    val houseNumber: String = "",
    val phone: String = "", // Оставим String, но можно String?
    val photoUrl: String? = null // Nullable
) : Parcelable {
    constructor() : this(userId = "", nickname = "", name = "", email = "", age = 0, city = "", street = "", houseNumber = "", phone = "", photoUrl = null)
}