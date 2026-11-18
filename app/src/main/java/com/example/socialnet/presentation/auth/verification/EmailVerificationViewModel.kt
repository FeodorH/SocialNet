package com.example.socialnet.presentation.auth.verification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.socialnet.core.common.AppError
import com.example.socialnet.core.common.AppResult
import com.example.socialnet.core.common.WorkingRules.RESENDING_COOLDOWN_SECONDS
import com.example.socialnet.domain.usecase.LogoutUseCase
import com.example.socialnet.domain.usecase.ReloadCurrentUserUseCase
import com.example.socialnet.domain.usecase.SendEmailVerificationUseCase
import com.example.socialnet.presentation.common.ErrorMapper
import com.example.socialnet.presentation.common.UiStrings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

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
    private val errorMapper: ErrorMapper,
    private val uiStrings: UiStrings,
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
                startCooldown(RESENDING_COOLDOWN_SECONDS)
            }
            is AppResult.Failure -> {
                _state.update {
                    it.copy(isSending = false, errorMessage = errorMapper.message(r.error))
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
                    _state.update { it.copy(isChecking = false) }
                } else {
                    _state.update {
                        it.copy(
                            isChecking = false,
                            errorMessage = uiStrings.emailVerificationPending(),
                        )
                    }
                }
            }
            is AppResult.Failure -> {
                _state.update {
                    it.copy(isChecking = false, errorMessage = errorMapper.message(r.error))
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
                delay(1000.milliseconds)
                _state.update { it.copy(canResendIn = it.canResendIn - 1) }
            }
        }
    }
}