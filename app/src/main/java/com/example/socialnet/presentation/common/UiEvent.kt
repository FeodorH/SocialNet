package com.example.socialnet.presentation.common

sealed interface UiEvent {
    data class ShowMessage(val text: String) : UiEvent
}