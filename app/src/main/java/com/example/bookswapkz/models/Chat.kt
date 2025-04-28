package com.example.bookswapkz.models

import android.os.Parcelable
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import kotlinx.parcelize.Parcelize

@Parcelize
@IgnoreExtraProperties
data class Chat(
    @DocumentId
    val chatId: String = "",
    val participantIds: List<String> = emptyList(),
    val participantInfo: Map<String, ParticipantInfo> = emptyMap(),
    val lastMessageText: String? = null,
    val lastMessageTimestamp: Timestamp? = null,
    val lastMessageSenderId: String? = null,
    val unreadCount: Int = 0
) : Parcelable

@Parcelize
data class ParticipantInfo(
    val name: String = "",
    val photoUrl: String? = null
) : Parcelable 