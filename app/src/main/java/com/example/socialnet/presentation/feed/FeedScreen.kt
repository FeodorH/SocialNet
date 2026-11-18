package com.example.socialnet.presentation.feed

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.socialnet.core.common.UiState
import com.example.socialnet.core.model.FeedType
import com.example.socialnet.core.model.ReactionType
import com.example.socialnet.presentation.common.components.PostCard
import com.example.socialnet.presentation.feed.model.FeedUiState
import kotlinx.coroutines.flow.distinctUntilChanged
import com.example.socialnet.R

@Composable
fun FeedScreen(
    state: UiState<FeedUiState>,
    selectedTab: FeedType,
    role: Boolean,
    onTabSelected: (FeedType) -> Unit,
    onReactionClick: (postId: String, type: ReactionType) -> Unit,
    onSubscribeClick: (authorId: String, isSubscribed: Boolean) -> Unit,
    onAuthorClick :(authorId: String) -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {

        SecondaryTabRow(
            selectedTabIndex = selectedTab.ordinal,
        ) {
            Tab(
                selected = selectedTab == FeedType.GENERAL,
                onClick = { onTabSelected(FeedType.GENERAL) },
                text = { Text(stringResource(R.string.feed_tab_general)) },
            )
            Tab(
                selected = selectedTab == FeedType.SUBSCRIPTIONS,
                onClick = { onTabSelected(FeedType.SUBSCRIPTIONS) },
                enabled = role,
                text = {
                    Text(
                        text = stringResource(R.string.feed_tab_subscriptions),
                        color = if (selectedTab == FeedType.SUBSCRIPTIONS) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (role) 1f else 0.38f)
                        },
                    )
                },
            )
        }

        when (state) {
            UiState.Loading -> LoadingState()
            UiState.Empty -> EmptyState(
                message = stringResource(R.string.feed_empty),
                hint = if (role) stringResource(R.string.feed_empty_hint_user)
                else stringResource(R.string.feed_empty_hint_guest),
            )
            is UiState.Error -> ErrorState(
                message = state.message,
                onRetry = onRefresh,
            )
            is UiState.Content -> ContentState(
                state = state.data,
                isUser = role,
                onReactionClick = onReactionClick,
                onSubscribeClick = onSubscribeClick,
                onAuthorClick = onAuthorClick,
                onRefresh = onRefresh,
                onLoadMore = onLoadMore,
            )
        }
    }
}

@Composable
private fun ContentState(
    state: FeedUiState,
    isUser: Boolean,
    onReactionClick: (postId: String, type: ReactionType) -> Unit,
    onSubscribeClick: (authorId: String, isSubscribed: Boolean) -> Unit,
    onAuthorClick: (authorId: String) -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
) {
    val listState = rememberLazyListState()
    val latestState by rememberUpdatedState(state)

    // Триггер пагинации: когда пользователь приблизился к концу
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .distinctUntilChanged()
            .collect { lastVisible ->
                val s = latestState
                if (lastVisible != null
                    && lastVisible >= s.posts.size - 3
                    && s.hasMore
                    && !s.isLoadingMore
                    && !s.isRefreshing
                ) {
                    onLoadMore()
                }
            }
    }

    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
        ) {
            items(state.posts, key = { it.id }) { post ->
                val isOwnPost = post.author.id == state.currentUserId
                val isSubscribed = post.author.id in state.subscribedAuthorIds
                PostCard(
                    post = post,
                    enabled = isUser,
                    isSubscribed = isSubscribed,
                    isOwnPost = isOwnPost,
                    showSubscribeButton = true,
                    onReactionClick = { type -> onReactionClick(post.id, type) },
                    onSubscribeClick = { onSubscribeClick(post.author.id, isSubscribed) },
                    onAuthorClick = { onAuthorClick(post.author.id) },
                )
            }
            if (state.isLoadingMore) {
                item {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center,
                    ) { CircularProgressIndicator() }
                }
            }
            if (!state.hasMore && state.posts.isNotEmpty()) {
                item {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            stringResource(R.string.no_more_posts),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptyState(message: String, hint: String) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(message, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            hint,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(message, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))
        Button(onClick = onRetry) { Text(stringResource(R.string.retry)) }
    }
}