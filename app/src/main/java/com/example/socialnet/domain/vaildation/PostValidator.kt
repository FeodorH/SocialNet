package com.example.socialnet.domain.vaildation

import com.example.socialnet.core.common.AppError
import com.example.socialnet.core.common.ValidationRules

object PostValidator {
    fun validate(text: String, imageCount: Int): AppError.Validation? = when {
        text.isBlank() && imageCount == 0 ->
            AppError.Validation.Required(AppError.Validation.Field.TEXT)
        text.length > ValidationRules.MAX_POST_TEXT ->
            AppError.Validation.TooLong(AppError.Validation.Field.TEXT, ValidationRules.MAX_POST_TEXT)
        imageCount > ValidationRules.MAX_POST_IMAGES ->
            AppError.Validation.TooMany(AppError.Validation.Field.IMAGES, ValidationRules.MAX_POST_IMAGES)
        else -> null
    }
}