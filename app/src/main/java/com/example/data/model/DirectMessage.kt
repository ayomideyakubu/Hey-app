package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

data class DirectMessage(
    val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderAvatarUrl: String = "",
    val receiverId: String = "",
    val text: String = "",
    val participants: List<String> = emptyList(),
    @ServerTimestamp
    val createdAt: Timestamp? = null
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "senderId" to senderId,
        "senderName" to senderName,
        "senderAvatarUrl" to senderAvatarUrl,
        "receiverId" to receiverId,
        "text" to text,
        "participants" to participants,
        "createdAt" to createdAt
    ).filterValues { it != null }
}
