package com.example.socialnet.presentation.post.model

import com.example.socialnet.core.common.AppError
import com.example.socialnet.core.model.Post

sealed interface SavePostEvent {
    /** Пост успешно создан или обновлён — можно закрывать экран. */
    data class Saved(val post: Post) : SavePostEvent

    /** Ошибка сохранения — показать snackbar. */
    data class ShowError(val error: AppError) : SavePostEvent
}