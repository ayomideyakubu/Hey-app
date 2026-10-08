package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

data class Post(
    val id: String = "",
    val authorId: String = "",
    val authorUsername: String = "",
    val authorDisplayName: String = "",
    val authorAvatarUrl: String = "",
    val content: String = "",
    val imageUrl: String = "",
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    @ServerTimestamp
    val createdAt: Timestamp? = null
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "authorId" to authorId,
        "authorUsername" to authorUsername,
        "authorDisplayName" to authorDisplayName,
        "authorAvatarUrl" to authorAvatarUrl,
        "content" to content,
        "imageUrl" to imageUrl,
        "likesCount" to likesCount,
        "commentsCount" to commentsCount,
        "createdAt" to createdAt
    ).filterValues { it != null }
}
