package com.example.socalnet.presentation.common.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.socalnet.core.model.Post
import com.example.socalnet.core.model.ReactionType
import com.example.socalnet.presentation.common.UserAvatar
import com.example.socalnet.presentation.common.formatRelativeTime

@Composable
fun PostCard(
    post: Post,
    enabled: Boolean,                    // false для Guest
    showSubscribeButton: Boolean = true, // чтобы не видеть кнопку в просмотре профиля
    isSubscribed: Boolean = false,
    isOwnPost: Boolean = false,
    onReactionClick: (ReactionType) -> Unit,
    onSubscribeClick: () -> Unit = {},
    onAuthorClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var viewerIndex by remember { mutableStateOf<Int?>(null) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onAuthorClick),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                UserAvatar(post.author.name, post.author.avatarUrl)
                Column(Modifier.weight(1f)) {
                    Text(
                        text = post.author.name,
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = formatRelativeTime(post.createdAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (enabled && !isOwnPost && showSubscribeButton) {
                SubscribeButton(isSubscribed, onSubscribeClick)
            }
        }

        if (post.text.isNotBlank()) {
            Text(
                text = post.text,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        PostImages(
            images = post.images,
            onImageClick = { index -> viewerIndex = index }
        )

        ReactionBar(
            summary = post.reactions,
            myReaction = post.myReaction,
            enabled = enabled,
            onReactionClick = onReactionClick,
        )
    }
    HorizontalDivider(thickness = 0.5.dp)

    viewerIndex?.let { index ->
        ImageViewer(
            images = post.images,
            initialIndex = index,
            onDismiss = { viewerIndex = null },
        )
    }
}

@Composable
private fun SubscribeButton(
    isSubscribed: Boolean,
    onClick: () -> Unit,
) {
    if (isSubscribed) {
        TextButton(
            onClick = onClick,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
        ) {
            Text("Отписаться", style = MaterialTheme.typography.labelSmall)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
        ) {
            Text("Подписаться", style = MaterialTheme.typography.labelSmall)
        }
    }
}