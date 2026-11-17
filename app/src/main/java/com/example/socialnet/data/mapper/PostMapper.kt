package com.example.socialnet.data.mapper

import com.example.socialnet.core.model.ImageRef
import com.example.socialnet.core.model.Post
import com.example.socialnet.core.model.PostAuthor
import com.example.socialnet.core.model.PostDraft
import com.example.socialnet.core.model.ReactionType
import com.example.socialnet.data.dto.PostResponse

/**
 * Маппинг из ответа Firestore в доменную модель.
 *
 * @param author Данные автора (имя, аватар), полученные из кэша или Firestore.
 * @param myReaction Реакция текущего пользователя на этот пост (если есть).
 */
fun PostResponse.toDomain(
    author: PostAuthor,
    myReaction: ReactionType? = null
): Post {
    return Post(
        id = id,
        author = author,
        text = text,
        images = imageUrls.mapIndexed { index, url ->
            val publicId = imagePublicIds.getOrNull(index) // Безопасно берем ID по индексу
            ImageRef.Remote(url, publicId)
        },
        createdAt = createdAt.toDomainInstant(),
        updatedAt = updatedAt?.toDomainInstant(),
        reactions = reactionsByType.toReactionSummary(reactionsTotal),
        myReaction = myReaction
    )
}
