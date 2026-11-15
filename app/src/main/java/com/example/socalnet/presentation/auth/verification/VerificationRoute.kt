package com.example.socalnet.presentation.auth.verification

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun EmailVerificationRoute(
    email: String,
    viewModel: EmailVerificationViewModel = hiltViewModel(),
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