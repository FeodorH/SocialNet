package com.example.socalnet.presentation.auth.verification.preview

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.socalnet.presentation.auth.verification.EmailVerificationScreen
import com.example.socalnet.presentation.auth.verification.EmailVerificationUiState

@Preview(showBackground = true, heightDp = 700, name = "Verify · начальное")
@Composable
private fun EmailVerificationInitialPreview() {
    MaterialTheme {
        EmailVerificationScreen(
            state = EmailVerificationUiState(email = "user@example.com"),
            onResendClick = {}, onCheckClick = {}, onLogoutClick = {},
        )
    }
}

@Preview(showBackground = true, heightDp = 700, name = "Verify · отправка")
@Composable
private fun EmailVerificationSendingPreview() {
    MaterialTheme {
        EmailVerificationScreen(
            state = EmailVerificationUiState(email = "user@example.com", isSending = true),
            onResendClick = {}, onCheckClick = {}, onLogoutClick = {},
        )
    }
}

@Preview(showBackground = true, heightDp = 700, name = "Verify · cooldown")
@Composable
private fun EmailVerificationCooldownPreview() {
    MaterialTheme {
        EmailVerificationScreen(
            state = EmailVerificationUiState(email = "user@example.com", canResendIn = 45),
            onResendClick = {}, onCheckClick = {}, onLogoutClick = {},
        )
    }
}

@Preview(showBackground = true, heightDp = 700, name = "Verify · не подтверждено")
@Composable
private fun EmailVerificationErrorPreview() {
    MaterialTheme {
        EmailVerificationScreen(
            state = EmailVerificationUiState(
                email = "user@example.com",
                errorMessage = "Почта ещё не подтверждена. Проверьте письмо.",
            ),
            onResendClick = {}, onCheckClick = {}, onLogoutClick = {},
        )
    }
}

@Preview(showBackground = true, heightDp = 700, name = "Verify · проверка")
@Composable
private fun EmailVerificationCheckingPreview() {
    MaterialTheme {
        EmailVerificationScreen(
            state = EmailVerificationUiState(email = "user@example.com", isChecking = true),
            onResendClick = {}, onCheckClick = {}, onLogoutClick = {},
        )
    }
}