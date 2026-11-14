package com.example.socalnet.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.socalnet.domain.usecase.ObserveCurrentUserUseCase
import com.example.socalnet.presentation.main.model.AuthState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    observeCurrentUser: ObserveCurrentUserUseCase,
) : ViewModel() {

    val authState: StateFlow<AuthState> = observeCurrentUser()
        .map { user ->
            if (user == null) AuthState.Guest
            else AuthState.User(user)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = AuthState.Loading,
        )
}