package com.example.bookswapkz.models

import android.os.Parcelable
import com.google.firebase.firestore.PropertyName
import kotlinx.parcelize.Parcelize

@Parcelize
data class Book(
    val id: String = "",
    val title: String = "",
    val author: String = "",
    val condition: String = "",
    val city: String = "",
    val userId: String = "",
    val phone: String? = null,
    val ownerCount: Int = 1,
    @PropertyName("forRent")
    val isForRent: Boolean = false,
    val rentPrice: Double? = null,
    val rentPeriod: String? = null, // e.g., "1 week", "2 weeks", "1 month"
    @PropertyName("rented")
    val isRented: Boolean = false,
    val rentedToUserId: String? = null,
    val rentStartDate: String? = null,
    val rentEndDate: String? = null
) : Parcelable