package com.example.socalnet.presentation.profile

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.socalnet.core.common.AppError

@Composable
fun ProfileRoute(
    role: Boolean,
    onLoginClick: () -> Unit,
    onRegisterClick: () -> Unit,
    onEditPostClick: (String) -> Unit,
    onLoggedOut: () -> Unit,
    snackbarHostState: SnackbarHostState,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ProfileEvent.ShowMessage -> snackbarHostState.showSnackbar(event.text)
                is ProfileEvent.ShowError -> snackbarHostState.showSnackbar(messageFor(event.error))
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
        onReactionClick = { _, _ -> /* можно сделать изменение реакции */ },
        onLogoutClick = viewModel::logout,
    )
}

private fun messageFor(error: AppError): String = when (error) {
    AppError.Network -> "Нет подключения к интернету"
    AppError.Unauthorized -> "Требуется вход"
    AppError.NotFound -> "Данные не найдены"
    is AppError.Validation -> error.reason
    is AppError.Server -> "Ошибка сервера. Попробуйте позже"
    is AppError.Unknown -> "Что-то пошло не так"
}