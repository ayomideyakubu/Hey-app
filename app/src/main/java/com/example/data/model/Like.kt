package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

data class Like(
    val userId: String = "",
    val postId: String = "",
    @ServerTimestamp
    val createdAt: Timestamp? = null
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "userId" to userId,
        "postId" to postId,
        "createdAt" to createdAt
    ).filterValues { it != null }
}
