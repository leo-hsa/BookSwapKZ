package com.example.bookswapkz.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class User(
    val userId: String = "",
    val nickname: String = "",
    val name: String = "",
    val email: String = "",
    val age: Int = 0,
    val city: String = "",
    val street: String = "",
    val houseNumber: String = "",
    val phone: String = ""

) : Parcelable