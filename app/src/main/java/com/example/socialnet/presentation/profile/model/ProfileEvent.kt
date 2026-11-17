package com.example.socialnet.presentation.profile.model

sealed interface ProfileEvent {
    data class ShowMessage(val text: String) : ProfileEvent
    data class ShowError(val error: com.example.socialnet.core.common.AppError) : ProfileEvent
    data object LoggedOut : ProfileEvent
}