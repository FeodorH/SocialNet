package com.example.socialnet.presentation.profile

import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.socialnet.R
import com.example.socialnet.core.common.AppResult
import com.example.socialnet.core.common.UiState
import com.example.socialnet.core.model.ImageRef
import com.example.socialnet.core.model.Post
import com.example.socialnet.core.model.ReactionType
import com.example.socialnet.domain.repository.remote.MediaRepository
import com.example.socialnet.domain.usecase.DeletePostUseCase
import com.example.socialnet.domain.usecase.LogoutUseCase
import com.example.socialnet.domain.usecase.ObserveCurrentUserUseCase
import com.example.socialnet.domain.usecase.ObserveUserPostsUseCase
import com.example.socialnet.domain.usecase.SyncCurrentUserUseCase
import com.example.socialnet.domain.usecase.SyncUserPostsUseCase
import com.example.socialnet.domain.usecase.ToggleReactionUseCase
import com.example.socialnet.domain.usecase.UpdateProfileUseCase
import com.example.socialnet.presentation.common.UiStrings
import com.example.socialnet.presentation.profile.model.ProfileEvent
import com.example.socialnet.presentation.profile.model.OwnProfileUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class OwnProfileViewModel @Inject constructor(
    private val observeCurrentUser: ObserveCurrentUserUseCase,
    private val observeUserPosts: ObserveUserPostsUseCase,
    private val syncCurrentUser: SyncCurrentUserUseCase,
    private val syncUserPosts: SyncUserPostsUseCase,
    private val updateProfile: UpdateProfileUseCase,
    private val toggleReactionUseCase: ToggleReactionUseCase,
    private val deletePostUc: DeletePostUseCase,
    private val logoutUc: LogoutUseCase,
    private val mediaRepository: MediaRepository,
    private val uiStrings: UiStrings,
) : ViewModel() {

    private val _isRefreshingPosts = MutableStateFlow(false)
    private val _isUpdatingProfile = MutableStateFlow(false)
    private val _isUploadingAvatar = MutableStateFlow(false)

    private val _events = Channel<ProfileEvent>(Channel.BUFFERED)
    val events: Flow<ProfileEvent> = _events.receiveAsFlow()

    // Посты текущего пользователя (empty если нет user или постов)
    private val postsFlow: Flow<List<Post>> = observeCurrentUser()
        .flatMapLatest { user ->
            if (user == null) flowOf(emptyList())
            else observeUserPosts(user.id)
        }

    val state: StateFlow<UiState<OwnProfileUiState>> = combine(
        observeCurrentUser(),
        postsFlow,
        _isRefreshingPosts,
        _isUpdatingProfile,
        _isUploadingAvatar,
    ) { user, posts, refreshing, updating, uploading ->
        if (user == null) {
            // Нет пользователя — либо Loading (проверяем токен), либо Guest.
            // Guest-сценарий отсекается на уровне MainNavHost (role == GUEST → GuestProfilePrompt).
            UiState.Loading
        } else {
            UiState.Content(
                OwnProfileUiState(
                    user = user,
                    posts = posts,
                    isRefreshingPosts = refreshing,
                    isUpdatingProfile = updating,
                    isUploadingAvatar = uploading,
                )
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = UiState.Loading,
    )

    init {
        // Первая загрузка: профиль синхронизируем «молча», посты — с индикатором
        viewModelScope.launch {
            val userId = observeCurrentUser().first()?.id ?: return@launch

            syncCurrentUser()              // быстро, кэш → сеть

            _isRefreshingPosts.value = true
            syncUserPosts(userId)
            _isRefreshingPosts.value = false
        }
    }

    /**
     * Pull-to-refresh — только посты (как ты и хотел).
     * Профиль обновится после updateProfile-операций или при следующем входе.
     */
    fun refresh() = viewModelScope.launch {
        val userId = (state.value as? UiState.Content)?.data?.user?.id ?: return@launch
        _isRefreshingPosts.value = true
        when (val r = syncUserPosts(userId)) {
            is AppResult.Success -> Unit
            is AppResult.Failure -> _events.send(ProfileEvent.ShowError(r.error))
        }
        _isRefreshingPosts.value = false
    }

    fun updateNameAndBio(name: String, bio: String) = viewModelScope.launch {
        _isUpdatingProfile.value = true
        when (val r = updateProfile(name = name, bio = bio, avatarUrl = null, avatarPublicId = null)) {
            is AppResult.Success -> _events.send(ProfileEvent.ShowMessage(uiStrings.profileUpdated()))
            is AppResult.Failure -> _events.send(ProfileEvent.ShowError(r.error))
        }
        _isUpdatingProfile.value = false
    }

    fun updateAvatar(localUri: String) = viewModelScope.launch {
        val userId = (state.value as? UiState.Content)?.data?.user?.id ?: return@launch
        _isUploadingAvatar.value = true

        when (val uploadResult = mediaRepository.upload(
            ImageRef.Local(localUri),
            path = "socialnet/avatars/$userId/${System.currentTimeMillis()}"
        )) {
            is AppResult.Success -> {
                when (val r = updateProfile(
                    name = null,
                    bio = null,
                    avatarUrl = uploadResult.data.imageRef.url,
                    avatarPublicId = uploadResult.data.imageRef.publicId,
                )) {
                    is AppResult.Success -> _events.send(ProfileEvent.ShowMessage(uiStrings.avatarUpdated()))
                    is AppResult.Failure -> {
                        uploadResult.data.deleteToken?.let { mediaRepository.deleteByToken(it) }
                        _events.send(ProfileEvent.ShowError(r.error))
                    }
                }
            }
            is AppResult.Failure -> _events.send(ProfileEvent.ShowError(uploadResult.error))
        }
        _isUploadingAvatar.value = false
    }

    fun removeAvatar() = viewModelScope.launch {
        _isUpdatingProfile.value = true
        when (val r = updateProfile(
            name = null, bio = null, avatarUrl = null, avatarPublicId = null, removeAvatar = true
        )) {
            is AppResult.Success -> _events.send(ProfileEvent.ShowMessage(uiStrings.avatarDeleted()))
            is AppResult.Failure -> _events.send(ProfileEvent.ShowError(r.error))
        }
        _isUpdatingProfile.value = false
    }

    fun deletePost(postId: String) = viewModelScope.launch {
        when (val r = deletePostUc(postId)) {
            is AppResult.Success -> _events.send(ProfileEvent.ShowMessage(uiStrings.postDeleted()))
            is AppResult.Failure -> _events.send(ProfileEvent.ShowError(r.error))
        }
    }

    fun logout() = viewModelScope.launch {
        when (val r = logoutUc()) {
            is AppResult.Success -> _events.send(ProfileEvent.LoggedOut)
            is AppResult.Failure -> _events.send(ProfileEvent.ShowError(r.error))
        }
    }

    fun onReactionClick(postId: String, type: ReactionType) = viewModelScope.launch {
        val current = state.value
        if (current !is UiState.Content) return@launch

        val post = current.data.posts.find { it.id == postId } ?: return@launch

        when (val r = toggleReactionUseCase(postId, type, post.myReaction)) {
            is AppResult.Success -> Unit
            // Room обновится → postsFlow эмитит → state пересчитается → UI перерисуется

            is AppResult.Failure -> _events.send(
                ProfileEvent.ShowError(r.error)
            )
        }
    }
}