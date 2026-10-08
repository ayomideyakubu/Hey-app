package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Comment
import com.example.data.model.Post
import com.example.ui.SocialViewModel
import com.example.ui.theme.HeyPink
import com.example.ui.theme.HeyPurple
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PostCard(
    post: Post,
    isLiked: Boolean,
    viewModel: SocialViewModel,
    onAuthorClick: (String) -> Unit,
    onDeleteClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showBigHeart by remember { mutableStateOf(false) }
    var isHeartBouncing by remember { mutableStateOf(false) }
    var showInlineComments by remember { mutableStateOf(false) }
    var inlineCommentText by remember { mutableStateOf("") }
    var replyingToComment by remember { mutableStateOf<Comment?>(null) }

    val coroutineScope = rememberCoroutineScope()

    // Bouncing spring animation for like button
    val heartScale by animateFloatAsState(
        targetValue = if (isHeartBouncing) 1.35f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "HeartScaleAnim",
        finishedListener = {
            isHeartBouncing = false
        }
    )

    // Observe comments for this post if expanded
    val commentsFlow = remember(post.id) { viewModel.observeCommentsForPost(post.id) }
    val comments by commentsFlow.collectAsState(initial = emptyList())

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("post_card_${post.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header: Author Avatar + Name
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onAuthorClick(post.authorId) }
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .border(
                                width = 2.dp,
                                brush = Brush.linearGradient(listOf(HeyPink, HeyPurple)),
                                shape = CircleShape
                            )
                            .padding(2.dp)
                    ) {
                        if (post.authorAvatarUrl.isNotEmpty()) {
                            AsyncImage(
                                model = post.authorAvatarUrl,
                                contentDescription = "Author Avatar",
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
                                    text = post.authorDisplayName.take(1).ifEmpty { "U" }.uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = post.authorUsername.ifEmpty { "user" },
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (post.authorDisplayName.isNotEmpty() && post.authorDisplayName != post.authorUsername) {
                            Text(
                                text = post.authorDisplayName,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (onDeleteClick != null) {
                    IconButton(onClick = onDeleteClick) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Post options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Image Container with Double Tap to Like
            if (post.imageUrl.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onDoubleTap = {
                                    if (!isLiked) {
                                        viewModel.toggleLike(post)
                                    }
                                    isHeartBouncing = true
                                    showBigHeart = true
                                    coroutineScope.launch {
                                        delay(850)
                                        showBigHeart = false
                                    }
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = post.imageUrl,
                        contentDescription = "Post Image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    // Big Animated Pop-up Heart on double tap
                    androidx.compose.animation.AnimatedVisibility(
                        visible = showBigHeart,
                        enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
                        exit = scaleOut() + fadeOut()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(110.dp)
                                .background(Color.Black.copy(alpha = 0.25f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = null,
                                tint = HeyPink,
                                modifier = Modifier.size(80.dp)
                            )
                        }
                    }
                }
            }

            // Action Buttons Row (Like, Comment, Share)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Heart icon with spring bounce animation
                IconButton(
                    onClick = {
                        isHeartBouncing = true
                        viewModel.toggleLike(post)
                    },
                    modifier = Modifier
                        .scale(heartScale)
                        .testTag("like_button_${post.id}")
                ) {
                    Icon(
                        imageVector = if (isLiked) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = if (isLiked) "Unlike" else "Like",
                        tint = if (isLiked) HeyPink else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(26.dp)
                    )
                }

                IconButton(
                    onClick = {
                        showInlineComments = !showInlineComments
                    },
                    modifier = Modifier.testTag("comment_button_${post.id}")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ChatBubbleOutline,
                        contentDescription = "Comment",
                        tint = if (showInlineComments) HeyPink else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp)
                    )
                }

                IconButton(onClick = { viewModel.openComments(post) }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Send,
                        contentDescription = "Open Sheet",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Likes Count
            if (post.likesCount > 0) {
                Text(
                    text = "${post.likesCount} ${if (post.likesCount == 1) "like" else "likes"}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                )
            }

            // Caption
            if (post.content.isNotEmpty()) {
                val captionText = buildAnnotatedString {
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(post.authorUsername.ifEmpty { "user" })
                        append(" ")
                    }
                    append(post.content)
                }
                Text(
                    text = captionText,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    lineHeight = 18.sp
                )
            }

            // Comments Toggle / Teaser Button
            val totalComments = maxOf(post.commentsCount, comments.size)
            Text(
                text = if (showInlineComments) "Hide comments" else if (totalComments > 0) "View all $totalComments comments & replies" else "Leave a comment...",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = if (showInlineComments) HeyPink else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .clickable { showInlineComments = !showInlineComments }
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            )

            // IN-FEED COMMENTS & REPLIES SECTION
            if (showInlineComments) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(12.dp)
                ) {
                    if (comments.isEmpty()) {
                        Text(
                            text = "No comments yet. Be the first to reply!",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    } else {
                        // Display comments with replies
                        comments.take(6).forEach { comment ->
                            InlineCommentItem(
                                comment = comment,
                                onReplyClick = {
                                    replyingToComment = comment
                                }
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                        }

                        if (comments.size > 6) {
                            Text(
                                text = "View ${comments.size - 6} more comments in sheet...",
                                fontSize = 12.sp,
                                color = HeyPink,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .clickable { viewModel.openComments(post) }
                                    .padding(vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Replying to badge
                    if (replyingToComment != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = HeyPink.copy(alpha = 0.15f),
                            modifier = Modifier.padding(bottom = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Replying to @${replyingToComment?.authorUsername}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = HeyPink
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cancel reply",
                                    tint = HeyPink,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable { replyingToComment = null }
                                )
                            }
                        }
                    }

                    // Inline reply input field
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = inlineCommentText,
                            onValueChange = { inlineCommentText = it },
                            placeholder = {
                                Text(
                                    text = if (replyingToComment != null) "Reply to @${replyingToComment?.authorUsername}..." else "Add a comment...",
                                    fontSize = 13.sp
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("inline_comment_input_${post.id}"),
                            shape = RoundedCornerShape(20.dp),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        IconButton(
                            onClick = {
                                if (inlineCommentText.isNotBlank()) {
                                    val repUser = replyingToComment?.authorUsername ?: ""
                                    val repId = replyingToComment?.id ?: ""
                                    viewModel.addComment(
                                        postId = post.id,
                                        text = inlineCommentText.trim(),
                                        replyToUsername = repUser,
                                        replyToCommentId = repId
                                    )
                                    inlineCommentText = ""
                                    replyingToComment = null
                                }
                            },
                            enabled = inlineCommentText.isNotBlank(),
                            modifier = Modifier.testTag("send_inline_comment_${post.id}")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = if (inlineCommentText.isNotBlank()) HeyPink else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
fun InlineCommentItem(
    comment: Comment,
    onReplyClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        if (comment.authorAvatarUrl.isNotEmpty()) {
            AsyncImage(
                model = comment.authorAvatarUrl,
                contentDescription = comment.authorUsername,
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = comment.authorDisplayName.take(1).ifEmpty { "U" }.uppercase(),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = comment.authorUsername.ifEmpty { "user" },
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (comment.replyToUsername.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "replied to @${comment.replyToUsername}",
                        fontSize = 11.sp,
                        color = HeyPink,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Text(
                text = comment.text,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 16.sp
            )

            Text(
                text = "Reply",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .clickable { onReplyClick() }
                    .padding(vertical = 2.dp)
            )
        }
    }
}
