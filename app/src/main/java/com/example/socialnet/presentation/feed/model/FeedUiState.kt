package com.example.socialnet.presentation.feed.model

import com.example.socialnet.core.model.Post

data class FeedUiState(
    val posts: List<Post> = emptyList(),
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = true,
    val isRefreshing: Boolean = false,
    val currentUserId: String? = null,
    val subscribedAuthorIds: Set<String> = emptySet(),
)