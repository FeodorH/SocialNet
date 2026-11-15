package com.example.socalnet.presentation.auth.verification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.socalnet.core.common.AppError
import com.example.socalnet.core.common.AppResult
import com.example.socalnet.domain.usecase.LogoutUseCase
import com.example.socalnet.domain.usecase.ReloadCurrentUserUseCase
import com.example.socalnet.domain.usecase.SendEmailVerificationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EmailVerificationUiState(
    val email: String = "",
    val isSending: Boolean = false,
    val isChecking: Boolean = false,
    val canResendIn: Int = 0,
    val errorMessage: String? = null,
) {
    val canResend: Boolean
        get() = !isSending && canResendIn == 0
    val canCheck: Boolean
        get() = !isChecking
}

@HiltViewModel
class EmailVerificationViewModel @Inject constructor(
    private val reloadCurrentUser: ReloadCurrentUserUseCase,
    private val sendEmailVerification: SendEmailVerificationUseCase,
    private val logout: LogoutUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(EmailVerificationUiState())
    val state: StateFlow<EmailVerificationUiState> = _state.asStateFlow()

    private var cooldownJob: Job? = null

    fun setEmail(email: String) {
        if (_state.value.email.isEmpty()) {
            _state.update { it.copy(email = email) }
        }
    }

    fun resend() = viewModelScope.launch {
        if (!_state.value.canResend) return@launch
        _state.update { it.copy(isSending = true, errorMessage = null) }

        when (val r = sendEmailVerification()) {
            is AppResult.Success -> {
                _state.update { it.copy(isSending = false) }
                startCooldown(60)
            }
            is AppResult.Failure -> {
                _state.update {
                    it.copy(isSending = false, errorMessage = mapError(r.error))
                }
            }
        }
    }

    fun checkVerification() = viewModelScope.launch {
        if (_state.value.isChecking) return@launch
        _state.update { it.copy(isChecking = true, errorMessage = null) }

        when (val r = reloadCurrentUser()) {
            is AppResult.Success -> {
                if (r.data) {
                    // Почта подтверждена — AppViewModel сам переключит экран
                    _state.update { it.copy(isChecking = false) }
                } else {
                    _state.update {
                        it.copy(
                            isChecking = false,
                            errorMessage = "Почта ещё не подтверждена. Проверьте письмо.",
                        )
                    }
                }
            }
            is AppResult.Failure -> {
                _state.update {
                    it.copy(isChecking = false, errorMessage = mapError(r.error))
                }
            }
        }
    }

    fun logoutAndExit() = viewModelScope.launch {
        logout()
    }

    private fun startCooldown(seconds: Int) {
        cooldownJob?.cancel()
        cooldownJob = viewModelScope.launch {
            _state.update { it.copy(canResendIn = seconds) }
            while (_state.value.canResendIn > 0) {
                delay(1000)
                _state.update { it.copy(canResendIn = it.canResendIn - 1) }
            }
        }
    }

    private fun mapError(e: AppError): String = when (e) {
        AppError.Network -> "Нет подключения к интернету"
        AppError.Unauthorized -> "Сессия истекла. Войдите заново."
        is AppError.Server -> if (e.code == "RATE_LIMITED")
            "Слишком часто. Подождите минуту."
        else "Ошибка сервера"
        else -> "Не удалось выполнить операцию"
    }
}