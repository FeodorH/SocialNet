package com.example.socialnet.presentation.profile.model

import com.example.socialnet.core.model.Post
import com.example.socialnet.core.model.User

data class OtherProfileUiState(
    val user: User,
    val posts: List<Post> = emptyList(),
    val isRefreshingPosts: Boolean = false,
    val isSubscribed: Boolean = false,
    val isSubscriptionLoading: Boolean = false,
)