package com.example.socalnet.presentation.main.model

sealed interface AuthState {
    data object Loading : AuthState
    data object Guest : AuthState
    data class User(val user: com.example.socalnet.core.model.User) : AuthState
}