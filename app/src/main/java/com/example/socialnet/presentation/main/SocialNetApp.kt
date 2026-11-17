package com.example.socialnet.presentation.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.socialnet.presentation.auth.verification.EmailVerificationRoute
import com.example.socialnet.presentation.main.model.AuthState
import com.example.socialnet.presentation.main.navigation.MainNavHost

@Composable
fun SocialNetApp(
    viewModel: AppViewModel = hiltViewModel(),
) {
    val authState by viewModel.authState.collectAsStateWithLifecycle()

    MaterialTheme {
        when (val state = authState) {
            AuthState.Loading -> SplashScreen()
            AuthState.Guest -> MainNavHost(role = false)
            is AuthState.UnverifiedUser -> EmailVerificationRoute(email = state.user.email)
            is AuthState.VerifiedUser -> MainNavHost(role = true)
        }
    }
}

@Composable
private fun SplashScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}