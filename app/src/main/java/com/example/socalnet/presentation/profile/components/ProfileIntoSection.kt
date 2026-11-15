package com.example.socalnet.presentation.profile.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.socalnet.core.model.User
import com.example.socalnet.presentation.common.UserAvatar

/**
 * Общая шапка профиля — аватар, имя, bio, счётчик постов.
 * Не содержит кнопок — они рендерятся родительским экраном.
 *
 * @param onAvatarClick null → аватар не кликабелен (для чужого профиля)
 */
@Composable
fun ProfileInfoSection(
    user: User,
    postsCount: Int,
    onAvatarClick: (() -> Unit)? = null,
    isUploadingAvatar: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .then(
                    if (onAvatarClick != null && !isUploadingAvatar) {
                        Modifier.clickable(onClick = onAvatarClick)
                    } else Modifier
                ),
        ) {
            UserAvatar(
                name = user.name,
                avatarUrl = user.avatarUrl,
                size = 96.dp,
            )
            if (isUploadingAvatar) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center).size(32.dp),
                    strokeWidth = 2.dp,
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Text(user.name, style = MaterialTheme.typography.headlineSmall)

        if (!user.bio.isNullOrBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = user.bio,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Постов: $postsCount",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}