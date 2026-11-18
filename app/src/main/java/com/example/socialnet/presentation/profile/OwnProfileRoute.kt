package com.example.socialnet.presentation.profile

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.socialnet.core.common.AppError
import com.example.socialnet.presentation.common.rememberErrorMapper
import com.example.socialnet.presentation.profile.model.ProfileEvent

@Composable
fun ProfileRoute(
    role: Boolean,
    onLoginClick: () -> Unit,
    onRegisterClick: () -> Unit,
    onEditPostClick: (String) -> Unit,
    onLoggedOut: () -> Unit,
    snackbarHostState: SnackbarHostState,
    viewModel: OwnProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val errorMapper = rememberErrorMapper()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ProfileEvent.ShowMessage -> snackbarHostState.showSnackbar(event.text)
                is ProfileEvent.ShowError ->
                    snackbarHostState.showSnackbar(errorMapper.message(event.error))
                ProfileEvent.LoggedOut -> onLoggedOut()
            }
        }
    }

    ProfileScreen(
        state = state,
        role = role,
        onLoginClick = onLoginClick,
        onRegisterClick = onRegisterClick,
        onRefresh = viewModel::refresh,
        onAvatarPicked = viewModel::updateAvatar,
        onRemoveAvatar = viewModel::removeAvatar,
        onUpdateNameBio = viewModel::updateNameAndBio,
        onEditPostClick = onEditPostClick,
        onDeletePostClick = viewModel::deletePost,
        onReactionClick = viewModel::onReactionClick,
        onLogoutClick = viewModel::logout,
    )
}