package com.example.socialnet.domain.vaildation

import com.example.socialnet.core.common.AppError
import com.example.socialnet.core.common.ValidationRules

object AuthValidator {
    fun validateEmail(email: String) : AppError.Validation? =
        when{
            email.isBlank() ->
                AppError.Validation.Required(AppError.Validation.Field.EMAIL)
            !email.contains('@')||email.length > ValidationRules.MAX_EMAIL ->
                AppError.Validation.InvalidFormat(AppError.Validation.Field.EMAIL)
            else -> null
        }

    fun validatePassword(password: String): AppError.Validation? = when {
        password.isEmpty() -> AppError.Validation.Required(AppError.Validation.Field.PASSWORD)
        password.length < ValidationRules.MIN_PASSWORD ->
            AppError.Validation.TooShort(AppError.Validation.Field.PASSWORD, ValidationRules.MIN_PASSWORD)
        password.length > ValidationRules.MAX_PASSWORD ->
            AppError.Validation.TooLong(AppError.Validation.Field.PASSWORD, ValidationRules.MAX_PASSWORD)
        else -> null
    }

    fun validateName(name: String) : AppError.Validation? =
        when{
            name.isBlank() ->
                return AppError.Validation.Required(AppError.Validation.Field.NAME)
            name.length > ValidationRules.MAX_NAME->
                return AppError.Validation.TooLong(AppError.Validation.Field.NAME, ValidationRules.MAX_NAME)
            else -> null
        }
}