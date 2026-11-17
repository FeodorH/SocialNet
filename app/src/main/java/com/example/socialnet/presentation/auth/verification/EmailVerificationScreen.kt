package com.example.socialnet.presentation.auth.verification

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun EmailVerificationScreen(
    state: EmailVerificationUiState,
    onResendClick: () -> Unit,
    onCheckClick: () -> Unit,
    onLogoutClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(96.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.Email,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        Text(
            "Подтвердите почту",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(12.dp))

        Text(
            "Мы отправили письмо на",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        if (state.email.isNotEmpty()) {
            Text(
                state.email,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
        }

        Spacer(Modifier.height(8.dp))

        Text(
            "Перейдите по ссылке из письма, затем нажмите «Я подтвердил».",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        if (state.errorMessage != null) {
            Spacer(Modifier.height(12.dp))
            Text(
                state.errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
            )
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = onCheckClick,
            enabled = state.canCheck,
            modifier = Modifier.fillMaxWidth().height(48.dp),
        ) {
            if (state.isChecking) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Text("Я подтвердил")
            }
        }

        Spacer(Modifier.height(8.dp))

        OutlinedButton(
            onClick = onResendClick,
            enabled = state.canResend,
            modifier = Modifier.fillMaxWidth().height(48.dp),
        ) {
            if (state.isSending) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                )
            } else if (state.canResendIn > 0) {
                Text("Отправить повторно через ${state.canResendIn} с")
            } else {
                Text("Отправить письмо повторно")
            }
        }

        Spacer(Modifier.height(24.dp))

        TextButton(onClick = onLogoutClick) {
            Text("Выйти из аккаунта", color = MaterialTheme.colorScheme.error)
        }
    }
}