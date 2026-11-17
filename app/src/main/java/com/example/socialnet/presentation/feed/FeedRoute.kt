package com.example.socialnet.presentation.feed

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.socialnet.core.common.UiState
import com.example.socialnet.presentation.common.UiEvent

@Composable
fun FeedRoute(
    role: Boolean,
    snackbarHostState: SnackbarHostState,
    onOwnProfileClick: () -> Unit,
    onOtherProfileClick: (String) -> Unit,
    viewModel: FeedViewModel = hiltViewModel(),
) {
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currentUserId = (state as? UiState.Content)?.data?.currentUserId

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is UiEvent.ShowMessage -> snackbarHostState.showSnackbar(event.text)
            }
        }
    }

    FeedScreen(
        state = state,
        selectedTab = selectedTab,
        role = role,
        onTabSelected = viewModel::selectTab,
        onReactionClick = viewModel::onReactionClick,
        onSubscribeClick = viewModel::onSubscribeClick,
        onAuthorClick = { authorId ->
            if (authorId == currentUserId) onOwnProfileClick()
            else onOtherProfileClick(authorId)
        },
        onRefresh = viewModel::refresh,
        onLoadMore = viewModel::loadMore,
    )
}