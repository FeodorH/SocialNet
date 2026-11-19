package com.example.socialnet.presentation.auth.model

sealed interface AuthEvent {
    /** Письмо сброса пароля отправлено — показать snackbar и вернуться на логин. */
    data object PasswordResetSent : AuthEvent
}