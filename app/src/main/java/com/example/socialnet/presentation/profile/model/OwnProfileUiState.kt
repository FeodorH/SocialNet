package com.example.socialnet.presentation.profile.model

import com.example.socialnet.core.model.Post
import com.example.socialnet.core.model.User

data class OwnProfileUiState(
    val user: User,
    val posts: List<Post> = emptyList(),

    val isRefreshingPosts: Boolean = false,  // загрузка постов постов
    val isUpdatingProfile: Boolean = false,  // сохранение name/bio
    val isUploadingAvatar: Boolean = false,  // загрузка аватара
)