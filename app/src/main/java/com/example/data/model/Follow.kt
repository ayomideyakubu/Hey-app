package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

data class Follow(
    val id: String = "",
    val followerId: String = "",
    val targetUserId: String = "",
    @ServerTimestamp
    val createdAt: Timestamp? = null
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "followerId" to followerId,
        "targetUserId" to targetUserId,
        "createdAt" to createdAt
    ).filterValues { it != null }
}
