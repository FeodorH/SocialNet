package com.example.socalnet.presentation.main.model

import com.example.socalnet.core.model.User

sealed interface AuthState {
    data object Loading : AuthState
    data object Guest : AuthState
    data class UnverifiedUser(val user: User) : AuthState // Вошёл, но почта не подтверждена
    data class VerifiedUser(val user: User) : AuthState
}