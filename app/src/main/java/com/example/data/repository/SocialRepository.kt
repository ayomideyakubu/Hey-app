package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.Comment
import com.example.data.model.DirectMessage
import com.example.data.model.HeyNotification
import com.example.data.model.Like
import com.example.data.model.NotificationType
import com.example.data.model.Post
import com.example.data.model.UserProfile
import com.example.util.OperationType
import com.example.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class SocialRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    fun currentUserId(): String? = auth.currentUser?.uid

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    // --- User Profile ---

    fun observeUserProfile(userId: String): Flow<UserProfile?> = callbackFlow {
        val path = "users/$userId"
        val registration = db.collection("users").document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.GET, path)
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val profile = snapshot.toObject(UserProfile::class.java)
                    trySend(profile)
                } else {
                    trySend(null)
                }
            }
        awaitClose { registration.remove() }
    }

    suspend fun saveUserProfile(profile: UserProfile): Result<Unit> {
        return try {
            val uid = requireUserId()
            val path = "users/$uid"
            val userRef = db.collection("users").document(uid)
            val snapshot = userRef.get().await()

            if (!snapshot.exists()) {
                val data = mapOf(
                    "userId" to uid,
                    "username" to profile.username,
                    "displayName" to profile.displayName,
                    "bio" to profile.bio,
                    "avatarUrl" to profile.avatarUrl,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                userRef.set(data).await()
            } else {
                val updates = mapOf(
                    "username" to profile.username,
                    "displayName" to profile.displayName,
                    "bio" to profile.bio,
                    "avatarUrl" to profile.avatarUrl,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                userRef.update(updates).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "users/${auth.currentUser?.uid}")
            Result.failure(e)
        }
    }

    fun observeAllUsers(): Flow<List<UserProfile>> = db.collection("users")
        .snapshots()
        .map { snapshot ->
            snapshot.toObjects(UserProfile::class.java)
        }
        .catch { error ->
            if (error is Exception) handleFirestoreError(error, OperationType.LIST, "users")
            throw error
        }

    // --- Posts Feed ---

    fun observeFeedPosts(): Flow<List<Post>> = db.collection("posts")
        .orderBy("createdAt", Query.Direction.DESCENDING)
        .snapshots()
        .map { snapshot ->
            snapshot.toObjects(Post::class.java)
        }
        .catch { error ->
            if (error is Exception) handleFirestoreError(error, OperationType.LIST, "posts")
            throw error
        }

    fun observeUserPosts(userId: String): Flow<List<Post>> = db.collection("posts")
        .whereEqualTo("authorId", userId)
        .snapshots()
        .map { snapshot ->
            snapshot.toObjects(Post::class.java).sortedByDescending { it.createdAt?.seconds ?: 0L }
        }
        .catch { error ->
            if (error is Exception) handleFirestoreError(error, OperationType.LIST, "posts")
            throw error
        }

    suspend fun createPost(
        content: String,
        imageUrl: String,
        authorProfile: UserProfile
    ): Result<String> {
        return try {
            val uid = requireUserId()
            val postId = UUID.randomUUID().toString()
            val path = "posts/$postId"
            val postData = mapOf(
                "id" to postId,
                "authorId" to uid,
                "authorUsername" to authorProfile.username.ifEmpty { "user" },
                "authorDisplayName" to authorProfile.displayName.ifEmpty { "Hey User" },
                "authorAvatarUrl" to authorProfile.avatarUrl,
                "content" to content,
                "imageUrl" to imageUrl,
                "likesCount" to 0,
                "commentsCount" to 0,
                "createdAt" to FieldValue.serverTimestamp()
            )
            db.collection("posts").document(postId).set(postData).await()
            Result.success(postId)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "posts")
            Result.failure(e)
        }
    }

    suspend fun deletePost(postId: String): Result<Unit> {
        return try {
            requireUserId()
            db.collection("posts").document(postId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "posts/$postId")
            Result.failure(e)
        }
    }

    // --- Likes ---

    fun observePostLikedByMe(postId: String): Flow<Boolean> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(false)
            close()
            return@callbackFlow
        }
        val path = "posts/$postId/likes/$uid"
        val registration = db.collection("posts").document(postId)
            .collection("likes").document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.GET, path)
                    trySend(false)
                    return@addSnapshotListener
                }
                trySend(snapshot != null && snapshot.exists())
            }
        awaitClose { registration.remove() }
    }

    suspend fun toggleLike(postId: String, currentlyLiked: Boolean): Result<Unit> {
        return try {
            val uid = requireUserId()
            val postRef = db.collection("posts").document(postId)
            val likeRef = postRef.collection("likes").document(uid)

            if (currentlyLiked) {
                likeRef.delete().await()
                postRef.update("likesCount", FieldValue.increment(-1)).await()
            } else {
                val likeData = mapOf(
                    "userId" to uid,
                    "postId" to postId,
                    "createdAt" to FieldValue.serverTimestamp()
                )
                likeRef.set(likeData).await()
                postRef.update("likesCount", FieldValue.increment(1)).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "posts/$postId/likes")
            Result.failure(e)
        }
    }

    suspend fun fetchLikedPostIds(postIds: List<String>): Set<String> {
        val uid = auth.currentUser?.uid ?: return emptySet()
        val liked = mutableSetOf<String>()
        for (postId in postIds) {
            try {
                val doc = db.collection("posts").document(postId)
                    .collection("likes").document(uid).get().await()
                if (doc.exists()) {
                    liked.add(postId)
                }
            } catch (e: Exception) {
                // Ignore individual read error
            }
        }
        return liked
    }

    // --- Comments ---

    fun observeComments(postId: String): Flow<List<Comment>> = db.collection("posts")
        .document(postId)
        .collection("comments")
        .orderBy("createdAt", Query.Direction.ASCENDING)
        .snapshots()
        .map { snapshot ->
            snapshot.toObjects(Comment::class.java)
        }
        .catch { error ->
            if (error is Exception) handleFirestoreError(error, OperationType.LIST, "posts/$postId/comments")
            throw error
        }

    suspend fun addComment(
        postId: String,
        text: String,
        authorProfile: UserProfile,
        replyToUsername: String = "",
        replyToCommentId: String = ""
    ): Result<String> {
        return try {
            val uid = requireUserId()
            val commentId = UUID.randomUUID().toString()
            val postRef = db.collection("posts").document(postId)
            val baseData = mutableMapOf<String, Any>(
                "id" to commentId,
                "postId" to postId,
                "authorId" to uid,
                "authorUsername" to authorProfile.username.ifEmpty { "user" },
                "authorDisplayName" to authorProfile.displayName.ifEmpty { "Hey User" },
                "authorAvatarUrl" to authorProfile.avatarUrl,
                "text" to text,
                "createdAt" to FieldValue.serverTimestamp()
            )
            if (replyToUsername.isNotEmpty()) {
                baseData["replyToUsername"] = replyToUsername
            }
            if (replyToCommentId.isNotEmpty()) {
                baseData["replyToCommentId"] = replyToCommentId
            }
            postRef.collection("comments").document(commentId).set(baseData).await()
            postRef.update("commentsCount", FieldValue.increment(1)).await()
            Result.success(commentId)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "posts/$postId/comments")
            Result.failure(e)
        }
    }

    // --- Direct Messages ---

    fun observeMessages(otherUserId: String): Flow<List<DirectMessage>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val path = "messages"
        val registration = db.collection("messages")
            .whereArrayContains("participants", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.LIST, path)
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val messages = snapshot.toObjects(DirectMessage::class.java)
                        .filter { it.participants.contains(otherUserId) }
                        .sortedBy { it.createdAt?.seconds ?: 0L }
                    trySend(messages)
                }
            }
        awaitClose { registration.remove() }
    }

    suspend fun sendMessage(
        receiverId: String,
        text: String,
        senderProfile: UserProfile
    ): Result<String> {
        return try {
            val uid = requireUserId()
            val messageId = UUID.randomUUID().toString()
            val msgData = mapOf(
                "id" to messageId,
                "senderId" to uid,
                "senderName" to senderProfile.displayName.ifEmpty { "Hey User" },
                "senderAvatarUrl" to senderProfile.avatarUrl,
                "receiverId" to receiverId,
                "text" to text,
                "participants" to listOf(uid, receiverId),
                "createdAt" to FieldValue.serverTimestamp()
            )
            db.collection("messages").document(messageId).set(msgData).await()
            Result.success(messageId)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "messages")
            Result.failure(e)
        }
    }

    // --- Follow / Unfollow ---

    fun observeFollowingUserIds(userId: String): Flow<Set<String>> = callbackFlow {
        val path = "follows"
        val registration = db.collection("follows")
            .whereEqualTo("followerId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.LIST, path)
                    trySend(emptySet())
                    return@addSnapshotListener
                }
                val set = snapshot?.documents?.mapNotNull { it.getString("targetUserId") }?.toSet() ?: emptySet()
                trySend(set)
            }
        awaitClose { registration.remove() }
    }

    suspend fun toggleFollow(targetUserId: String, currentlyFollowing: Boolean): Result<Unit> {
        return try {
            val uid = requireUserId()
            val followId = "${uid}_$targetUserId"
            val followRef = db.collection("follows").document(followId)

            if (currentlyFollowing) {
                followRef.delete().await()
            } else {
                val data = mapOf(
                    "id" to followId,
                    "followerId" to uid,
                    "targetUserId" to targetUserId,
                    "createdAt" to FieldValue.serverTimestamp()
                )
                followRef.set(data).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "follows")
            Result.failure(e)
        }
    }

    suspend fun fetchLatestFeedPosts(): Result<List<Post>> {
        return try {
            val snapshot = db.collection("posts")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get().await()
            val posts = snapshot.toObjects(Post::class.java)
            Result.success(posts)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, "posts")
            Result.failure(e)
        }
    }

    // --- Notifications ---

    fun observeNotifications(userId: String): Flow<List<HeyNotification>> = callbackFlow {
        val path = "users/$userId/notifications"
        val registration = db.collection("users").document(userId)
            .collection("notifications")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.LIST, path)
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val notifs = snapshot.toObjects(HeyNotification::class.java)
                    trySend(notifs)
                }
            }
        awaitClose { registration.remove() }
    }

    suspend fun sendNotification(
        recipientId: String,
        actorProfile: UserProfile,
        type: NotificationType,
        postId: String = "",
        postPreviewText: String = "",
        commentText: String = ""
    ): Result<Unit> {
        val actorId = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not logged in"))
        if (actorId == recipientId) return Result.success(Unit) // Don't notify self

        return try {
            val notifId = UUID.randomUUID().toString()
            val data = mutableMapOf<String, Any>(
                "id" to notifId,
                "recipientId" to recipientId,
                "actorId" to actorId,
                "actorUsername" to actorProfile.username.ifEmpty { "user" },
                "actorDisplayName" to actorProfile.displayName.ifEmpty { "Hey User" },
                "actorAvatarUrl" to actorProfile.avatarUrl,
                "type" to type.name,
                "read" to false,
                "createdAt" to FieldValue.serverTimestamp()
            )
            if (postId.isNotEmpty()) data["postId"] = postId
            if (postPreviewText.isNotEmpty()) data["postPreviewText"] = postPreviewText.take(150)
            if (commentText.isNotEmpty()) data["commentText"] = commentText.take(150)

            db.collection("users").document(recipientId)
                .collection("notifications").document(notifId)
                .set(data).await()
            Result.success(Unit)
        } catch (e: Exception) {
            // Notification failures shouldn't block primary user actions
            Result.failure(e)
        }
    }

    suspend fun markNotificationAsRead(userId: String, notificationId: String): Result<Unit> {
        return try {
            db.collection("users").document(userId)
                .collection("notifications").document(notificationId)
                .update("read", true).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun markAllNotificationsAsRead(userId: String, notificationIds: List<String>): Result<Unit> {
        return try {
            val batch = db.batch()
            for (id in notificationIds) {
                val ref = db.collection("users").document(userId)
                    .collection("notifications").document(id)
                batch.update(ref, "read", true)
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
