package com.example.socialnet.presentation.post

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.socialnet.core.common.AppError
import com.example.socialnet.presentation.common.rememberErrorMapper
import com.example.socialnet.presentation.post.model.SavePostEvent

@Composable
fun SavePostRoute(
    onSaved: () -> Unit,
    onBack: () -> Unit,
    snackbarHostState: SnackbarHostState,
    viewModel: SavePostViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val errorMapper = rememberErrorMapper()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is SavePostEvent.Saved -> onSaved()
                is SavePostEvent.ShowError ->
                    snackbarHostState.showSnackbar(errorMapper.message(event.error))
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
        onRetryLoad = onBack,
    )
}