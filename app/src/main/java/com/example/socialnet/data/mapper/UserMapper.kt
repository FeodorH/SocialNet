package com.example.socialnet.data.mapper

import com.example.socialnet.core.model.User
import com.example.socialnet.data.dto.UserResponse

/**
 * Маппинг из ответа Firestore в доменную модель.
 */
fun UserResponse.toDomain(): User {
    return User(
        id = id,
        email = email,
        name = name,
        avatarUrl = avatarUrl,
        avatarPublicId = avatarPublicId,
        bio = bio,
        createdAt = createdAt.toDomainInstant()
    )
}
