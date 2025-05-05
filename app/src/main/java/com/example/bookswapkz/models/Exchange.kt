package com.example.bookswapkz.models

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import com.google.firebase.firestore.DocumentId

@Parcelize
data class Exchange(
    @DocumentId var id: String = "",
    val bookId: String = "",
    val oldOwnerId: String = "",
    val newOwnerId: String = "",
    val status: String = "pending",
    @ServerTimestamp
    val exchangeDate: Date? = null,
    // @ServerTimestamp val createdAt: Date? = null,
    // @ServerTimestamp val updatedAt: Date? = null,
    val bookTitle: String = "",
    val oldOwnerName: String = "", // Имя/Ник
    val newOwnerName: String = ""  // Имя/Ник
) : Parcelable {
    constructor() : this("", "", "", "", "pending", null, /* null, null, */ "", "", "")
}