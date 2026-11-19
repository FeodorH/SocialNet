package com.example.socialnet.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.socialnet.core.common.AppError
import com.example.socialnet.core.common.AppResult
import com.example.socialnet.domain.usecase.LoginUseCase
import com.example.socialnet.domain.usecase.RegisterUseCases
import com.example.socialnet.domain.usecase.ResetPasswordUseCase
import com.example.socialnet.presentation.auth.model.AuthEvent
import com.example.socialnet.presentation.auth.model.AuthUiState
import com.example.socialnet.presentation.common.ErrorMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val registerUseCase: RegisterUseCases,
    private val resetPasswordUseCase: ResetPasswordUseCase,
    private val errorMapper: ErrorMapper
) : ViewModel() {

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    private val _events = Channel<AuthEvent>(Channel.BUFFERED)
    val events: Flow<AuthEvent> = _events.receiveAsFlow()

    fun onEmailChange(email: String) {
        _state.update { it.copy(email = email, emailError = null, generalError = null) }
    }

    fun onPasswordChange(password: String) {
        _state.update { it.copy(password = password, passwordError = null, generalError = null) }
    }

    fun onNameChange(name: String) {
        _state.update { it.copy(name = name, nameError = null, generalError = null) }
    }

    fun submitLogin() {
        val s = _state.value
        if (!s.canSubmitLogin) return

        viewModelScope.launch {
            _state.update {
                it.copy(isLoading = true, emailError = null, passwordError = null, generalError = null)
            }

            when (val r = loginUseCase(s.email, s.password)) {
                is AppResult.Success -> {
                    // Экран уничтожится автоматически при смене authState.
                    // isLoading не сбрасываем — чтобы кнопка не нажалась повторно
                    // в эти 100–300 мс до переключения графа.
                }
                is AppResult.Failure -> applyError(r.error)
            }
        }
    }

    fun submitRegister() {
        val s = _state.value
        if (!s.canSubmitRegister) return

        viewModelScope.launch {
            _state.update {
                it.copy(isLoading = true, emailError = null, passwordError = null, nameError = null, generalError = null)
            }

            when (val r = registerUseCase(s.email, s.password, s.name)) {
                is AppResult.Success -> {
                    // экран уничтожится сам
                }
                is AppResult.Failure -> applyError(r.error)
            }
        }
    }

    fun submitReset() {
        val s = _state.value
        if (!s.canSubmitReset) return

        viewModelScope.launch {
            _state.update {
                it.copy(isLoading = true, emailError = null, generalError = null)
            }

            when (val r = resetPasswordUseCase(s.email)) {
                is AppResult.Success -> {
                    _state.update { it.copy(isLoading = false) }
                    _events.send(AuthEvent.PasswordResetSent)
                }
                is AppResult.Failure -> applyError(r.error)
            }
        }
    }

    private fun applyError(error: AppError) {
        _state.update { current ->
            val withField = current.withFieldError(error)
            (withField ?: current.copy(generalError = errorMapper.message(error)))
                .copy(isLoading = false)
        }
    }

    private fun AuthUiState.withFieldError(error: AppError): AuthUiState? {
        val (field, msg) = errorMapper.fieldError(error) ?: return null
        return when (field) {
            AppError.Validation.Field.EMAIL -> copy(emailError = msg)
            AppError.Validation.Field.PASSWORD -> copy(passwordError = msg)
            AppError.Validation.Field.NAME -> copy(nameError = msg)
            AppError.Validation.Field.TEXT,
            AppError.Validation.Field.IMAGES -> null
        }
    }
}