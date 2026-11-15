package com.example.socalnet.presentation.feed

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.socalnet.presentation.common.UiEvent

@Composable
fun FeedRoute(
    role: Boolean,
    snackbarHostState: SnackbarHostState,
    viewModel: FeedViewModel = hiltViewModel(),
) {
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()

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
        onRefresh = viewModel::refresh,
        onLoadMore = viewModel::loadMore,
    )
}