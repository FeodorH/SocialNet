package com.example.socialnet.presentation.auth.verification

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner

@Composable
fun EmailVerificationRoute(
    email: String,
    viewModel: EmailVerificationViewModel = androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel(
        checkNotNull(LocalViewModelStoreOwner.current) {
                "No ViewModelStoreOwner was provided via LocalViewModelStoreOwner"
            },
        null
    ),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(email) {
        viewModel.setEmail(email)
    }

    EmailVerificationScreen(
        state = state,
        onResendClick = viewModel::resend,
        onCheckClick = viewModel::checkVerification,
        onLogoutClick = viewModel::logoutAndExit,
    )
}