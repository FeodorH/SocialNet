package com.example.socialnet.core.common


sealed interface AppError {

    data object Network : AppError
    data object Unauthorized : AppError
    data object NotFound : AppError

    sealed interface Validation : AppError {
        val field: Field

        enum class Field { EMAIL, PASSWORD, NAME, TEXT, IMAGES }

        data class Required(override val field: Field) : Validation
        data class TooShort(override val field: Field, val min: Int) : Validation
        data class TooLong(override val field: Field, val max: Int) : Validation
        data class InvalidFormat(override val field: Field) : Validation
        data class TooMany(override val field: Field, val max: Int) : Validation
        data class TooLarge(override val field: Field, val maxBytes: Long) : Validation
        data class UnsupportedType(override val field: Field) : Validation
        data class UserAlreadyExists(override val field: Field): Validation
    }

    data object SelfSubscription : AppError

    /** Ошибка сервера. `code` — техническая строка (для логов/аналитики), в UI не показывается. */
    data class Server(val code: String? = null, val cause: Throwable? = null) : AppError

    data class Unknown(val cause: Throwable? = null) : AppError
}
