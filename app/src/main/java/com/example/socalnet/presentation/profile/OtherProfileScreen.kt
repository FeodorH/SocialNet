package com.example.socalnet.presentation.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.socalnet.core.common.UiState
import com.example.socalnet.core.model.ReactionType
import com.example.socalnet.presentation.common.components.PostCard
import com.example.socalnet.presentation.profile.components.OtherProfileActions
import com.example.socalnet.presentation.profile.components.ProfileInfoSection
import com.example.socalnet.presentation.profile.model.OtherProfileUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtherProfileScreen(
    state: UiState<OtherProfileUiState>,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onSubscribeClick: () -> Unit,
    onReactionClick: (postId: String, type: ReactionType) -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = (state as? UiState.Content)?.data?.user?.name ?: "Профиль"

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад")
                    }
                },
            )
        },
    ) { padding ->
        when (state) {
            UiState.Loading -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            UiState.Empty -> MessageContent(
                message = "Профиль не найден",
                action = "Повторить" to onRefresh,
                modifier = Modifier.padding(padding),
            )

            is UiState.Error -> MessageContent(
                message = state.message,
                action = if (state.retryable) "Повторить" to onRefresh else null,
                modifier = Modifier.padding(padding),
            )

            is UiState.Content -> ContentState(
                state = state.data,
                onRefresh = onRefresh,
                onSubscribeClick = onSubscribeClick,
                onReactionClick = onReactionClick,
                modifier = Modifier.padding(padding),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContentState(
    state: OtherProfileUiState,
    onRefresh: () -> Unit,
    onSubscribeClick: () -> Unit,
    onReactionClick: (String, ReactionType) -> Unit,
    modifier: Modifier = Modifier,
) {
    PullToRefreshBox(
        isRefreshing = state.isRefreshingPosts,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize(),
    ) {
        LazyColumn(Modifier.fillMaxSize()) {

            item {
                Column(Modifier.fillMaxWidth()) {
                    ProfileInfoSection(
                        user = state.user,
                        postsCount = state.posts.size,
                        // аватар чужого не кликабелен
                        onAvatarClick = null,
                        isUploadingAvatar = false,
                    )
                    OtherProfileActions(
                        isSubscribed = state.isSubscribed,
                        isLoading = state.isSubscriptionLoading,
                        onToggleSubscription = onSubscribeClick,
                    )
                    Spacer(Modifier.height(12.dp))
                }
                HorizontalDivider()
            }

            if (state.posts.isEmpty()) {
                item {
                    Box(
                        Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "У пользователя пока нет постов",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                items(state.posts, key = { it.id }) { post ->
                    PostCard(
                        post = post,
                        enabled = true,
                        isSubscribed = state.isSubscribed,
                        isOwnPost = false,
                        showSubscribeButton = false,
                        onReactionClick = { type -> onReactionClick(post.id, type) },
                        onSubscribeClick = {},
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageContent(
    message: String,
    action: Pair<String, () -> Unit>?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(message, style = MaterialTheme.typography.titleMedium)
        if (action != null) {
            Spacer(Modifier.height(12.dp))
            Button(onClick = action.second) { Text(action.first) }
        }
    }
}