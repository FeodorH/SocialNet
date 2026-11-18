package com.example.socialnet.presentation.common

import android.content.Context
import androidx.annotation.StringRes
import com.example.socialnet.R
import com.example.socialnet.core.common.AppError
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ErrorMapper @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun message(error: AppError): String = when (error) {
        AppError.Network -> context.getString(R.string.error_network)
        AppError.Unauthorized -> context.getString(R.string.error_unauthorized)
        AppError.NotFound -> context.getString(R.string.error_not_found)
        AppError.SelfSubscription -> context.getString(R.string.error_self_subscription)
        is AppError.Server -> context.getString(R.string.error_server)
        is AppError.Unknown -> context.getString(R.string.error_unknown)
        is AppError.Validation -> validationMessage(error)
    }

    /**
     * Возвращает текст для inline-ошибки поля (под полем ввода).
     * Для не-Validation ошибок — null (их показываем общим snackbar'ом).
     */
    fun fieldError(error: AppError): Pair<AppError.Validation.Field, String>? =
        (error as? AppError.Validation)?.let { v -> v.field to validationMessage(v) }

    private fun validationMessage(error: AppError.Validation): String {
        val field = context.getString(fieldRes(error.field))
        return when (error) {
            is AppError.Validation.Required ->
                context.getString(R.string.error_validation_required, field)
            is AppError.Validation.TooShort ->
                context.getString(R.string.error_validation_too_short, field, error.min)
            is AppError.Validation.TooLong ->
                context.getString(R.string.error_validation_too_long, field, error.max)
            is AppError.Validation.InvalidFormat ->
                context.getString(R.string.error_validation_invalid_format, field)
            is AppError.Validation.TooMany ->
                context.getString(R.string.error_validation_too_many, field, error.max)
            is AppError.Validation.TooLarge ->
                context.getString(
                    R.string.error_validation_too_large,
                    field,
                    error.maxBytes / (1024 * 1024),
                )
            is AppError.Validation.UnsupportedType ->
                context.getString(R.string.error_validation_unsupported_type, field)
            is AppError.Validation.UserAlreadyExists ->
                context.getString(R.string.error_validation_user_already_exists)
        }
    }

    @StringRes
    private fun fieldRes(field: AppError.Validation.Field): Int = when (field) {
        AppError.Validation.Field.EMAIL -> R.string.field_email
        AppError.Validation.Field.PASSWORD -> R.string.field_password
        AppError.Validation.Field.NAME -> R.string.field_name
        AppError.Validation.Field.TEXT -> R.string.field_text
        AppError.Validation.Field.IMAGES -> R.string.field_images
    }
}