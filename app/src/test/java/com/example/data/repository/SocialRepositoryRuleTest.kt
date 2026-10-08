package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.UserProfile
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class SocialRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createPost_validPayload_createsDocumentAndReturnsId() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = SocialRepository(firestore)

        val author = UserProfile(
            userId = auth.currentUser!!.uid,
            username = "alice",
            displayName = "Alice Wonderland"
        )
        val result = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createPost("Hello from Hey!", "", author)
        }
        assertTrue(result.isSuccess)
        val postId = result.getOrThrow()
        assertNotNull(postId)
    }

    @Test
    fun observeFeedPosts_authenticatedUser_returnsPosts() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = SocialRepository(firestore)

        val author = UserProfile(
            userId = auth.currentUser!!.uid,
            username = "alice",
            displayName = "Alice Wonderland"
        )
        val postId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createPost("Another post!", "", author).getOrThrow()
        }

        val posts = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeFeedPosts().first { list -> list.any { it.id == postId } }
        }
        assertTrue(posts.any { it.id == postId })
    }

    @Test
    fun toggleLike_authenticatedUser_updatesLikeInFirestore() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = SocialRepository(firestore)

        val author = UserProfile(
            userId = auth.currentUser!!.uid,
            username = "alice",
            displayName = "Alice Wonderland"
        )
        val postId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createPost("Post to like", "", author).getOrThrow()
        }

        // Like the post
        val likeResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.toggleLike(postId, currentlyLiked = false)
        }
        assertTrue(likeResult.isSuccess)

        // Verify it was marked as liked in Firestore
        val liked = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.fetchLikedPostIds(listOf(postId))
        }
        assertTrue(liked.contains(postId))

        // Unlike the post
        val unlikeResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.toggleLike(postId, currentlyLiked = true)
        }
        assertTrue(unlikeResult.isSuccess)
    }

    @Test
    fun addCommentWithReply_validPayload_createsComment() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = SocialRepository(firestore)

        val author = UserProfile(
            userId = auth.currentUser!!.uid,
            username = "alice",
            displayName = "Alice Wonderland"
        )
        val postId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createPost("Post for comments", "", author).getOrThrow()
        }

        val commentResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.addComment(
                postId = postId,
                text = "Great photo!",
                authorProfile = author,
                replyToUsername = "bob",
                replyToCommentId = UUID.randomUUID().toString()
            )
        }
        assertTrue(commentResult.isSuccess)

        val comments = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeComments(postId).first { it.isNotEmpty() }
        }
        assertTrue(comments.any { it.text == "Great photo!" && it.replyToUsername == "bob" })
    }

    @Test
    fun observePosts_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repository = SocialRepository(firestore)

        var caughtCode: FirebaseFirestoreException.Code? = null
        try {
            repository.observeFeedPosts().first()
        } catch (e: Throwable) {
            var cause: Throwable? = e
            while (cause != null) {
                if (cause is FirebaseFirestoreException) {
                    caughtCode = cause.code
                    break
                }
                cause = cause.cause
            }
        }
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, caughtCode)
    }

    private companion object {
        const val ALICE_EMAIL = "alice@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
