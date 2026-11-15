package com.example.socalnet.presentation.profile

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.socalnet.presentation.profile.model.ProfileEvent

@Composable
fun OtherProfileRoute(
    onBack: () -> Unit,
    snackbarHostState: SnackbarHostState,
    viewModel: OtherProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ProfileEvent.ShowMessage -> snackbarHostState.showSnackbar(event.text)
                is ProfileEvent.ShowError -> snackbarHostState.showSnackbar("Ошибка при выполнении действия")
                ProfileEvent.LoggedOut -> Unit
            }
        }
    }

    OtherProfileScreen(
        state = state,
        onBack = onBack,
        onRefresh = viewModel::refresh,
        onSubscribeClick = viewModel::onSubscribeClick,
        onReactionClick = viewModel::onReactionClick,
    )
}