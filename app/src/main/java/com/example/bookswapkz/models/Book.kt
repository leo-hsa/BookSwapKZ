package com.example.bookswapkz.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Book(
    val id: String = "",
    val title: String = "",
    val author: String = "",
    val condition: String = "",
    val city: String = "",
    val userId: String = "",
    val imageUrl: String? = null,
    val phone: String? = null
) : Parcelable