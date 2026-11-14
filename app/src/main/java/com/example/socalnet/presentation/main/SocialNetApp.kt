package com.example.socalnet.presentation.main

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
import com.example.socalnet.presentation.main.model.AuthState
import com.example.socalnet.presentation.main.navigation.MainNavHost

@Composable
fun SocialNetApp(
    viewModel: AppViewModel = hiltViewModel(),
) {
    val authState by viewModel.authState.collectAsStateWithLifecycle()

    MaterialTheme {
        when (val state = authState) {
            AuthState.Loading -> SplashScreen()
            AuthState.Guest -> MainNavHost(role = false)
            is AuthState.User -> MainNavHost(role = true)
        }
    }
}

@Composable
private fun SplashScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}