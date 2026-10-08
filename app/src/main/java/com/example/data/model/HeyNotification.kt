package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

enum class NotificationType {
    LIKE,
    COMMENT,
    FOLLOW
}

data class HeyNotification(
    val id: String = "",
    val recipientId: String = "",
    val actorId: String = "",
    val actorUsername: String = "",
    val actorDisplayName: String = "",
    val actorAvatarUrl: String = "",
    val type: String = NotificationType.LIKE.name,
    val postId: String = "",
    val postPreviewText: String = "",
    val commentText: String = "",
    val read: Boolean = false,
    @ServerTimestamp
    val createdAt: Timestamp? = null
)
