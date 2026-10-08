package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Post
import com.example.data.model.UserProfile
import com.example.ui.SocialViewModel
import com.example.ui.UiState
import com.example.ui.components.PostCard
import com.example.ui.components.PostSkeletonCard
import com.example.ui.components.StorySkeletonRow
import com.example.ui.theme.HeyPink
import com.example.ui.theme.HeyPurple

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    viewModel: SocialViewModel,
    onCreatePostClick: () -> Unit,
    onUserClick: (UserProfile) -> Unit,
    onExploreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val feedState by viewModel.feedPostsState.collectAsState()
    val likedIds by viewModel.likedPostIds.collectAsState()
    val allUsers by viewModel.allUsers.collectAsState()
    val followingIds by viewModel.followingUserIds.collectAsState()
    val feedFilter by viewModel.feedFilter.collectAsState()
    val isRefreshing by viewModel.isRefreshingFeed.collectAsState()
    val currentUserId = viewModel.currentUserId

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { viewModel.refreshFeed() },
        modifier = modifier
            .fillMaxSize()
            .testTag("feed_pull_to_refresh")
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .testTag("feed_list")
        ) {
            // Stories Header Reel
            item {
                StoriesReel(
                    users = allUsers,
                    onUserClick = onUserClick,
                    onCreatePostClick = onCreatePostClick
                )
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    thickness = 0.5.dp
                )
            }

            // Feed Curation Tabs (For You vs Following)
            item {
                TabRow(
                    selectedTabIndex = feedFilter.ordinal,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = HeyPink,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[feedFilter.ordinal]),
                            color = HeyPink
                        )
                    },
                    divider = {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    }
                ) {
                    Tab(
                        selected = feedFilter == SocialViewModel.FeedFilter.ALL,
                        onClick = { viewModel.setFeedFilter(SocialViewModel.FeedFilter.ALL) },
                        text = {
                            Text(
                                text = "For You",
                                fontWeight = if (feedFilter == SocialViewModel.FeedFilter.ALL) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 14.sp
                            )
                        },
                        modifier = Modifier.testTag("tab_feed_all")
                    )

                    Tab(
                        selected = feedFilter == SocialViewModel.FeedFilter.FOLLOWING,
                        onClick = { viewModel.setFeedFilter(SocialViewModel.FeedFilter.FOLLOWING) },
                        text = {
                            Text(
                                text = "Following (${followingIds.size})",
                                fontWeight = if (feedFilter == SocialViewModel.FeedFilter.FOLLOWING) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 14.sp
                            )
                        },
                        modifier = Modifier.testTag("tab_feed_following")
                    )
                }
            }

            when (val state = feedState) {
                is UiState.Loading -> {
                    item {
                        Column {
                            repeat(3) {
                                PostSkeletonCard()
                            }
                        }
                    }
                }

                is UiState.Error -> {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Something went wrong",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = state.message,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.refreshFeed() },
                                colors = ButtonDefaults.buttonColors(containerColor = HeyPink)
                            ) {
                                Text("Retry")
                            }
                        }
                    }
                }

                is UiState.Success -> {
                    val allPosts = state.data
                    val filteredPosts = if (feedFilter == SocialViewModel.FeedFilter.FOLLOWING) {
                        allPosts.filter { followingIds.contains(it.authorId) || it.authorId == currentUserId }
                    } else {
                        allPosts
                    }

                    if (filteredPosts.isEmpty()) {
                        item {
                            if (feedFilter == SocialViewModel.FeedFilter.FOLLOWING) {
                                EmptyFollowingFeedView(
                                    onExploreClick = onExploreClick
                                )
                            } else {
                                EmptyFeedView(onCreatePostClick = onCreatePostClick)
                            }
                        }
                    } else {
                        items(filteredPosts, key = { it.id }) { post ->
                            val isLiked = likedIds.contains(post.id)
                            PostCard(
                                post = post,
                                isLiked = isLiked,
                                viewModel = viewModel,
                                onAuthorClick = { authorId ->
                                    val user = allUsers.find { it.userId == authorId }
                                    if (user != null) onUserClick(user)
                                },
                                onDeleteClick = if (post.authorId == currentUserId) {
                                    { viewModel.deletePost(post.id) }
                                } else null
                            )
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                                thickness = 0.5.dp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StoriesReel(
    users: List<UserProfile>,
    onUserClick: (UserProfile) -> Unit,
    onCreatePostClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // "Your story" / Add post item
        item {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.clickable { onCreatePostClick() }
            ) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Story",
                        tint = HeyPink,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Text(
                    text = "Your Story",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
            }
        }

        items(users) { user ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.clickable { onUserClick(user) }
            ) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .border(
                            width = 2.5.dp,
                            brush = Brush.sweepGradient(listOf(HeyPink, HeyPurple, Color(0xFFFF9500), HeyPink)),
                            shape = CircleShape
                        )
                        .padding(3.dp)
                ) {
                    if (user.avatarUrl.isNotEmpty()) {
                        AsyncImage(
                            model = user.avatarUrl,
                            contentDescription = user.username,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = user.displayName.take(1).ifEmpty { "U" }.uppercase(),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontSize = 18.sp
                            )
                        }
                    }
                }
                Text(
                    text = user.username.ifEmpty { "user" },
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun EmptyFeedView(
    onCreatePostClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.PhotoCamera,
                contentDescription = null,
                tint = HeyPink,
                modifier = Modifier.size(36.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Welcome to hey",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "No posts yet. Pull down to refresh or create your first post!",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onCreatePostClick,
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(containerColor = HeyPink)
        ) {
            Text("Create First Post", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun EmptyFollowingFeedView(
    onExploreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "No Following Posts Yet",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Follow other people to see their latest shared posts in your curated feed.",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = onExploreClick,
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(containerColor = HeyPink)
        ) {
            Text("Discover People", fontWeight = FontWeight.Bold)
        }
    }
}
