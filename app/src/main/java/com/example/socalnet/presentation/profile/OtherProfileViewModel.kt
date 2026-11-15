package com.example.socalnet.presentation.profile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.socalnet.core.common.AppResult
import com.example.socalnet.core.common.UiState
import com.example.socalnet.core.model.ReactionType
import com.example.socalnet.domain.usecase.*
import com.example.socalnet.presentation.profile.model.OtherProfileUiState
import com.example.socalnet.presentation.profile.model.ProfileEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class OtherProfileViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val observeUser: ObserveUserUseCase,
    private val observeUserPosts: ObserveUserPostsUseCase,
    private val syncUser: SyncUserUseCase,
    private val syncUserPosts: SyncUserPostsUseCase,
    private val observeIsSubscribed: ObserveIsSubscribedUseCase,
    private val toggleSubscription: ToggleSubscriptionUseCase,
    private val toggleReaction: ToggleReactionUseCase,
) : ViewModel() {

    private val userId: String = savedStateHandle["userId"]
        ?: error("OtherProfileViewModel requires 'userId' argument")

    private val _isRefreshing = MutableStateFlow(false)
    private val _isSubscribing = MutableStateFlow(false)

    private val _events = Channel<ProfileEvent>(Channel.BUFFERED)
    val events: Flow<ProfileEvent> = _events.receiveAsFlow()

    val state: StateFlow<UiState<OtherProfileUiState>> = combine(
        observeUser(userId),
        observeUserPosts(userId),
        observeIsSubscribed(userId),
        _isRefreshing,
        _isSubscribing,
    ) { user, posts, subscribed, refreshing, subscribing ->
        if (user == null) {
            UiState.Loading
        } else {
            UiState.Content(
                OtherProfileUiState(
                    user = user,
                    posts = posts,
                    isRefreshingPosts = refreshing,
                    isSubscribed = subscribed,
                    isSubscriptionLoading = subscribing,
                )
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = UiState.Loading,
    )

    init {
        refresh()
    }

    fun refresh() = viewModelScope.launch {
        _isRefreshing.value = true
        syncUser(userId)
        syncUserPosts(userId)
        _isRefreshing.value = false
    }

    fun onSubscribeClick() = viewModelScope.launch {
        val current = state.value as? UiState.Content ?: return@launch
        _isSubscribing.value = true
        when (val r = toggleSubscription(userId, current.data.isSubscribed)) {
            is AppResult.Success -> Unit
            is AppResult.Failure -> _events.send(ProfileEvent.ShowError(r.error))
        }
        _isSubscribing.value = false
    }

    fun onReactionClick(postId: String, type: ReactionType) = viewModelScope.launch {
        val current = state.value as? UiState.Content ?: return@launch
        val post = current.data.posts.find { it.id == postId } ?: return@launch
        when (val r = toggleReaction(postId, type, post.myReaction)) {
            is AppResult.Success -> Unit
            is AppResult.Failure -> _events.send(ProfileEvent.ShowError(r.error))
        }
    }
}