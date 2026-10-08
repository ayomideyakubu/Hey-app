package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.HeyNotification
import com.example.data.model.NotificationType
import com.example.data.model.UserProfile
import com.example.ui.SocialViewModel
import com.example.ui.theme.HeyPink
import com.example.ui.theme.HeyPurple

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    viewModel: SocialViewModel,
    onBackClick: () -> Unit,
    onUserClick: (UserProfile) -> Unit,
    onPostClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val notifications by viewModel.notifications.collectAsState()
    val allUsers by viewModel.allUsers.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Notifications",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (notifications.any { !it.read }) {
                        TextButton(
                            onClick = { viewModel.markAllNotificationsAsRead() },
                            modifier = Modifier.testTag("mark_all_read_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DoneAll,
                                contentDescription = "Mark all read",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Mark read", fontSize = 13.sp)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier
    ) { padding ->
        if (notifications.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(MaterialTheme.colorScheme.background)
                    .testTag("empty_notifications_view"),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            tint = HeyPink,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No notifications yet",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "When someone likes, comments, or follows you, you'll see it here.",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(MaterialTheme.colorScheme.background)
                    .testTag("notifications_list")
            ) {
                items(notifications, key = { it.id }) { item ->
                    NotificationItemRow(
                        notification = item,
                        onClick = {
                            viewModel.markNotificationAsRead(item.id)
                            if (item.type == NotificationType.FOLLOW.name) {
                                val user = allUsers.find { it.userId == item.actorId }
                                if (user != null) {
                                    onUserClick(user)
                                } else {
                                    onUserClick(
                                        UserProfile(
                                            userId = item.actorId,
                                            username = item.actorUsername,
                                            displayName = item.actorDisplayName,
                                            avatarUrl = item.actorAvatarUrl
                                        )
                                    )
                                }
                            } else if (item.postId.isNotEmpty()) {
                                onPostClick(item.postId)
                            }
                        },
                        onActorClick = {
                            viewModel.markNotificationAsRead(item.id)
                            val user = allUsers.find { it.userId == item.actorId }
                            if (user != null) {
                                onUserClick(user)
                            } else {
                                onUserClick(
                                    UserProfile(
                                        userId = item.actorId,
                                        username = item.actorUsername,
                                        displayName = item.actorDisplayName,
                                        avatarUrl = item.actorAvatarUrl
                                    )
                                )
                            }
                        }
                    )
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                        thickness = 0.5.dp
                    )
                }
            }
        }
    }
}

@Composable
fun NotificationItemRow(
    notification: HeyNotification,
    onClick: () -> Unit,
    onActorClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val unread = !notification.read

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(if (unread) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("notification_item_${notification.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Actor avatar with small action badge
        Box(
            modifier = Modifier
                .size(48.dp)
                .clickable { onActorClick() }
        ) {
            if (notification.actorAvatarUrl.isNotEmpty()) {
                AsyncImage(
                    model = notification.actorAvatarUrl,
                    contentDescription = notification.actorUsername,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = notification.actorDisplayName.take(1).ifEmpty { "U" }.uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontSize = 16.sp
                    )
                }
            }

            // Notification action badge overlay
            val badgeBg = when (notification.type) {
                NotificationType.LIKE.name -> HeyPink
                NotificationType.COMMENT.name -> HeyPurple
                else -> MaterialTheme.colorScheme.primary
            }

            val badgeIcon = when (notification.type) {
                NotificationType.LIKE.name -> Icons.Default.Favorite
                NotificationType.COMMENT.name -> Icons.Default.Comment
                else -> Icons.Default.PersonAdd
            }

            Box(
                modifier = Modifier
                    .size(20.dp)
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(badgeBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = badgeIcon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Message text
        Column(
            modifier = Modifier.weight(1f)
        ) {
            val actorName = notification.actorDisplayName.ifEmpty { "@${notification.actorUsername}" }
            val actionText = when (notification.type) {
                NotificationType.LIKE.name -> "liked your post"
                NotificationType.COMMENT.name -> "commented: \"${notification.commentText.take(40)}\""
                NotificationType.FOLLOW.name -> "started following you"
                else -> "interacted with you"
            }

            Text(
                text = "$actorName $actionText",
                fontSize = 14.sp,
                fontWeight = if (unread) FontWeight.Bold else FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (notification.postPreviewText.isNotEmpty() && notification.type == NotificationType.LIKE.name) {
                Text(
                    text = "\"${notification.postPreviewText.take(50)}\"",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (unread) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(HeyPink)
            )
        }
    }
}
