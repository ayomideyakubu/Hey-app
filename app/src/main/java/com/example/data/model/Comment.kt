package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

data class Comment(
    val id: String = "",
    val postId: String = "",
    val authorId: String = "",
    val authorUsername: String = "",
    val authorDisplayName: String = "",
    val authorAvatarUrl: String = "",
    val text: String = "",
    val replyToUsername: String = "",
    val replyToCommentId: String = "",
    @ServerTimestamp
    val createdAt: Timestamp? = null
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "postId" to postId,
        "authorId" to authorId,
        "authorUsername" to authorUsername,
        "authorDisplayName" to authorDisplayName,
        "authorAvatarUrl" to authorAvatarUrl,
        "text" to text,
        "replyToUsername" to replyToUsername,
        "replyToCommentId" to replyToCommentId,
        "createdAt" to createdAt
    ).filterValues { it != null }
}
