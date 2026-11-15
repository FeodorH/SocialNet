package com.example.socalnet.presentation.profile.model

sealed interface ProfileEvent {
    data class ShowMessage(val text: String) : ProfileEvent
    data class ShowError(val error: com.example.socalnet.core.common.AppError) : ProfileEvent
    data object LoggedOut : ProfileEvent
}