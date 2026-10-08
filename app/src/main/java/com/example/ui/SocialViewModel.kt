package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Comment
import com.example.data.model.DirectMessage
import com.example.data.model.HeyNotification
import com.example.data.model.NotificationType
import com.example.data.model.Post
import com.example.data.model.UserProfile
import com.example.data.repository.SocialRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

class SocialViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SocialRepository(application)
    private val auth = Firebase.auth

    val currentUserId: String? get() = repository.currentUserId()

    // Dark mode state
    private val _isDarkMode = MutableStateFlow(true)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    fun toggleDarkMode() {
        _isDarkMode.value = !_isDarkMode.value
    }

    // Current User Profile State
    private val _userProfileState = MutableStateFlow<UiState<UserProfile?>>(UiState.Loading)
    val userProfileState: StateFlow<UiState<UserProfile?>> = _userProfileState.asStateFlow()

    // Feed Posts State
    val feedPostsState: StateFlow<UiState<List<Post>>> = repository.observeFeedPosts()
        .map<List<Post>, UiState<List<Post>>> { UiState.Success(it) }
        .catch { emit(UiState.Error(it.message ?: "Failed to load feed")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    // User's own posts
    private val _userPosts = MutableStateFlow<List<Post>>(emptyList())
    val userPosts: StateFlow<List<Post>> = _userPosts.asStateFlow()

    // Follow / Unfollow State
    private val _followingUserIds = MutableStateFlow<Set<String>>(emptySet())
    val followingUserIds: StateFlow<Set<String>> = _followingUserIds.asStateFlow()

    // Feed Curation Filter (All vs Following)
    enum class FeedFilter { ALL, FOLLOWING }
    private val _feedFilter = MutableStateFlow(FeedFilter.ALL)
    val feedFilter: StateFlow<FeedFilter> = _feedFilter.asStateFlow()

    fun setFeedFilter(filter: FeedFilter) {
        _feedFilter.value = filter
    }

    // Pull-to-refresh state
    private val _isRefreshingFeed = MutableStateFlow(false)
    val isRefreshingFeed: StateFlow<Boolean> = _isRefreshingFeed.asStateFlow()

    // Other User Profile inspection state
    private val _selectedUserProfile = MutableStateFlow<UserProfile?>(null)
    val selectedUserProfile: StateFlow<UserProfile?> = _selectedUserProfile.asStateFlow()

    private val _selectedUserPosts = MutableStateFlow<List<Post>>(emptyList())
    val selectedUserPosts: StateFlow<List<Post>> = _selectedUserPosts.asStateFlow()

    // All Users for discovery and chatting
    private val _allUsers = MutableStateFlow<List<UserProfile>>(emptyList())
    val allUsers: StateFlow<List<UserProfile>> = _allUsers.asStateFlow()

    // Liked Posts IDs (tracked for instantaneous UI responsiveness)
    private val _likedPostIds = MutableStateFlow<Set<String>>(emptySet())
    val likedPostIds: StateFlow<Set<String>> = _likedPostIds.asStateFlow()

    // Active Chat State
    private val _selectedChatUser = MutableStateFlow<UserProfile?>(null)
    val selectedChatUser: StateFlow<UserProfile?> = _selectedChatUser.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<DirectMessage>>(emptyList())
    val chatMessages: StateFlow<List<DirectMessage>> = _chatMessages.asStateFlow()

    // Active Comments Sheet Post
    private val _activeCommentPost = MutableStateFlow<Post?>(null)
    val activeCommentPost: StateFlow<Post?> = _activeCommentPost.asStateFlow()

    private val _postComments = MutableStateFlow<List<Comment>>(emptyList())
    val postComments: StateFlow<List<Comment>> = _postComments.asStateFlow()

    // Notification Feed State
    private val _notifications = MutableStateFlow<List<HeyNotification>>(emptyList())
    val notifications: StateFlow<List<HeyNotification>> = _notifications.asStateFlow()

    val unreadNotificationsCount: StateFlow<Int> = _notifications
        .map { list -> list.count { !it.read } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = 0
        )

    init {
        loadUserProfile()
        loadAllUsers()
        observeFeedForPersistentLikes()
        observeFollowingRelationships()
        observeNotifications()
    }

    private fun observeNotifications() {
        val uid = currentUserId ?: return
        viewModelScope.launch {
            repository.observeNotifications(uid).collect { notifs ->
                _notifications.value = notifs
            }
        }
    }

    fun markNotificationAsRead(notificationId: String) {
        val uid = currentUserId ?: return
        viewModelScope.launch {
            repository.markNotificationAsRead(uid, notificationId)
        }
    }

    fun markAllNotificationsAsRead() {
        val uid = currentUserId ?: return
        val unreadIds = _notifications.value.filter { !it.read }.map { it.id }
        if (unreadIds.isEmpty()) return
        viewModelScope.launch {
            repository.markAllNotificationsAsRead(uid, unreadIds)
        }
    }

    private fun observeFollowingRelationships() {
        val uid = currentUserId ?: return
        viewModelScope.launch {
            repository.observeFollowingUserIds(uid).collect { following ->
                _followingUserIds.value = following
            }
        }
    }

    fun toggleFollow(targetUserId: String) {
        val isCurrentlyFollowing = _followingUserIds.value.contains(targetUserId)
        val updated = if (isCurrentlyFollowing) {
            _followingUserIds.value - targetUserId
        } else {
            _followingUserIds.value + targetUserId
        }
        _followingUserIds.value = updated

        viewModelScope.launch {
            repository.toggleFollow(targetUserId, isCurrentlyFollowing)
            if (!isCurrentlyFollowing) {
                // Send Follow notification to target user
                val myProfile = (_userProfileState.value as? UiState.Success)?.data ?: UserProfile(
                    userId = currentUserId ?: "",
                    username = "user",
                    displayName = "User"
                )
                repository.sendNotification(
                    recipientId = targetUserId,
                    actorProfile = myProfile,
                    type = NotificationType.FOLLOW
                )
            }
        }
    }

    fun refreshFeed(onFinished: () -> Unit = {}) {
        viewModelScope.launch {
            _isRefreshingFeed.value = true
            val result = repository.fetchLatestFeedPosts()
            if (result.isSuccess) {
                val posts = result.getOrThrow()
                val postIds = posts.map { it.id }
                if (postIds.isNotEmpty()) {
                    val persistentLikes = repository.fetchLikedPostIds(postIds)
                    _likedPostIds.value = _likedPostIds.value + persistentLikes
                }
            }
            _isRefreshingFeed.value = false
            onFinished()
        }
    }

    fun openUserProfile(user: UserProfile) {
        _selectedUserProfile.value = user
        viewModelScope.launch {
            repository.observeUserPosts(user.userId).collect { posts ->
                _selectedUserPosts.value = posts
            }
        }
    }

    fun closeUserProfile() {
        _selectedUserProfile.value = null
        _selectedUserPosts.value = emptyList()
    }

    private fun observeFeedForPersistentLikes() {
        viewModelScope.launch {
            feedPostsState.collect { state ->
                if (state is UiState.Success) {
                    val postIds = state.data.map { it.id }
                    if (postIds.isNotEmpty()) {
                        val persistentLikes = repository.fetchLikedPostIds(postIds)
                        _likedPostIds.value = _likedPostIds.value + persistentLikes
                    }
                }
            }
        }
    }

    fun loadUserProfile() {
        val uid = currentUserId ?: return
        viewModelScope.launch {
            _userProfileState.value = UiState.Loading
            repository.observeUserProfile(uid).collect { profile ->
                if (profile == null) {
                    // Initialize default profile from Firebase Auth details if empty
                    val user = auth.currentUser
                    val initialProfile = UserProfile(
                        userId = uid,
                        username = user?.email?.substringBefore("@")?.lowercase() ?: "user_${uid.take(4)}",
                        displayName = user?.displayName ?: "Hey User",
                        avatarUrl = user?.photoUrl?.toString() ?: "",
                        bio = "Living life & sharing moments ✨"
                    )
                    repository.saveUserProfile(initialProfile)
                    _userProfileState.value = UiState.Success(initialProfile)
                } else {
                    _userProfileState.value = UiState.Success(profile)
                }
            }
        }

        viewModelScope.launch {
            repository.observeUserPosts(uid).collect { posts ->
                _userPosts.value = posts
            }
        }
    }

    private fun loadAllUsers() {
        viewModelScope.launch {
            repository.observeAllUsers().collect { users ->
                _allUsers.value = users
            }
        }
    }

    fun toggleLike(post: Post) {
        val isCurrentlyLiked = _likedPostIds.value.contains(post.id)
        val updatedSet = if (isCurrentlyLiked) {
            _likedPostIds.value - post.id
        } else {
            _likedPostIds.value + post.id
        }
        _likedPostIds.value = updatedSet

        viewModelScope.launch {
            repository.toggleLike(post.id, isCurrentlyLiked)
            if (!isCurrentlyLiked) {
                // Send LIKE notification to post author
                val myProfile = (_userProfileState.value as? UiState.Success)?.data ?: UserProfile(
                    userId = currentUserId ?: "",
                    username = "user",
                    displayName = "User"
                )
                repository.sendNotification(
                    recipientId = post.authorId,
                    actorProfile = myProfile,
                    type = NotificationType.LIKE,
                    postId = post.id,
                    postPreviewText = post.content
                )
            }
        }
    }

    fun createPost(
        content: String,
        imageUrl: String,
        onComplete: (Boolean) -> Unit
    ) {
        val profile = (_userProfileState.value as? UiState.Success)?.data ?: UserProfile(
            userId = currentUserId ?: "",
            username = "user",
            displayName = "User"
        )
        viewModelScope.launch {
            val result = repository.createPost(content, imageUrl, profile)
            onComplete(result.isSuccess)
        }
    }

    fun deletePost(postId: String) {
        viewModelScope.launch {
            repository.deletePost(postId)
        }
    }

    fun observeCommentsForPost(postId: String): kotlinx.coroutines.flow.Flow<List<Comment>> {
        return repository.observeComments(postId)
    }

    fun openComments(post: Post) {
        _activeCommentPost.value = post
        viewModelScope.launch {
            repository.observeComments(post.id).collect { comments ->
                _postComments.value = comments
            }
        }
    }

    fun closeComments() {
        _activeCommentPost.value = null
        _postComments.value = emptyList()
    }

    fun addComment(
        postId: String? = null,
        text: String,
        replyToUsername: String = "",
        replyToCommentId: String = ""
    ) {
        val targetPostId = postId ?: _activeCommentPost.value?.id ?: return
        val profile = (_userProfileState.value as? UiState.Success)?.data ?: UserProfile(
            userId = currentUserId ?: "",
            username = "user",
            displayName = "User"
        )
        viewModelScope.launch {
            repository.addComment(targetPostId, text, profile, replyToUsername, replyToCommentId)
            val postAuthorId = _activeCommentPost.value?.takeIf { it.id == targetPostId }?.authorId
            if (postAuthorId != null && postAuthorId.isNotEmpty()) {
                repository.sendNotification(
                    recipientId = postAuthorId,
                    actorProfile = profile,
                    type = NotificationType.COMMENT,
                    postId = targetPostId,
                    postPreviewText = _activeCommentPost.value?.content ?: "",
                    commentText = text
                )
            }
        }
    }

    fun openChat(user: UserProfile) {
        _selectedChatUser.value = user
        viewModelScope.launch {
            repository.observeMessages(user.userId).collect { messages ->
                _chatMessages.value = messages
            }
        }
    }

    fun closeChat() {
        _selectedChatUser.value = null
        _chatMessages.value = emptyList()
    }

    fun sendChatMessage(text: String) {
        val receiver = _selectedChatUser.value ?: return
        val senderProfile = (_userProfileState.value as? UiState.Success)?.data ?: UserProfile(
            userId = currentUserId ?: "",
            username = "user",
            displayName = "User"
        )
        viewModelScope.launch {
            repository.sendMessage(receiver.userId, text, senderProfile)
        }
    }

    fun updateProfile(username: String, displayName: String, bio: String, avatarUrl: String) {
        val uid = currentUserId ?: return
        val updated = UserProfile(
            userId = uid,
            username = username,
            displayName = displayName,
            bio = bio,
            avatarUrl = avatarUrl
        )
        viewModelScope.launch {
            repository.saveUserProfile(updated)
            _userProfileState.value = UiState.Success(updated)
        }
    }
}
