package com.example.socalnet.presentation.post

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.socalnet.core.common.AppError
import com.example.socalnet.presentation.post.model.SavePostEvent

@Composable
fun SavePostRoute(
    onSaved: () -> Unit,
    onBack: () -> Unit,
    snackbarHostState: SnackbarHostState,
    viewModel: SavePostViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is SavePostEvent.Saved -> onSaved()
                is SavePostEvent.ShowError -> {
                    snackbarHostState.showSnackbar(messageFor(event.error))
                }
            }
        }
    }

    SavePostScreen(
        state = state,
        onTextChange = viewModel::onTextChange,
        onImagesSelected = viewModel::onImagesSelected,
        onNewImageRemoved = viewModel::onNewImageRemoved,
        onExistingImageRemoved = viewModel::onExistingImageRemoved,
        onSave = viewModel::save,
        onBack = onBack,
        onRetryLoad = onBack,  // для MVP — просто назад, если пост не загрузился
    )
}

private fun messageFor(error: AppError): String = when (error) {
    AppError.Network -> "Нет подключения к интернету"
    AppError.Unauthorized -> "Требуется вход"
    AppError.NotFound -> "Пост не найден"
    is AppError.Validation -> error.reason
    is AppError.Server -> "Ошибка сервера. Попробуйте позже"
    is AppError.Unknown -> "Не удалось сохранить пост"
}