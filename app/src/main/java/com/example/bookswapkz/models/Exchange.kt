package com.example.bookswapkz.models

import java.util.Date

data class Exchange(
    val id: String = "",
    val bookId: String = "",
    val oldOwnerId: String = "",
    val newOwnerId: String = "",
    val exchangeDate: Date = Date(),
    val bookTitle: String = "",
    val oldOwnerName: String = "",
    val newOwnerName: String = ""
) 