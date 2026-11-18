package com.example.socialnet.domain.vaildation

import com.example.socialnet.core.common.AppError
import com.example.socialnet.core.common.ValidationRules
import com.example.socialnet.core.model.ImageMetadata

object ImageValidator {

    fun validate(metadata: ImageMetadata): AppError.Validation? = when {
        metadata.sizeBytes <= 0L ->
            AppError.Validation.InvalidFormat(AppError.Validation.Field.IMAGES)
        metadata.sizeBytes > ValidationRules.MAX_IMAGE_BYTES ->
            AppError.Validation.TooLarge(AppError.Validation.Field.IMAGES, ValidationRules.MAX_IMAGE_BYTES)
        metadata.mimeType == null || metadata.mimeType !in ValidationRules.ALLOWED_IMAGE_MIME ->
            AppError.Validation.UnsupportedType(AppError.Validation.Field.IMAGES)
        else -> null
    }
}