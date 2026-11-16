package com.example.socalnet.presentation.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.socalnet.core.common.AppResult
import com.example.socalnet.core.common.UiState
import com.example.socalnet.core.model.FeedType
import com.example.socalnet.core.model.PageCursor
import com.example.socalnet.core.model.Post
import com.example.socalnet.core.model.ReactionType
import com.example.socalnet.domain.usecase.ObserveCurrentUserUseCase
import com.example.socalnet.domain.usecase.ObserveFeedUseCase
import com.example.socalnet.domain.usecase.ObserveSubscriptionIdsUseCase
import com.example.socalnet.domain.usecase.SyncFeedUseCase
import com.example.socalnet.domain.usecase.ToggleReactionUseCase
import com.example.socalnet.domain.usecase.ToggleSubscriptionUseCase
import com.example.socalnet.presentation.common.UiEvent
import com.example.socalnet.presentation.feed.model.FeedUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.collections.emptyList

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class FeedViewModel @Inject constructor(
    private val observeFeed: ObserveFeedUseCase,
    private val syncFeedUseCase: SyncFeedUseCase,
    private val toggleReaction: ToggleReactionUseCase,
    private val observeCurrentUser: ObserveCurrentUserUseCase,
    private val observeSubscriptionIds: ObserveSubscriptionIdsUseCase,
    private val toggleSubscription: ToggleSubscriptionUseCase,
) : ViewModel() {

    /** управляющие флаги */
    private val _selectedTab = MutableStateFlow(FeedType.GENERAL)
    private val _isRefreshing = MutableStateFlow(false)
    private val _isLoadingMore = MutableStateFlow(false)
    private val _hasMore = MutableStateFlow(true)
    private val _initialAttemptCompleted = MutableStateFlow(false)
    private val _initialError = MutableStateFlow<String?>(null)

    private val currentUserIdFlow = observeCurrentUser().map { it?.id }
    private val subscriptionIdsFlow = observeSubscriptionIds()
    val selectedTab: StateFlow<FeedType> = _selectedTab.asStateFlow()

    private val _autoLoadMoreBlocked = MutableStateFlow(false) // состояние блокировки загрузки постов

    private var cursor: PageCursor? = null

    /** События (Snackbar и т.п.) */
    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events: Flow<UiEvent> = _events.receiveAsFlow()


    private val feedFlow: Flow<List<Post>> =
        _selectedTab.flatMapLatest {
            observeFeed(it)
        }

    val state: StateFlow<UiState<FeedUiState>> = combine(
        feedFlow,
        combine(_isRefreshing, _isLoadingMore) { r, l -> r to l },
        combine(_hasMore, _initialAttemptCompleted) { h, a -> h to a },
        combine(_initialError, currentUserIdFlow, subscriptionIdsFlow) { e, u, s -> Triple(e, u, s) },
    ) { posts, (refreshing, loadingMore), (hasMore, completed), (error, currentUserId, subscribedIds) ->
        val feedState = FeedUiState(
            posts = posts,
            isRefreshing = refreshing,
            isLoadingMore = loadingMore,
            hasMore = hasMore,
            currentUserId = currentUserId,
            subscribedAuthorIds = subscribedIds,
        )
        when {
            posts.isNotEmpty() -> UiState.Content(feedState)
            !completed -> UiState.Loading
            error != null -> UiState.Error(error)
            else -> UiState.Empty
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = UiState.Loading,
    )

    init {
        performSync(_selectedTab.value, cursor = null)

        viewModelScope.launch {
            observeCurrentUser().collect { user ->
                if (user == null && _selectedTab.value == FeedType.SUBSCRIPTIONS) {
                    selectTab(FeedType.GENERAL)
                }
            }
        }
    }

    fun selectTab(tab: FeedType) = viewModelScope.launch {
        if (tab == _selectedTab.value) return@launch
        if (tab == FeedType.SUBSCRIPTIONS && observeCurrentUser().first() == null) {
            _events.send(UiEvent.ShowMessage("Войдите, чтобы увидеть подписки"))
            return@launch
        }
        _selectedTab.value = tab
        cursor = null
        _hasMore.value = true
        _initialAttemptCompleted.value = false
        _initialError.value = null
        performSync(tab, cursor = null)
    }

    fun refresh() {
        if (_isRefreshing.value || _isLoadingMore.value) return
        cursor = null
        _hasMore.value = true
        performSync(_selectedTab.value, cursor = null)
    }

    fun loadMore() {
        if (_isRefreshing.value || _isLoadingMore.value) return
        if (_autoLoadMoreBlocked.value) return
        val c = cursor ?: return
        if (!_hasMore.value) return
        performSync(_selectedTab.value, c)
    }

    fun onReactionClick(postId: String, type: ReactionType) = viewModelScope.launch {
        val current = state.value
        if (current !is UiState.Content) return@launch

        val post = current.data.posts.find { it.id == postId } ?: return@launch

        when (val r = toggleReaction(postId, type, post.myReaction)) {
            is AppResult.Success -> {

            }
            is AppResult.Failure -> {
                _events.send(UiEvent.ShowMessage("Не удалось изменить реакцию"))
            }
        }
    }

    fun onSubscribeClick(authorId: String, isSubscribed: Boolean) = viewModelScope.launch {
        when (val r = toggleSubscription(authorId, isSubscribed)) {
            is AppResult.Success -> Unit
            is AppResult.Failure -> _events.send(UiEvent.ShowMessage("Не удалось изменить подписку"))
        }
    }

    private fun performSync(type: FeedType, cursor: PageCursor?) {
        val isRefresh = cursor == null

        if (isRefresh) {
            if (_isRefreshing.value) return
            _isRefreshing.value = true
            _autoLoadMoreBlocked.value = false
        } else {
            if (_isLoadingMore.value) return
            _isLoadingMore.value = true
        }
        viewModelScope.launch {
            try {
                when (val result = syncFeedUseCase(type, cursor)) {
                    is AppResult.Success -> {
                        this@FeedViewModel.cursor = result.data
                        _hasMore.value = result.data != null
                        _initialAttemptCompleted.value = true
                        _initialError.value = null
                    }

                    is AppResult.Failure -> {
                        _initialAttemptCompleted.value = true
                        if (cursor != null) {
                            _autoLoadMoreBlocked.value = true
                        }
                        if (state.value is UiState.Content) {
                            _events.send(UiEvent.ShowMessage("Не удалось загрузить"))
                        } else {
                            _initialError.value = result.error.toString()
                        }
                    }
                }
            } finally {
                if (isRefresh) _isRefreshing.value = false
                else _isLoadingMore.value = false
            }
        }
    }
}